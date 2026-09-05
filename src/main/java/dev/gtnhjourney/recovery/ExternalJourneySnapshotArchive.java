package dev.gtnhjourney.recovery;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.SyncFailedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import dev.gtnhjourney.GTNHJourney;
import dev.gtnhjourney.research.ResearchKey;

/**
 * Standalone Journey research/player recovery snapshots outside the Minecraft world save tree.
 *
 * <p>This deliberately does not use WorldSavedData. If vanilla/Forge autosave is disabled or wedged, the recovery
 * archive still reaches disk independently. Player NBT is captured on the server thread before the file write, so the
 * local singleplayer owner inventory is recoverable without depending on level.dat's embedded Player compound.</p>
 */
public final class ExternalJourneySnapshotArchive {

    public static final long MAX_ARCHIVE_BYTES = 50L * 1024L * 1024L;
    private static final int DATA_VERSION = 2;

    private ExternalJourneySnapshotArchive() {}

    public static File write(
        File instanceRoot,
        String worldName,
        UUID playerId,
        long worldTick,
        long createdAtMillis,
        ResearchStateSnapshot state) throws IOException {
        return write(instanceRoot, worldName, playerId, worldTick, createdAtMillis, state, null);
    }

    public static File write(
        File instanceRoot,
        String worldName,
        UUID playerId,
        long worldTick,
        long createdAtMillis,
        ResearchStateSnapshot state,
        NBTTagCompound playerState) throws IOException {
        if (instanceRoot == null) throw new IllegalArgumentException("instanceRoot");
        if (playerId == null) throw new IllegalArgumentException("playerId");
        if (state == null) throw new IllegalArgumentException("state");

        File directory = archiveDirectory(instanceRoot, worldName);
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Could not create Journey recovery directory: " + directory);
        }

        String fileName = "snapshot-" + createdAtMillis + "-tick-" + worldTick + "-" + playerId + ".dat";
        File target = new File(directory, fileName);
        File temporary = new File(directory, fileName + ".tmp");
        if (temporary.exists() && !temporary.delete()) {
            throw new IOException("Could not clear stale Journey recovery temp file: " + temporary);
        }

        NBTTagCompound root = serialize(playerId, worldTick, createdAtMillis, state, playerState);
        boolean moved = false;
        try {
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                CompressedStreamTools.writeCompressed(root, output);
                try {
                    output.getFD().sync();
                } catch (SyncFailedException unsupportedSync) {
                    // Some valid filesystems/runners do not expose fsync. The fully written compressed temp file still
                    // proceeds through atomic replacement; an unsupported durability hint must not discard recovery.
                }
            }
            try {
                Files.move(
                    temporary.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
            target.setLastModified(createdAtMillis);
            pruneToBudget(directory, MAX_ARCHIVE_BYTES);
            return target;
        } finally {
            if (!moved && temporary.exists()) temporary.delete();
        }
    }

