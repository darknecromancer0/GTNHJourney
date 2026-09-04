package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;

public class NetworkToolTransientStatePolicyTest {

    @Test
    public void ae2NetworkVisualiserModeAndTargetDoNotDefineResearchIdentity() throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("NETWORK_VISUALISER", "P2P");
        tag.setInteger("dim", 185);
        tag.setInteger("x", 37);
        tag.setInteger("y", 7);
        tag.setInteger("z", 17);
        tag.setString("Marker", "keep");

        normalize("appliedenergistics2:item.ToolNetworkVisualiser", 0, tag);

        assertFalse(tag.hasKey("NETWORK_VISUALISER"));
        assertFalse(tag.hasKey("dim"));
        assertFalse(tag.hasKey("x"));
        assertFalse(tag.hasKey("y"));
        assertFalse(tag.hasKey("z"));
        assertEquals("keep", tag.getString("Marker"));
    }

    @Test
    public void betterP2pAdvancedMemoryCardSelectionStateDoesNotDefineResearchIdentity() throws Exception {
        NBTTagCompound selectedIndex = new NBTTagCompound();
        selectedIndex.setInteger("d", 185);
        selectedIndex.setByte("f", (byte) 5);
        selectedIndex.setInteger("x", 35);
        selectedIndex.setInteger("y", 2);
        selectedIndex.setInteger("z", -8);

        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("frequency", 1788485770889L);
        tag.setByte("gui", (byte) 3);
        tag.setInteger("mode", 1);
        tag.setTag("selectedIndex", selectedIndex);
        tag.setString("Marker", "keep");

        normalize("betterp2p:advanced_memory_card", 0, tag);

        assertFalse(tag.hasKey("frequency"));
        assertFalse(tag.hasKey("gui"));
        assertFalse(tag.hasKey("mode"));
        assertFalse(tag.hasKey("selectedIndex"));
        assertEquals("keep", tag.getString("Marker"));
    }

    @Test
    public void sameNamedFieldsRemainSemanticForUnrelatedItems() throws Exception {
        NBTTagCompound selectedIndex = new NBTTagCompound();
        selectedIndex.setInteger("x", 12);

        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("NETWORK_VISUALISER", "FULL");
        tag.setInteger("dim", 7);
        tag.setInteger("x", 1);
        tag.setInteger("y", 2);
        tag.setInteger("z", 3);
        tag.setLong("frequency", 42L);
        tag.setByte("gui", (byte) 1);
        tag.setInteger("mode", 2);
        tag.setTag("selectedIndex", selectedIndex);

        normalize("example:network_item", 0, tag);

        assertEquals("FULL", tag.getString("NETWORK_VISUALISER"));
        assertEquals(7, tag.getInteger("dim"));
        assertEquals(42L, tag.getLong("frequency"));
        assertTrue(tag.hasKey("selectedIndex"));
    }

    private static void normalize(String registryId, int meta, NBTTagCompound tag) throws Exception {
        Class<?> policy = Class.forName("dev.gtnhjourney.minecraft.KnownTransientItemStatePolicy");
        Method normalize = policy.getDeclaredMethod("normalize", String.class, int.class, NBTTagCompound.class);
        normalize.setAccessible(true);
        normalize.invoke(null, registryId, meta, tag);
    }
}
