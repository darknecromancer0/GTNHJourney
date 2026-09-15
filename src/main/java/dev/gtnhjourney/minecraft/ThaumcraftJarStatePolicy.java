package dev.gtnhjourney.minecraft;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import cpw.mods.fml.common.registry.GameRegistry;

/** Keeps Thaumcraft warded jars semantic by aspect/filter while collapsing partial fill amounts to a full jar. */
public final class ThaumcraftJarStatePolicy {

    private static final String FILLED_JAR = "Thaumcraft:BlockJarFilledItem";
    private static final int JAR_CAPACITY = 64;

    private ThaumcraftJarStatePolicy() {}

    public static void normalize(ItemStack stack, NBTTagCompound tag) {
        if (stack == null || stack.getItem() == null || tag == null) return;
        String registryId = registryId(stack);
        normalize(registryId, tag);
    }

    public static void normalize(String registryId, NBTTagCompound tag) {
        if (!FILLED_JAR.equals(registryId) || tag == null || !tag.hasKey("Aspects", 9)) return;
        NBTTagList aspects = tag.getTagList("Aspects", 10);
        // Warded jars hold one essentia aspect. Unknown/malformed multi-aspect payloads remain exact/fail-closed.
        if (aspects == null || aspects.tagCount() != 1) return;
        NBTTagCompound aspect = aspects.getCompoundTagAt(0);
        if (aspect == null || !aspect.hasKey("key", 8) || aspect.getString("key").isEmpty()) return;
        if (!aspect.hasKey("amount", 99) || aspect.getInteger("amount") <= 0) return;
        aspect.setInteger("amount", JAR_CAPACITY);
        // AspectFilter is deliberately untouched: a labelled jar and an unlabelled jar remain distinct semantic states.
    }

    private static String registryId(ItemStack stack) {
        try {
            GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
            return id == null ? null : id.toString();
        } catch (RuntimeException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }
}
