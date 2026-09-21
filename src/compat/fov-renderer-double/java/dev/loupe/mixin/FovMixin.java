package dev.loupe.mixin;

import dev.loupe.client.LoupeRuntime;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
abstract class FovMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void loupe$applyZoom(Camera camera, float partialTicks, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        if (useFovSetting) {
            cir.setReturnValue((double) LoupeRuntime.modifyFov((float) cir.getReturnValueD()));
        }
    }
}
