package dev.loupe.gametest;

import net.minecraft.client.Minecraft;

final class CameraAccess {
    private CameraAccess() {
    }

    static float fov(Minecraft client) {
        return client.gameRenderer.getMainCamera().getFov();
    }
}
