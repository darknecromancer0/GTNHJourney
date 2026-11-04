package dev.gtnhjourney.mixin;

import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.world.GameRules;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.gtnhjourney.config.JourneyConfig;
import dev.gtnhjourney.safety.EndermanBlockCarryPolicy;

/**
 * Vanilla 1.7.10 checks mobGriefing once around Enderman block pickup and placement in onLivingUpdate.
 * Change only that Enderman-local answer, preserving the gamerule and other mobs' behavior.
 * Existing carried blocks remain carried while disabled and may be placed normally after re-enabling.
 */
@Mixin(EntityEnderman.class)
public abstract class EntityEndermanBlockCarryMixin {

    @Redirect(
        method = "onLivingUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/GameRules;getGameRuleBooleanValue(Ljava/lang/String;)Z"),
        require = 1)
    private boolean gtnhjourney$allowEndermanBlockMovement(GameRules rules, String ruleName) {
        boolean vanilla = rules.getGameRuleBooleanValue(ruleName);
        if (!"mobGriefing".equals(ruleName)) return vanilla;
        return EndermanBlockCarryPolicy.mayMoveBlocks(vanilla, JourneyConfig.endermenMoveBlocksEnabled());
    }
}
