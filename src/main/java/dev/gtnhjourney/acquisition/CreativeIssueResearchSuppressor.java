package dev.gtnhjourney.acquisition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.research.ResearchFingerprint;
import dev.gtnhjourney.research.ResearchKey;

/**
 * Session-side exclusion for exact identities issued from C. The exclusion remains only while that identity is still
 * present in the player's main inventory, preventing the ordinary inventory scanner from turning C into research.
 */
public final class CreativeIssueResearchSuppressor {

    private static final Map<UUID, Set<ResearchFingerprint>> SUPPRESSED =
        new HashMap<UUID, Set<ResearchFingerprint>>();

    private CreativeIssueResearchSuppressor() {}

    public static synchronized void mark(EntityPlayerMP player, ItemStack stack) {
        ResearchFingerprint fingerprint = fingerprint(stack);
        UUID playerId = player == null ? null : player.getUniqueID();
        if (playerId == null || fingerprint == null) return;
        Set<ResearchFingerprint> values = SUPPRESSED.get(playerId);
        if (values == null) {
            values = new HashSet<ResearchFingerprint>();
            SUPPRESSED.put(playerId, values);
        }
        values.add(fingerprint);
    }

    public static synchronized boolean shouldSuppress(EntityPlayerMP player, ItemStack stack) {
        UUID playerId = player == null ? null : player.getUniqueID();
        if (playerId == null) return false;
        Set<ResearchFingerprint> values = SUPPRESSED.get(playerId);
        if (values == null || values.isEmpty()) return false;
        ResearchFingerprint fingerprint = fingerprint(stack);
        return fingerprint != null && values.contains(fingerprint);
    }

    /** Releases exclusions as soon as no matching C-issued identity remains in the main inventory. */
    public static synchronized void retainPresent(EntityPlayerMP player) {
        UUID playerId = player == null ? null : player.getUniqueID();
        if (playerId == null || player.inventory == null) return;
        Set<ResearchFingerprint> values = SUPPRESSED.get(playerId);
        if (values == null || values.isEmpty()) return;

        Set<ResearchFingerprint> present = new HashSet<ResearchFingerprint>();
        ItemStack[] main = player.inventory.mainInventory;
        if (main != null) {
            for (ItemStack stack : main) {
                ResearchFingerprint fingerprint = fingerprint(stack);
                if (fingerprint != null && values.contains(fingerprint)) present.add(fingerprint);
            }
        }
        values.retainAll(present);
        if (values.isEmpty()) SUPPRESSED.remove(playerId);
    }

    public static synchronized void clear(EntityPlayerMP player) {
        if (player != null && player.getUniqueID() != null) SUPPRESSED.remove(player.getUniqueID());
    }

    public static synchronized void clearAll() {
        SUPPRESSED.clear();
    }

    private static ResearchFingerprint fingerprint(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        try {
            ResearchKey key = ItemStackKeyFactory.from(stack);
            return key == null ? null : ResearchFingerprint.of(key);
        } catch (IllegalArgumentException ignored) {
            return null;
        } catch (RuntimeException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }
}
