package dev.gtnhjourney.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import codechicken.nei.ItemList;
import codechicken.nei.RestartableTask;
import dev.gtnhjourney.nei.JourneyNeiFilterRevision;

/** Mirrors NEI's own filter invalidation signal so Journey refreshes for search syntax and Item Subsets changes. */
@Mixin(value = RestartableTask.class, remap = false)
public abstract class RestartableTaskJourneyFilterMixin {

    @Inject(method = "restart", at = @At("HEAD"), remap = false)
    private void gtnhjourney$observeFilterRestart(CallbackInfo ci) {
        if ((Object) this == ItemList.updateFilter) JourneyNeiFilterRevision.invalidate();
    }
}
