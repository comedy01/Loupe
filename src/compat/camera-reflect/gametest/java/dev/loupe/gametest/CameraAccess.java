package dev.loupe.gametest;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;

import java.lang.reflect.Method;

final class CameraAccess {
    private static final Method GET_FOV = lookup();

    private CameraAccess() {
    }

    private static Method lookup() {
        try {
            Method method = GameRenderer.class.getDeclaredMethod("getFov", Camera.class, float.class, boolean.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static float fov(Minecraft client) {
        try {
            GameRenderer renderer = client.gameRenderer;
            return ((Number) GET_FOV.invoke(renderer, renderer.getMainCamera(), 1.0F, true)).floatValue();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
