package dev.gtnhjourney.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class JourneyResearchV7MigrationTest {

    @Test
    public void v7ResearchSurvivesButLegacyUndoIsNotRewritten() {
        UUID player = UUID.randomUUID();
        JourneyResearchData data = new JourneyResearchData();
        data.readFromNBT(v7Root(player));

        assertEquals(2, data.snapshotInUnlockOrder(player).size());
        assertEquals("minecraft:stone", data.snapshotInUnlockOrder(player).get(0).getItemId());
        assertEquals("minecraft:dirt", data.snapshotInUnlockOrder(player).get(1).getItemId());

        NBTTagCompound rewritten = new NBTTagCompound();
        data.writeToNBT(rewritten);
        assertEquals(8, rewritten.getInteger("Version"));
        assertEquals(0, rewritten.getTagList("UndoPlayers", 10).tagCount());

        JourneyRecoveryData recovery = new JourneyRecoveryData();
        assertEquals(0, recovery.undoDepth(player));
        assertEquals(0, recovery.redoDepth(player));
        assertEquals(0, recovery.deletionCount(player));
    }

    private static NBTTagCompound v7Root(UUID playerId) {
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("Version", 7);

        NBTTagCompound player = new NBTTagCompound();
        player.setLong("UuidMost", playerId.getMostSignificantBits());
        player.setLong("UuidLeast", playerId.getLeastSignificantBits());
        NBTTagList entries = new NBTTagList();
        entries.appendTag(entry("minecraft:stone", 0));
        entries.appendTag(entry("minecraft:dirt", 0));
        player.setTag("Entries", entries);
        NBTTagList players = new NBTTagList();
        players.appendTag(player);
        root.setTag("Players", players);

        NBTTagCompound undoPlayer = new NBTTagCompound();
        undoPlayer.setLong("UuidMost", playerId.getMostSignificantBits());
        undoPlayer.setLong("UuidLeast", playerId.getLeastSignificantBits());
        NBTTagList undoEntries = new NBTTagList();
        undoEntries.appendTag(entry("minecraft:glass", 0));
        undoPlayer.setTag("Entries", undoEntries);
        NBTTagList undoPlayers = new NBTTagList();
        undoPlayers.appendTag(undoPlayer);
        root.setTag("UndoPlayers", undoPlayers);
        return root;
    }

    private static NBTTagCompound entry(String itemId, int meta) {
        NBTTagCompound entry = new NBTTagCompound();
        entry.setString("ItemId", itemId);
        entry.setInteger("Meta", meta);
        entry.setString("CanonicalNbt", "");
        return entry;
    }
}
