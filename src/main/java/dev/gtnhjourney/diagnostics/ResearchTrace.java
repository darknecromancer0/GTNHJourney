package dev.gtnhjourney.diagnostics;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import cpw.mods.fml.common.FMLLog;
import dev.gtnhjourney.minecraft.GtChargeStatePolicy;
import dev.gtnhjourney.minecraft.Ic2ChargeStatePolicy;
import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.research.ResearchKey;

/** Opt-in per-session research tracing for live compatibility tests. Never persists into the save. */
public final class ResearchTrace {

    private static final Set<UUID> ENABLED = Collections.synchronizedSet(new HashSet<UUID>());

    private ResearchTrace() {}

    public static boolean enabled(EntityPlayerMP player) {
        return player != null && ENABLED.contains(player.getUniqueID());
    }

    public static boolean set(EntityPlayerMP player, boolean enabled) {
        if (player == null) return false;
        if (enabled) ENABLED.add(player.getUniqueID());
        else ENABLED.remove(player.getUniqueID());
        return enabled;
    }

    public static void clear() {
        ENABLED.clear();
    }

    public static void unlocked(EntityPlayerMP player, ItemStack stack) {
        if (!enabled(player) || stack == null || stack.getItem() == null) return;
        try {
            ResearchKey key = ItemStackKeyFactory.from(stack);
            String text = "unlock " + key.getItemId()
                + " @"
                + key.getMeta()
                + (key.getCanonicalNbt()
                    .isEmpty() ? " [base]" : " [NBT]")
                + " GT="
                + GtChargeStatePolicy.describe(stack)
                + " IC2="
                + Ic2ChargeStatePolicy.describe(stack);
            player.addChatMessage(new ChatComponentText("[Journey trace] " + text));
            FMLLog.info("[GTNH Journey trace %s] %s", player.getUniqueID(), text);
        } catch (IllegalArgumentException failure) {
            FMLLog
                .info("[GTNH Journey trace %s] unlock identity failed: %s", player.getUniqueID(), failure.getMessage());
        }
    }
}
