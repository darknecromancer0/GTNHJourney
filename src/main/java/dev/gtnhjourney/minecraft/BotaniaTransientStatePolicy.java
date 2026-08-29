package dev.gtnhjourney.minecraft;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Removes narrowly verified Botania runtime/use state from research identity and retrieval templates. */
public final class BotaniaTransientStatePolicy {

    private static final String MAGNET_RING_CLASS = "vazkii.botania.common.item.equipment.bauble.ItemMagnetRing";
    private static final String SPECIAL_FLOWER = "Botania:specialFlower";
    private static final String TWIG_WAND = "Botania:twigWand";
    private static final String COOLDOWN_KEY = "cooldown";
    private static final String PASSIVE_DECAY_TICKS = "passiveDecayTicks";
    private static final Class<?> MAGNET_RING = load(MAGNET_RING_CLASS);

    private BotaniaTransientStatePolicy() {}

    public static boolean isVerifiedMagnetRing(ItemStack stack) {
        return stack != null && stack.getItem() != null
            && MAGNET_RING != null
            && MAGNET_RING.isInstance(stack.getItem());
    }

    public static boolean isRuntimeAvailable() {
        return MAGNET_RING != null;
    }

    public static void normalize(ItemStack owner, NBTTagCompound tag) {
        if (tag == null) return;
        if (isVerifiedMagnetRing(owner)) tag.removeTag(COOLDOWN_KEY);
        String registryId = registryId(owner);
        if (registryId != null) normalize(registryId, tag);
    }

    static void normalize(String registryId, NBTTagCompound tag) {
        if (registryId == null || tag == null) return;
        if (SPECIAL_FLOWER.equals(registryId)) {
            // Passive flowers increment this lifetime counter while placed. Flower type remains the semantic identity.
            tag.removeTag(PASSIVE_DECAY_TICKS);
            return;
        }
        if (TWIG_WAND.equals(registryId)) {
            // The Wand of the Forest remembers the last bound tile coordinates. Colors/configuration stay semantic.
            tag.removeTag("boundTileX");
            tag.removeTag("boundTileY");
            tag.removeTag("boundTileZ");
        }
    }

    private static String registryId(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        try {
            GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
            return id == null ? null : id.toString();
        } catch (RuntimeException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name, false, BotaniaTransientStatePolicy.class.getClassLoader());
        } catch (ClassNotFoundException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }
}
