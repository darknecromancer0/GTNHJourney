package dev.gtnhjourney;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.minecraft.ResearchTemplateNormalizer;
import dev.gtnhjourney.nei.JourneyVariantScope;
import dev.gtnhjourney.nei.JourneyViewState;
import dev.gtnhjourney.research.ResearchKey;
import net.minecraft.nbt.NBTTagCompound;

class Pre6RegressionContractTest {

    @SuppressWarnings("unchecked")
    @Test
    void journeyPanelOrderKeepsResearchOrderAndReversesNewestTail() throws Exception {
        Class<?> planner = Class.forName("dev.gtnhjourney.nei.JourneyPanelOrder");
        Method keysForMode = planner.getDeclaredMethod(
            "keysForMode",
            List.class,
            JourneyViewState.Mode.class,
            int.class);
        keysForMode.setAccessible(true);

        ResearchKey oldest = new ResearchKey("test:oldest", 0, "");
        ResearchKey middle = new ResearchKey("test:middle", 0, "");
        ResearchKey newest = new ResearchKey("test:newest", 0, "");
        List<ResearchKey> unlockOrder = Arrays.asList(oldest, middle, newest);

        assertEquals(
            Arrays.asList(oldest, middle, newest),
            (List<ResearchKey>) keysForMode.invoke(null, unlockOrder, JourneyViewState.Mode.RESEARCHED, 2));
        assertEquals(
            Arrays.asList(newest, middle),
            (List<ResearchKey>) keysForMode.invoke(null, unlockOrder, JourneyViewState.Mode.NEWEST, 2));
    }

    @Test
    void blankNbtVariantIsInjectedOnlyWhenExactNativeStateIsMissing() throws Exception {
        Method policy = JourneyVariantScope.class.getDeclaredMethod(
            "shouldInjectVariant",
            ResearchKey.class,
            boolean.class);
        policy.setAccessible(true);

        ResearchKey blankBase = new ResearchKey("IC2:itemToolDrill", 26, "");
        ResearchKey exactNbt = new ResearchKey("test:compound", 0, "10{4:data=3:1;}");

        assertTrue((Boolean) policy.invoke(null, blankBase, false));
        assertFalse((Boolean) policy.invoke(null, blankBase, true));
        assertTrue((Boolean) policy.invoke(null, exactNbt, true));
    }

    @Test
    void tconWearResetPreservesLevelingAndModifierState() throws Exception {
        Method normalize = ResearchTemplateNormalizer.class.getDeclaredMethod(
            "normalizeTconWearState",
            NBTTagCompound.class);
        normalize.setAccessible(true);

        NBTTagCompound infiTool = new NBTTagCompound();
        infiTool.setInteger("Damage", 37);
        infiTool.setBoolean("Broken", true);
        infiTool.setBoolean("RenderBroken", true);
        infiTool.setInteger("ToolLevel", 4);
        infiTool.setLong("ToolEXP", 1234L);
        infiTool.setLong("HeadEXP", 567L);
        infiTool.setInteger("ExtraRedstone", 18);

        NBTTagCompound normalized = (NBTTagCompound) normalize.invoke(null, infiTool);

        assertEquals(0, normalized.getInteger("Damage"));
        assertFalse(normalized.getBoolean("Broken"));
        assertFalse(normalized.hasKey("RenderBroken"));
        assertEquals(4, normalized.getInteger("ToolLevel"));
        assertEquals(1234L, normalized.getLong("ToolEXP"));
        assertEquals(567L, normalized.getLong("HeadEXP"));
        assertEquals(18, normalized.getInteger("ExtraRedstone"));

        NBTTagCompound legacy = new NBTTagCompound();
        legacy.setInteger("ToolLevel", 2);
        legacy.setBoolean("HarvestLevelModified", false);
        NBTTagCompound repaired = (NBTTagCompound) normalize.invoke(null, legacy);
        assertTrue(repaired.hasKey("ToolEXP"));
        assertEquals(0L, repaired.getLong("ToolEXP"));
        assertTrue(repaired.hasKey("HeadEXP"));
        assertEquals(0L, repaired.getLong("HeadEXP"));
    }

    @Test
    void flaskPresentationSanitizationDoesNotMutateRetrievalNbt() throws Exception {
        Class<?> safety = Class.forName("dev.gtnhjourney.nei.JourneyPresentationSafety");
        Method sanitize = safety.getDeclaredMethod("sanitizedFlaskTag", NBTTagCompound.class);
        sanitize.setAccessible(true);

        NBTTagCompound fluid = new NBTTagCompound();
        fluid.setString("FluidName", "fuelgc");
        fluid.setInteger("Amount", 32000);
        NBTTagCompound original = new NBTTagCompound();
        original.setInteger("Capacity", 32000);
        original.setTag("Fluid", fluid);

        NBTTagCompound display = (NBTTagCompound) sanitize.invoke(null, original);

        assertTrue(original.hasKey("Fluid"));
        assertFalse(display.hasKey("Fluid"));
        assertEquals(32000, display.getInteger("Capacity"));
    }
}
