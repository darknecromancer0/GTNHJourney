package dev.gtnhjourney.recovery;

import java.lang.reflect.Method;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

/** Narrow reflection bridge so death recovery covers Baubles without a hard runtime dependency. */
final class OptionalBaublesInventoryAccess {

    private static Method getBaubles;
    private static boolean unavailable;

    private OptionalBaublesInventoryAccess() {}

    static IInventory get(EntityPlayer player) {
        if (player == null || unavailable) return null;
        try {
            if (getBaubles == null) {
                Class<?> api = Class.forName("baubles.api.BaublesApi", false, OptionalBaublesInventoryAccess.class.getClassLoader());
                getBaubles = api.getMethod("getBaubles", EntityPlayer.class);
            }
            Object value = getBaubles.invoke(null, player);
            return value instanceof IInventory ? (IInventory) value : null;
        } catch (ClassNotFoundException ignored) {
            unavailable = true;
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
