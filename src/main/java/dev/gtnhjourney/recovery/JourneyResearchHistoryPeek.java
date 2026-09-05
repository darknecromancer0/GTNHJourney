package dev.gtnhjourney.recovery;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import dev.gtnhjourney.persistence.JourneyRecoveryData;

/** Read-only timestamp bridge for the legacy research journal without changing its persisted schema. */
final class JourneyResearchHistoryPeek {

    private JourneyResearchHistoryPeek() {}

    static long undoTimestamp(EntityPlayerMP player) { return timestamp(player, "Undo"); }
    static long redoTimestamp(EntityPlayerMP player) { return timestamp(player, "Redo"); }

    private static long timestamp(EntityPlayerMP player, String listName) {
        if (player == null) return Long.MIN_VALUE;
        World rootWorld = DimensionManager.getWorld(0);
        if (rootWorld == null) rootWorld = player.worldObj;
        NBTTagCompound root = new NBTTagCompound();
        JourneyRecoveryData.get(rootWorld).writeToNBT(root);
        NBTTagList players = root.getTagList("Players", 10);
        UUID wanted = player.getUniqueID();
        for (int i = 0; i < players.tagCount(); i++) {
            NBTTagCompound playerTag = players.getCompoundTagAt(i);
            UUID found = new UUID(playerTag.getLong("UuidMost"), playerTag.getLong("UuidLeast"));
            if (!wanted.equals(found)) continue;
            NBTTagList transactions = playerTag.getTagList(listName, 10);
            if (transactions.tagCount() <= 0) return Long.MIN_VALUE;
            return transactions.getCompoundTagAt(transactions.tagCount() - 1).getLong("Timestamp");
        }
        return Long.MIN_VALUE;
    }
}
