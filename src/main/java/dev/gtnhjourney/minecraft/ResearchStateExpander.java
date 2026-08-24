package dev.gtnhjourney.minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

/** Expands one genuinely observed stack into the small set of semantic Journey endpoints it proves ownership of. */
public final class ResearchStateExpander {

    private ResearchStateExpander() {}

    public static List<ItemStack> expand(ItemStack observed) {
        if (observed == null || observed.getItem() == null) return Collections.emptyList();
        ItemStack exact = observed.copy();
        exact.stackSize = 1;
        GtChargeStatePolicy.State chargeState = ResearchCompatibilityOptions.normalizeGtChargeEndpoints()
            ? GtChargeStatePolicy.classify(observed)
            : GtChargeStatePolicy.State.EXACT;
        if (chargeState == GtChargeStatePolicy.State.BASE) {
            ItemStack base = GtChargeStatePolicy.withoutCharge(exact);
            base.stackSize = 1;
            return Collections.singletonList(base);
        }
        if (chargeState == GtChargeStatePolicy.State.FULL) {
            List<ItemStack> endpoints = new ArrayList<ItemStack>(2);
            ItemStack base = GtChargeStatePolicy.withoutCharge(exact);
            base.stackSize = 1;
            endpoints.add(base);
            endpoints.add(exact);
            return Collections.unmodifiableList(endpoints);
        }
        if (ResearchCompatibilityOptions.normalizeIc2ChargeEndpoints()) {
            Ic2ChargeStatePolicy.State ic2State = Ic2ChargeStatePolicy.classify(exact);
            if (ic2State != Ic2ChargeStatePolicy.State.EXACT) return Ic2ChargeStatePolicy.expand(exact);
        }
        OpenComputersChargeStatePolicy.State ocState = OpenComputersChargeStatePolicy.classify(exact);
        if (ocState != OpenComputersChargeStatePolicy.State.EXACT) return OpenComputersChargeStatePolicy.expand(exact);
        CofhChargeStatePolicy.State cofhState = CofhChargeStatePolicy.classify(exact);
        if (cofhState != CofhChargeStatePolicy.State.EXACT) return CofhChargeStatePolicy.expand(exact);
        return Collections.singletonList(exact);
    }
}
