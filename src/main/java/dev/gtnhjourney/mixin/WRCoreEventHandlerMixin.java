package dev.gtnhjourney.mixin;

import net.minecraft.client.renderer.Tessellator;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.gtnhjourney.client.RenderBoundaryRecovery;

/** Repairs a leaked shared Tessellator batch before WR-CBE starts its wireless-bolt batch. */
@Pseudo
@Mixin(targets = "codechicken.wirelessredstone.core.WRCoreEventHandler", remap = false)
public abstract class WRCoreEventHandlerMixin {

    @Inject(method = "onRenderWorldLast", at = @At("HEAD"), require = 0)
    private void gtnhjourney$finishDanglingTessellator(RenderWorldLastEvent event, CallbackInfo ci) {
        Tessellator tessellator = Tessellator.instance;
        boolean drawing = ((TessellatorStateAccessor) (Object) tessellator).gtnhjourney$isDrawing();
        RenderBoundaryRecovery.finishDanglingBatch(drawing, tessellator::draw);
    }
}
