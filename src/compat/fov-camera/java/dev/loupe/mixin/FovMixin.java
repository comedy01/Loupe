package dev.loupe.mixin;

import dev.loupe.client.LoupeRuntime;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
abstract class FovMixin {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void loupe$applyZoom(float partialTicks, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(LoupeRuntime.modifyFov(cir.getReturnValueF()));
    }
}
