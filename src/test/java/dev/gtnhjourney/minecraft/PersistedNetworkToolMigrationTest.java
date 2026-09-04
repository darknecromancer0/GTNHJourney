package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;
import net.minecraft.nbt.NBTTagCompound;

public class PersistedNetworkToolMigrationTest {

    @Test
    public void persistedAe2NetworkVisualiserStatesCollapseToOneResearchKey() {
        NBTTagCompound channels = visualiser("CHANNELS", 37, 7, 16);
        NBTTagCompound p2p = visualiser("P2P", 36, 7, 17);

        PersistedResearchEntryResolver.ResolvedEntry left = PersistedResearchEntryResolver.resolveEntry(
            "appliedenergistics2:item.ToolNetworkVisualiser",
            0,
            ResearchNbtIdentity.canonicalize(channels),
            channels);
        PersistedResearchEntryResolver.ResolvedEntry right = PersistedResearchEntryResolver.resolveEntry(
            "appliedenergistics2:item.ToolNetworkVisualiser",
            0,
            ResearchNbtIdentity.canonicalize(p2p),
            p2p);

        assertEquals(left.key(), right.key());
        assertEquals("keep", left.template().getString("Marker"));
        assertFalse(left.template().hasKey("NETWORK_VISUALISER"));
        assertFalse(left.template().hasKey("dim"));
        assertFalse(left.template().hasKey("x"));
        assertFalse(left.template().hasKey("y"));
        assertFalse(left.template().hasKey("z"));
    }

    @Test
    public void persistedBetterP2pMemoryCardStatesCollapseToOneResearchKey() {
        NBTTagCompound first = memoryCard(1788484954940L, (byte) 0, 37, 7, 15);
        NBTTagCompound second = memoryCard(1788485770889L, (byte) 5, 35, 2, -8);

        PersistedResearchEntryResolver.ResolvedEntry left = PersistedResearchEntryResolver.resolveEntry(
            "betterp2p:advanced_memory_card",
            0,
            ResearchNbtIdentity.canonicalize(first),
            first);
        PersistedResearchEntryResolver.ResolvedEntry right = PersistedResearchEntryResolver.resolveEntry(
            "betterp2p:advanced_memory_card",
            0,
            ResearchNbtIdentity.canonicalize(second),
            second);

        ResearchKey leftKey = left.key();
        ResearchKey rightKey = right.key();
        assertEquals(leftKey, rightKey);
        assertEquals("keep", left.template().getString("Marker"));
        assertFalse(left.template().hasKey("frequency"));
        assertFalse(left.template().hasKey("gui"));
        assertFalse(left.template().hasKey("mode"));
        assertFalse(left.template().hasKey("selectedIndex"));
    }

    private static NBTTagCompound visualiser(String mode, int x, int y, int z) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("NETWORK_VISUALISER", mode);
        tag.setInteger("dim", 185);
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        tag.setString("Marker", "keep");
        return tag;
    }

    private static NBTTagCompound memoryCard(long frequency, byte face, int x, int y, int z) {
        NBTTagCompound selectedIndex = new NBTTagCompound();
        selectedIndex.setInteger("d", 185);
        selectedIndex.setByte("f", face);
        selectedIndex.setInteger("x", x);
        selectedIndex.setInteger("y", y);
        selectedIndex.setInteger("z", z);

        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("frequency", frequency);
        tag.setByte("gui", (byte) 3);
        tag.setInteger("mode", 1);
        tag.setTag("selectedIndex", selectedIndex);
        tag.setString("Marker", "keep");
        return tag;
    }
}
