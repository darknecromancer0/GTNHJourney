package dev.gtnhjourney.minecraft;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/** Removes only Draconic Evolution's auto-created five-empty-profile initialization payload. */
public final class DraconicTransientStatePolicy {

    private static final String TOOL_BASE_CLASS = "com.brandon3055.draconicevolution.common.items.tools.baseclasses.ToolBase";
    private static final String PROFILES_KEY = "ConfigProfiles";
    private static final Class<?> TOOL_BASE = load(TOOL_BASE_CLASS);

    private DraconicTransientStatePolicy() {}

    public static boolean isVerifiedTool(ItemStack stack) {
        return stack != null && stack.getItem() != null && TOOL_BASE != null && TOOL_BASE.isInstance(stack.getItem());
    }

    public static boolean isRuntimeAvailable() {
        return TOOL_BASE != null;
    }

    public static void normalize(ItemStack owner, NBTTagCompound tag) {
        if (tag == null || !isVerifiedTool(owner)) return;
        NBTBase raw = tag.getTag(PROFILES_KEY);
        if (!(raw instanceof NBTTagList)) return;
        NBTTagList profiles = (NBTTagList) raw.copy();
        if (profiles.tagCount() != 5) return;
        while (profiles.tagCount() > 0) {
            NBTBase profile = profiles.removeTag(profiles.tagCount() - 1);
            if (!(profile instanceof NBTTagCompound)) return;
            if (!((NBTTagCompound) profile).func_150296_c()
                .isEmpty()) return;
        }
        tag.removeTag(PROFILES_KEY);
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name, false, DraconicTransientStatePolicy.class.getClassLoader());
        } catch (ClassNotFoundException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }
}
