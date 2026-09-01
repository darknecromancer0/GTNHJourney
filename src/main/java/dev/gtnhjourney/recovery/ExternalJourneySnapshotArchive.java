package dev.gtnhjourney.recovery;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.SyncFailedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
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
}
