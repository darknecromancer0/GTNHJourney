package dev.gtnhjourney.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.gtnhjourney.config.JourneyConfig;

/** Prevents GregTech machines and multiblock hatches from destroying themselves when Journey explosions are off. */
@Mixin(targets = "gregtech.api.metatileentity.BaseMetaTileEntity", remap = false)
public abstract class GregTechBaseMetaTileExplosionMixin {

    @Inject(method = "doExplosion", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void gtnhjourney$cancelDirectMachineExplosion(long amount, CallbackInfo ci) {
        if (!JourneyConfig.explosionsEnabled()) ci.cancel();
    }
}