    /**
     * Returns the newest readable external recovery point for this world/player. Corrupt or incompatible newer files
     * are skipped so one damaged write can never hide an older usable snapshot.
     */
    public static ArchivedSnapshot latest(File instanceRoot, String worldName, UUID playerId) {
        if (instanceRoot == null || playerId == null) return null;
        File directory = archiveDirectory(instanceRoot, worldName);
        if (!directory.isDirectory()) return null;
        File[] files = directory.listFiles(file -> file != null && file.isFile() && file.getName().endsWith(".dat"));
        if (files == null || files.length == 0) return null;
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).thenComparing(File::getName).reversed());
        for (File file : files) {
            try {
                ArchivedSnapshot snapshot = read(file);
                if (snapshot != null && playerId.equals(snapshot.playerId())) return snapshot;
            } catch (IOException ignored) {
                // Keep scanning older recovery points. A corrupt newest file must not shadow healthy history.
            } catch (RuntimeException ignored) {
                // Malformed NBT is isolated to this file.
            }
        }
        return null;
    }

    static File archiveDirectory(File instanceRoot, String worldName) {
        String safeWorld = sanitize(worldName == null || worldName.trim().isEmpty() ? "world" : worldName);
        return new File(new File(new File(instanceRoot, "gtnhjourney-recovery"), safeWorld), "research-snapshots");
    }

    public static File instanceRootFor(File worldDirectory) {
        if (worldDirectory == null) return new File(".").getAbsoluteFile();
        File absolute = worldDirectory.getAbsoluteFile();
        File parent = absolute.getParentFile();
        if (parent != null && "saves".equalsIgnoreCase(parent.getName()) && parent.getParentFile() != null) {
            return parent.getParentFile();
        }
        return parent == null ? new File(".").getAbsoluteFile() : parent;
    }

    static void pruneToBudget(File directory, long budgetBytes) {
        if (directory == null || !directory.isDirectory()) return;
        File[] files = directory.listFiles(file -> file != null && file.isFile() && file.getName().endsWith(".dat"));
        if (files == null || files.length == 0) return;
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).thenComparing(File::getName));
        long total = 0L;
        for (File file : files) total += Math.max(0L, file.length());
        for (File file : files) {
            if (total <= Math.max(0L, budgetBytes)) break;
            long length = Math.max(0L, file.length());
            if (file.delete()) total -= length;
        }
    }

    static long totalBytes(File directory) {
        if (directory == null || !directory.isDirectory()) return 0L;
        File[] files = directory.listFiles(file -> file != null && file.isFile() && file.getName().endsWith(".dat"));
        if (files == null) return 0L;
        long total = 0L;
        for (File file : files) total += Math.max(0L, file.length());
        return total;
    }

    static int readEntryCount(File file) throws IOException {
        try (FileInputStream input = new FileInputStream(file)) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(input);
            return root.getInteger("EntryCount");
        }
    }

    private static ArchivedSnapshot read(File file) throws IOException {
        try (FileInputStream input = new FileInputStream(file)) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(input);
            if (root == null) throw new IOException("Empty Journey snapshot");
            int version = root.getInteger("Version");
            if (version <= 0 || version > DATA_VERSION) throw new IOException("Unsupported Journey snapshot version " + version);
            UUID playerId = new UUID(root.getLong("UuidMost"), root.getLong("UuidLeast"));
            long createdAtMillis = root.getLong("CreatedAtMillis");
            long worldTick = root.getLong("WorldTick");
            NBTTagList tags = root.getTagList("Entries", 10);
            int declaredCount = root.getInteger("EntryCount");
            if (declaredCount < 0 || declaredCount != tags.tagCount()) {
                throw new IOException("Journey snapshot entry count mismatch");
            }
            List<ResearchEntrySnapshot> entries = new ArrayList<ResearchEntrySnapshot>(tags.tagCount());
            for (int i = 0; i < tags.tagCount(); i++) {
                NBTTagCompound entry = tags.getCompoundTagAt(i);
                ResearchKey key = new ResearchKey(
                    entry.getString("ItemId"),
                    entry.getInteger("Meta"),
                    entry.getString("CanonicalNbt"));
                int timelineIndex = entry.getInteger("TimelineIndex");
                if (timelineIndex != i) throw new IOException("Journey snapshot timeline is not contiguous");
                NBTTagCompound template = entry.hasKey("Tag", 10) ? entry.getCompoundTag("Tag") : null;
                entries.add(new ResearchEntrySnapshot(key, template, timelineIndex));
            }
            NBTTagCompound playerState = root.hasKey("Player", 10) ? root.getCompoundTag("Player") : null;
            return new ArchivedSnapshot(
                file,
                playerId,
                createdAtMillis,
                worldTick,
                new ResearchStateSnapshot(entries),
                playerState);
        }
    }

    private static NBTTagCompound serialize(
        UUID playerId,
        long worldTick,
        long createdAtMillis,
        ResearchStateSnapshot state,
        NBTTagCompound playerState) {
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("Version", DATA_VERSION);
        root.setString("JourneyVersion", GTNHJourney.VERSION);
        root.setLong("CreatedAtMillis", createdAtMillis);
        root.setLong("WorldTick", worldTick);
        root.setLong("UuidMost", playerId.getMostSignificantBits());
        root.setLong("UuidLeast", playerId.getLeastSignificantBits());
        root.setInteger("EntryCount", state.size());

        NBTTagList entries = new NBTTagList();
        for (ResearchEntrySnapshot snapshot : state.entries()) {
            NBTTagCompound entry = new NBTTagCompound();
            ResearchKey key = snapshot.key();
            entry.setString("ItemId", key.getItemId());
            entry.setInteger("Meta", key.getMeta());
            entry.setString("CanonicalNbt", key.getCanonicalNbt());
            entry.setInteger("TimelineIndex", snapshot.timelineIndex());
            NBTTagCompound template = snapshot.template();
            if (template != null) entry.setTag("Tag", template);
            entries.appendTag(entry);
        }
        root.setTag("Entries", entries);

        if (playerState != null) {
            NBTTagCompound playerCopy = (NBTTagCompound) playerState.copy();
            root.setTag("Player", playerCopy);
            root.setInteger("InventoryEntryCount", playerCopy.getTagList("Inventory", 10).tagCount());
        } else {
            root.setInteger("InventoryEntryCount", 0);
        }
        return root;
    }

    private static String sanitize(String value) {
        String safe = value.replaceAll("[^A-Za-z0-9._-]+", "_");
        safe = safe.replaceAll("^_+|_+$", "");
        return safe.isEmpty() ? "world" : safe;
    }

    /** Immutable decoded external recovery point. */
    public static final class ArchivedSnapshot {
        private final File file;
        private final UUID playerId;
        private final long createdAtMillis;
        private final long worldTick;
        private final ResearchStateSnapshot state;
        private final NBTTagCompound playerState;

        ArchivedSnapshot(
            File file,
            UUID playerId,
            long createdAtMillis,
            long worldTick,
            ResearchStateSnapshot state,
            NBTTagCompound playerState) {
            this.file = file;
            this.playerId = playerId;
            this.createdAtMillis = createdAtMillis;
            this.worldTick = worldTick;
            this.state = state;
            this.playerState = playerState == null ? null : (NBTTagCompound) playerState.copy();
        }

        public File file() { return file; }
        public UUID playerId() { return playerId; }
        public long createdAtMillis() { return createdAtMillis; }
        public long worldTick() { return worldTick; }
        public ResearchStateSnapshot state() { return state; }
        public NBTTagCompound playerState() {
            return playerState == null ? null : (NBTTagCompound) playerState.copy();
        }
    }
}
