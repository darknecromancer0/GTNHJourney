package dev.gtnhjourney.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;

public class OrdinaryGregTechPersistenceRegressionTest {

    @Test
    public void ordinaryMetaitemAndMachineSurviveTwoSaveLoadCyclesExactly() {
        UUID playerId = UUID.randomUUID();
        NBTTagCompound firstRoot = root(
            playerId,
            entry("gregtech:gt.metaitem.01", 17305),
            entry("gregtech:gt.blockmachines", 101));

        JourneyResearchData first = new JourneyResearchData();
        first.readFromNBT(firstRoot);
        assertKeys(first.snapshotInUnlockOrder(playerId));

        NBTTagCompound rewritten = new NBTTagCompound();
        first.writeToNBT(rewritten);

        JourneyResearchData second = new JourneyResearchData();
        second.readFromNBT(rewritten);
        assertKeys(second.snapshotInUnlockOrder(playerId));

        NBTTagCompound rewrittenAgain = new NBTTagCompound();
        second.writeToNBT(rewrittenAgain);
        JourneyResearchData third = new JourneyResearchData();
        third.readFromNBT(rewrittenAgain);
        assertKeys(third.snapshotInUnlockOrder(playerId));
    }

    private static void assertKeys(List<ResearchKey> keys) {
        assertEquals(2, keys.size());
        assertEquals("gregtech:gt.metaitem.01", keys.get(0).getItemId());
        assertEquals(17305, keys.get(0).getMeta());
        assertEquals("", keys.get(0).getCanonicalNbt());
        assertEquals("gregtech:gt.blockmachines", keys.get(1).getItemId());
        assertEquals(101, keys.get(1).getMeta());
        assertEquals("", keys.get(1).getCanonicalNbt());
    }

    private static NBTTagCompound root(UUID playerId, NBTTagCompound... entries) {
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("Version", 8);
        NBTTagCompound player = new NBTTagCompound();
        player.setLong("UuidMost", playerId.getMostSignificantBits());
        player.setLong("UuidLeast", playerId.getLeastSignificantBits());
        NBTTagList entryList = new NBTTagList();
        for (NBTTagCompound entry : entries) entryList.appendTag(entry);
        player.setTag("Entries", entryList);
        NBTTagList players = new NBTTagList();
        players.appendTag(player);
        root.setTag("Players", players);
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
