package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;

public class ThaumcraftWandStatePolicyTest {

    private static final String WAND_ID = "Thaumcraft:WandCasting";
    private static final String POLICY = "dev.gtnhjourney.minecraft.ThaumcraftWandStatePolicy";
    private static final String[] VIS = { "aer", "aqua", "ignis", "ordo", "perditio", "terra" };

    @Test
    public void emptyWandKeepsOnlyEmptyVisEndpointAndDropsInteractionCoordinates() throws Exception {
        NBTTagCompound tag = emptyVis();
        tag.setInteger("IIUX", 12);
        tag.setInteger("IIUY", 64);
        tag.setInteger("IIUZ", -5);
        tag.setString("rod", "greatwood");
        tag.setString("cap", "gold");

        normalizeTag(tag, 5000);

        assertAllVis(tag, 0);
        assertFalse(tag.hasKey("IIUX"));
        assertFalse(tag.hasKey("IIUY"));
        assertFalse(tag.hasKey("IIUZ"));
        assertEquals("greatwood", tag.getString("rod"));
        assertEquals("gold", tag.getString("cap"));
    }

    @Test
    public void onePositiveUnitOfAnyVisCanonicalizesToFullyFilledEndpoint() throws Exception {
        NBTTagCompound tag = emptyVis();
        tag.setInteger("terra", 1);
        tag.setInteger("IIUX", 1);

        normalizeTag(tag, 2500);

        assertAllVis(tag, 2500);
        assertFalse(tag.hasKey("IIUX"));
    }

    @Test
    public void partialVisAndDifferentInteractionCoordinatesCollapseToSameIdentity() throws Exception {
        NBTTagCompound first = emptyVis();
        first.setInteger("terra", 300);
        first.setInteger("perditio", 2000);
        first.setInteger("IIUX", 252);
        first.setInteger("IIUY", 92);
        first.setInteger("IIUZ", -132);

        NBTTagCompound second = emptyVis();
        second.setInteger("aer", 600);
        second.setInteger("aqua", 2500);
        second.setInteger("ignis", 2400);
        second.setInteger("ordo", 1700);
        second.setInteger("IIUX", 238);
        second.setInteger("IIUY", 88);
        second.setInteger("IIUZ", -134);

        normalizeTag(first, 2500);
        normalizeTag(second, 2500);

        assertEquals(ResearchNbtIdentity.canonicalize(first), ResearchNbtIdentity.canonicalize(second));
        assertAllVis(first, 2500);
        assertAllVis(second, 2500);
    }

    @Test
    public void wandConstructionAndFocusIdentitySurviveVisCanonicalization() throws Exception {
        NBTTagCompound tag = emptyVis();
        tag.setInteger("ordo", 100);
        tag.setString("rod", "silverwood");
        tag.setString("cap", "thaumium");
        tag.setByte("sceptre", (byte) 1);
        NBTTagCompound focus = new NBTTagCompound();
        focus.setString("id", "Thaumcraft:FocusExcavation");
        tag.setTag("focus", focus);

        normalizeTag(tag, 7500);

        assertAllVis(tag, 7500);
        assertEquals("silverwood", tag.getString("rod"));
        assertEquals("thaumium", tag.getString("cap"));
        assertTrue(tag.hasKey("sceptre"));
        assertEquals("Thaumcraft:FocusExcavation", tag.getCompoundTag("focus").getString("id"));
    }

    @Test
    public void persistedDefaultWoodenWandCanCollapseBeforeRegistryReconstruction() throws Exception {
        NBTTagCompound tag = emptyVis();
        tag.setInteger("terra", 300);
        tag.setInteger("IIUX", 252);
        tag.setInteger("IIUY", 92);
        tag.setInteger("IIUZ", -132);

        normalizePersisted(tag, 0);

        assertAllVis(tag, 2500);
        assertFalse(tag.hasKey("IIUX"));
        assertFalse(tag.hasKey("IIUY"));
        assertFalse(tag.hasKey("IIUZ"));
    }

    @Test
    public void persistedGreatwoodWandFromLiveDumpCollapsesToFullEndpoint() throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("cap", "iron");
        tag.setString("rod", "greatwood");
        tag.setInteger("perditio", 300);
        tag.setInteger("IIUX", -246);
        tag.setInteger("IIUY", 68);
        tag.setInteger("IIUZ", -6826);

        normalizePersisted(tag, 20);

        assertAllVis(tag, 5000);
        assertFalse(tag.hasKey("IIUX"));
        assertFalse(tag.hasKey("IIUY"));
        assertFalse(tag.hasKey("IIUZ"));
        assertEquals("greatwood", tag.getString("rod"));
        assertEquals("iron", tag.getString("cap"));
    }

    @Test
    public void persistedGreatwoodStaffFromLiveDumpCollapsesToFullEndpoint() throws Exception {
        NBTTagCompound tag = emptyVis();
        tag.setString("cap", "gold");
        tag.setString("rod", "greatwood_staff");
        tag.setInteger("aer", 900);
        tag.setInteger("ignis", 300);
        tag.setInteger("IIUX", 526);
        tag.setInteger("IIUY", 78);
        tag.setInteger("IIUZ", -42);

        normalizePersisted(tag, 24);

        assertAllVis(tag, 12500);
        assertFalse(tag.hasKey("IIUX"));
        assertFalse(tag.hasKey("IIUY"));
        assertFalse(tag.hasKey("IIUZ"));
        assertEquals("greatwood_staff", tag.getString("rod"));
        assertEquals("gold", tag.getString("cap"));
    }

    @Test
    public void stackAwareResearchIdentityRunsWandPolicyBeforeCanonicalization() throws IOException {
        String source = new String(
            Files.readAllBytes(Paths.get("src/main/java/dev/gtnhjourney/minecraft/ResearchNbtIdentity.java")),
            StandardCharsets.UTF_8);

        assertTrue(source.contains("ThaumcraftWandStatePolicy.normalize(stack, identityTag);"));
    }

    @Test
    public void foreignItemNbtRemainsExact() throws Exception {
        NBTTagCompound tag = emptyVis();
        tag.setInteger("terra", 1);
        tag.setInteger("IIUX", 99);

        invoke("normalizeTag", new Class<?>[] { String.class, NBTTagCompound.class, int.class },
            new Object[] { "test:wandishThing", tag, Integer.valueOf(2500) });

        assertEquals(1, tag.getInteger("terra"));
        assertEquals(99, tag.getInteger("IIUX"));
    }

    private static NBTTagCompound emptyVis() {
        NBTTagCompound tag = new NBTTagCompound();
        for (String key : VIS) tag.setInteger(key, 0);
        return tag;
    }

    private static void normalizeTag(NBTTagCompound tag, int maxVis) throws Exception {
        invoke("normalizeTag", new Class<?>[] { String.class, NBTTagCompound.class, int.class },
            new Object[] { WAND_ID, tag, Integer.valueOf(maxVis) });
    }

    private static void normalizePersisted(NBTTagCompound tag, int meta) throws Exception {
        invoke("normalizePersisted", new Class<?>[] { String.class, int.class, NBTTagCompound.class },
            new Object[] { WAND_ID, Integer.valueOf(meta), tag });
    }

    private static void invoke(String methodName, Class<?>[] parameterTypes, Object[] args) throws Exception {
        Class<?> policy = Class.forName(POLICY);
        Method method = policy.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        method.invoke(null, args);
    }

    private static void assertAllVis(NBTTagCompound tag, int expected) {
        for (String key : VIS) assertEquals(expected, tag.getInteger(key), key);
    }
}
