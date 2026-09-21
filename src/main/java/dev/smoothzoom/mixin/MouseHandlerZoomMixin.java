package dev.smoothzoom.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.smoothzoom.client.SmoothZoomRuntime;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class MouseHandlerZoomMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void smoothzoom$zoomWithWheel(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (SmoothZoomRuntime.onScroll(vertical)) {
            ci.cancel();
        }
    }

    @WrapOperation(
            method = "turnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void smoothzoom$easeLook(LocalPlayer player, double dx, double dy, Operation<Void> original) {
        SmoothZoomRuntime.smoothLook(dx, dy);
        original.call(player, SmoothZoomRuntime.lookX(), SmoothZoomRuntime.lookY());
    }
}
