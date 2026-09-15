package dev.gtnhjourney.minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.registry.GameRegistry;

/** GalaxySpace Spacesuit Jetplate exposes one continuously changing electricity float; Journey keeps only 0%/100%. */
public final class GalaxySpaceChargeStatePolicy {

    private static final String JETPLATE = "GalaxySpace:item.spacesuit_jetplate";
    private static final int JETPLATE_META = 100;
    private static final String CHARGE_KEY = "electricity";
    private static final float MAX_CHARGE = 100000.0F;

    enum State {
        EXACT,
        BASE,
        FULL
    }

    private GalaxySpaceChargeStatePolicy() {}

    public static ItemStack identityStack(ItemStack observed) {
        if (observed == null) return null;
        State state = classify(observed);
        if (state == State.BASE) return withoutCharge(observed);
        if (state == State.FULL) return toFull(observed);
        return observed.copy();
    }

    public static List<ItemStack> expand(ItemStack observed) {
        if (observed == null || observed.getItem() == null) return Collections.emptyList();
        State state = classify(observed);
        if (state == State.EXACT) return Collections.singletonList(observed.copy());
        ItemStack base = withoutCharge(observed);
        base.stackSize = 1;
        if (state == State.BASE) return Collections.singletonList(base);
        ItemStack full = toFull(observed);
        full.stackSize = 1;
        List<ItemStack> endpoints = new ArrayList<ItemStack>(2);
        endpoints.add(base);
        endpoints.add(full);
        return Collections.unmodifiableList(endpoints);
    }

    static State classify(ItemStack observed) {
        if (!matches(observed)) return State.EXACT;
        NBTTagCompound tag = observed.getTagCompound();
        if (tag == null || !tag.hasKey(CHARGE_KEY, 99)) return State.BASE;
        float charge = tag.getFloat(CHARGE_KEY);
        if (Float.isNaN(charge) || Float.isInfinite(charge) || charge < 0.0F) return State.EXACT;
        return charge > 0.0F ? State.FULL : State.BASE;
    }

    static ItemStack withoutCharge(ItemStack stack) {
        ItemStack copy = stack.copy();
        if (!copy.hasTagCompound()) return copy;
        NBTTagCompound tag = (NBTTagCompound) copy.getTagCompound().copy();
        tag.removeTag(CHARGE_KEY);
        if (tag.func_150296_c().isEmpty()) copy.setTagCompound(null);
        else copy.setTagCompound(tag);
        return copy;
    }

    static ItemStack toFull(ItemStack stack) {
        ItemStack copy = stack.copy();
        NBTTagCompound tag = copy.hasTagCompound() ? (NBTTagCompound) copy.getTagCompound().copy() : new NBTTagCompound();
        tag.setFloat(CHARGE_KEY, MAX_CHARGE);
        copy.setTagCompound(tag);
        return copy;
    }

    static void normalizePersisted(String registryId, int meta, NBTTagCompound tag) {
        if (!JETPLATE.equals(registryId) || meta != JETPLATE_META || tag == null || !tag.hasKey(CHARGE_KEY, 99)) return;
        float charge = tag.getFloat(CHARGE_KEY);
        if (Float.isNaN(charge) || Float.isInfinite(charge) || charge < 0.0F) return;
        if (charge <= 0.0F) tag.removeTag(CHARGE_KEY);
        else tag.setFloat(CHARGE_KEY, MAX_CHARGE);
    }

    private static boolean matches(ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.getItemDamage() != JETPLATE_META) return false;
        try {
            GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
            return id != null && JETPLATE.equals(id.toString());
        } catch (RuntimeException ignored) {
            return false;
        } catch (LinkageError ignored) {
            return false;
        }
    }
}
