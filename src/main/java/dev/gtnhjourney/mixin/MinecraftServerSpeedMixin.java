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
import dev.gtnhjourney.time.ServerTickOverrunGuard;
import dev.gtnhjourney.time.ServerTickPeriodSchedule;

/** Changes the stock server cadence and, above 32x, adds guarded bursts of complete MinecraftServer ticks. */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerSpeedMixin {

    @Unique
    private volatile int gtnhjourney$speedMultiplier = 1;
    @Unique
    private volatile int gtnhjourney$pacingHooksSeen;
    @Unique
    private int gtnhjourney$pacingPhase;
    @Unique
    private boolean gtnhjourney$insideBurst;
    @Unique
    private long gtnhjourney$outerTickStartNanos;
    @Unique
    private long gtnhjourney$lastOuterTickCostMillis;

    @ModifyConstant(method = "run", constant = @Constant(longValue = 50L, ordinal = 1), require = 0)
    private long gtnhjourney$modifyAccumulatorThreshold(long original) {
        gtnhjourney$pacingHooksSeen |= 1;
        return gtnhjourney$currentPeriod();
    }

    @ModifyConstant(method = "run", constant = @Constant(longValue = 50L, ordinal = 2), require = 0)
    private long gtnhjourney$modifyAccumulatorSubtraction(long original) {
        gtnhjourney$pacingHooksSeen |= 2;
        long period = gtnhjourney$currentPeriod();
        long subtraction = ServerTickOverrunGuard.subtractionMillis(
            gtnhjourney$speedMultiplier,
            period,
            gtnhjourney$lastOuterTickCostMillis);
        gtnhjourney$pacingPhase = ServerTickPeriodSchedule.nextPhase(
            gtnhjourney$speedMultiplier,
            gtnhjourney$pacingPhase);
        return subtraction;
    }

    @ModifyConstant(method = "run", constant = @Constant(longValue = 50L, ordinal = 3), require = 0)
    private long gtnhjourney$modifySleepPeriod(long original) {
        gtnhjourney$pacingHooksSeen |= 4;
        return gtnhjourney$currentPeriod();
    }

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void gtnhjourney$beginOuterTick(CallbackInfo ci) {
        if (!gtnhjourney$insideBurst) gtnhjourney$outerTickStartNanos = System.nanoTime();
    }

    @Inject(method = "tick", at = @At("RETURN"), require = 0)
    private void gtnhjourney$runHighSpeedBurst(CallbackInfo ci) {
        if (gtnhjourney$insideBurst) return;

        int fullTicks = ServerTickPeriodSchedule.fullTicksPerOuterTick(gtnhjourney$speedMultiplier);
        if (fullTicks > 1) {
            gtnhjourney$insideBurst = true;
            try {
                for (int i = 1; i < fullTicks; i++) {
                    ((MinecraftServer) (Object) this).tick();
                }
            } finally {
                gtnhjourney$insideBurst = false;
            }
        }

        long started = gtnhjourney$outerTickStartNanos;
        if (started > 0L) {
            long elapsedNanos = Math.max(0L, System.nanoTime() - started);
            gtnhjourney$lastOuterTickCostMillis = elapsedNanos / 1_000_000L;
        }
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
        gtnhjourney$insideBurst = false;
        gtnhjourney$outerTickStartNanos = 0L;
        gtnhjourney$lastOuterTickCostMillis = 0L;
        return true;
    }

    public void gtnhjourney$resetSpeedMultiplier() {
        gtnhjourney$speedMultiplier = 1;
        gtnhjourney$pacingPhase = 0;
        gtnhjourney$insideBurst = false;
        gtnhjourney$outerTickStartNanos = 0L;
        gtnhjourney$lastOuterTickCostMillis = 0L;
    }
}
