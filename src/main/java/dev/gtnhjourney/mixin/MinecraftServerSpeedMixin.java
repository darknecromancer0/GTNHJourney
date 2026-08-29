package dev.gtnhjourney.mixin;

import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.gtnhjourney.time.JourneySpeedState;
import dev.gtnhjourney.time.ServerTickPeriodSchedule;

/** Changes only MinecraftServer's normal run-loop cadence; worlds still advance through one stock tick call per loop. */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerSpeedMixin {

    @Unique
    private volatile int gtnhjourney$speedMultiplier = 1;
    @Unique
    private volatile int gtnhjourney$pacingHooksSeen;
    @Unique
    private int gtnhjourney$pacingPhase;

    @ModifyConstant(method = "run", constant = @Constant(longValue = 50L, ordinal = 1), require = 0)
    private long gtnhjourney$modifyAccumulatorThreshold(long original) {
        gtnhjourney$pacingHooksSeen |= 1;
        return gtnhjourney$currentPeriod();
    }

    @ModifyConstant(method = "run", constant = @Constant(longValue = 50L, ordinal = 2), require = 0)
    private long gtnhjourney$modifyAccumulatorSubtraction(long original) {
        gtnhjourney$pacingHooksSeen |= 2;
        return gtnhjourney$currentPeriod();
    }

    @ModifyConstant(method = "run", constant = @Constant(longValue = 50L, ordinal = 3), require = 0)
    private long gtnhjourney$modifySleepPeriod(long original) {
        gtnhjourney$pacingHooksSeen |= 4;
        return gtnhjourney$currentPeriod();
    }

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void gtnhjourney$advancePacingPhase(CallbackInfo ci) {
        gtnhjourney$pacingPhase = ServerTickPeriodSchedule.nextPhase(
            gtnhjourney$speedMultiplier,
            gtnhjourney$pacingPhase);
    }

    @Unique
    private long gtnhjourney$currentPeriod() {
        return ServerTickPeriodSchedule.periodMillis(gtnhjourney$speedMultiplier, gtnhjourney$pacingPhase);
    }

    public boolean gtnhjourney$isSpeedHookAvailable() {
        return (gtnhjourney$pacingHooksSeen & 7) == 7;
    }

    public boolean gtnhjourney$setSpeedMultiplier(int multiplier) {
        if (!gtnhjourney$isSpeedHookAvailable() || !JourneySpeedState.isAllowedMultiplier(multiplier)) return false;
        gtnhjourney$speedMultiplier = multiplier;
        gtnhjourney$pacingPhase = 0;
        return true;
    }

    public void gtnhjourney$resetSpeedMultiplier() {
        gtnhjourney$speedMultiplier = 1;
        gtnhjourney$pacingPhase = 0;
    }
}
