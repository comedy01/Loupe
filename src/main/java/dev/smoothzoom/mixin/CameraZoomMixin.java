package dev.smoothzoom.mixin;

import dev.smoothzoom.client.SmoothZoomRuntime;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
abstract class CameraZoomMixin {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void smoothzoom$applyZoom(float partialTicks, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(SmoothZoomRuntime.modifyFov(cir.getReturnValueF()));
    }
}
