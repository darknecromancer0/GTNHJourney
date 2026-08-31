package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;
import net.minecraft.nbt.NBTTagCompound;

public class JetpackIdentityRegressionTest {

    private static final String COAL_JETPACK = "adventurebackpack:coalJetpack";
    private static final String IC2_ELECTRIC_JETPACK = "IC2:itemArmorJetpackElectric";

    @Test
    public void coalJetpackDropsObservedOperationalWearableStateButKeepsUnrelatedTopLevelData() {
        NBTTagCompound tag = coalJetpackRuntimeTag(200, 5348, 16000, true);
        tag.setString("PersistentUpgrade", "keep");

        WearableTransientStatePolicy.normalize(COAL_JETPACK, tag);

        assertFalse(tag.hasKey("wearableData"));
        assertEquals("keep", tag.getString("PersistentUpgrade"));
    }

    @Test
    public void foreignWearableDataRemainsExact() {
        NBTTagCompound tag = coalJetpackRuntimeTag(200, 5348, 16000, true);

        WearableTransientStatePolicy.normalize("test:foreignWearable", tag);

        assertTrue(tag.hasKey("wearableData"));
    }

    @Test
    public void persistedCoalJetpackRuntimeVariantsCollapseBeforeRegistryReconstruction() {
        NBTTagCompound cold = coalJetpackRuntimeTag(17, 8000, 0, false);
        NBTTagCompound hot = coalJetpackRuntimeTag(200, 5348, 16000, true);

        ResearchKey coldKey = PersistedResearchEntryResolver.resolve(
            COAL_JETPACK,
            0,
            ResearchNbtIdentity.canonicalize(cold),
            cold);
        ResearchKey hotKey = PersistedResearchEntryResolver.resolve(
            COAL_JETPACK,
            0,
            ResearchNbtIdentity.canonicalize(hot),
            hot);

        assertNotNull(coldKey);
        assertNotNull(hotKey);
        assertEquals(coldKey, hotKey);
        assertEquals("", coldKey.getCanonicalNbt());
    }

    @Test
    public void onlyIc2ElectricJetpackCollapsesTheFullChargeEndpoint() throws Exception {
        Method policy = Ic2ChargeStatePolicy.class.getDeclaredMethod("collapseFullEndpoint", String.class);
        policy.setAccessible(true);

        assertTrue(((Boolean) policy.invoke(null, IC2_ELECTRIC_JETPACK)).booleanValue());
        assertFalse(((Boolean) policy.invoke(null, "IC2:itemBatRE")).booleanValue());
        assertFalse(((Boolean) policy.invoke(null, "IC2:itemTreetapElectric")).booleanValue());
        assertFalse(((Boolean) policy.invoke(null, "test:foreignElectricItem")).booleanValue());
    }

    private static NBTTagCompound coalJetpackRuntimeTag(
        int temperature,
        int waterAmount,
        int steamAmount,
        boolean active) {
        NBTTagCompound root = new NBTTagCompound();
        NBTTagCompound wearable = new NBTTagCompound();
        wearable.setBoolean("boiling", active);
        wearable.setInteger("burnTicks", 1889309014);
        wearable.setInteger("coolTicks", -10455104);
        wearable.setInteger("currentBurn", 1889568000);
        wearable.setBoolean("inUse", active);
        wearable.setBoolean("leaking", active);
        wearable.setByte("status", (byte) (active ? 0 : 1));
        wearable.setInteger("temperature", temperature);

        NBTTagCompound waterTank = new NBTTagCompound();
        if (waterAmount > 0) {
            waterTank.setString("FluidName", "water");
            waterTank.setInteger("Amount", waterAmount);
        } else {
            waterTank.setString("Empty", "");
        }
        wearable.setTag("waterTank", waterTank);

        NBTTagCompound steamTank = new NBTTagCompound();
        if (steamAmount > 0) {
            steamTank.setString("FluidName", "steam");
            steamTank.setInteger("Amount", steamAmount);
        } else {
            steamTank.setString("Empty", "");
        }
        wearable.setTag("steamTank", steamTank);
        root.setTag("wearableData", wearable);
        return root;
    }
}
