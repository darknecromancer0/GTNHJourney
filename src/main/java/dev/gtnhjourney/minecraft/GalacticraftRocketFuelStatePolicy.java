package dev.gtnhjourney.minecraft;

import java.lang.reflect.Field;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Canonicalizes the verified Galacticraft rocket fuel payload to the two useful Journey endpoints.
 * Rocket metadata remains untouched because it encodes functional variants such as cargo/chest layouts.
 */
public final class GalacticraftRocketFuelStatePolicy {

    private static final String TIER_ONE = "GalacticraftCore:item.spaceship";
    private static final String TIER_TWO = "GalacticraftMars:item.spaceshipTier2";
    private static final String TIER_THREE = "GalacticraftMars:item.itemTier3Rocket";
    private static final String FUEL_KEY = "RocketFuel";

    private GalacticraftRocketFuelStatePolicy() {}

    public static void normalize(String itemId, NBTTagCompound tag) {
        normalizeForTests(itemId, tag, runtimeFuelFactor());
    }

    static void normalizeForTests(String itemId, NBTTagCompound tag, int fuelFactor) {
        if (tag == null) return;
        int baseCapacity = baseCapacity(itemId);
        if (baseCapacity <= 0) return;

        int fuel = tag.hasKey(FUEL_KEY) ? tag.getInteger(FUEL_KEY) : 0;
        if (fuel <= 0) {
            tag.removeTag(FUEL_KEY);
            return;
        }

        int factor = Math.max(1, fuelFactor);
        long full = (long) baseCapacity * (long) factor;
        tag.setInteger(FUEL_KEY, (int) Math.min(Integer.MAX_VALUE, full));
    }

    static boolean matches(String itemId) {
        return baseCapacity(itemId) > 0;
    }

    private static int baseCapacity(String itemId) {
        if (TIER_ONE.equals(itemId)) return 1000;
        if (TIER_TWO.equals(itemId) || TIER_THREE.equals(itemId)) return 1500;
        return -1;
    }

    private static int runtimeFuelFactor() {
        try {
            Class<?> config = Class.forName("micdoodle8.mods.galacticraft.core.util.ConfigManagerCore");
            Field field = config.getField("rocketFuelFactor");
            Object value = field.get(null);
            if (value instanceof Number) return Math.max(1, ((Number) value).intValue());
        } catch (ReflectiveOperationException ignored) {
        } catch (RuntimeException ignored) {
        } catch (LinkageError ignored) {}
        return 1;
    }
}
