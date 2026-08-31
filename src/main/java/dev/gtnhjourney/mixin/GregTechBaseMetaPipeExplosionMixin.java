package dev.gtnhjourney.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.gtnhjourney.config.JourneyConfig;

/** Prevents GregTech pipes/cables from entering their direct explosion path when Journey explosions are off. */
@Mixin(targets = "gregtech.api.metatileentity.BaseMetaPipeEntity", remap = false)
public abstract class GregTechBaseMetaPipeExplosionMixin {

    @Inject(method = "doExplosion", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void gtnhjourney$cancelDirectPipeExplosion(long amount, CallbackInfo ci) {
        if (!JourneyConfig.explosionsEnabled()) ci.cancel();
    }
}
