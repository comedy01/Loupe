package dev.loupe.client;

import dev.loupe.zoom.LookSmoother;
import dev.loupe.zoom.ZoomMath;
import dev.loupe.zoom.ZoomController;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.lang.ref.WeakReference;

public final class LoupeRuntime {
    private static final ZoomController CONTROLLER = new ZoomController();
    private static final LookSmoother LOOK = new LookSmoother();

    private static WeakReference<ClientLevel> lastLevel = new WeakReference<>(null);

    private LoupeRuntime() {
    }

    public static float modifyFov(float vanillaFov) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return vanillaFov;
        }

        trackWorld(client);

        return CONTROLLER.modifyFov(vanillaFov, zoomHeld(client), LoupeClient.config(), System.nanoTime());
    }

    public static boolean onScroll(double vertical) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return false;
        }
        return CONTROLLER.onScroll(vertical, zoomHeld(client), LoupeClient.config());
    }

    public static void smoothLook(double dx, double dy) {
        Minecraft client = Minecraft.getInstance();
        boolean gameSmooths = client != null && client.options.smoothCamera;
        double magnification = gameSmooths ? 1.0 : CONTROLLER.magnification();
        LOOK.advance(dx, dy, magnification, LoupeClient.config().lookSmoothing(), System.nanoTime());
    }

    public static double lookX() {
        return LOOK.x();
    }

    public static double lookY() {
        return LOOK.y();
    }

    public static void reset() {
        CONTROLLER.reset();
        LOOK.reset();
    }

    private static void trackWorld(Minecraft client) {
        ClientLevel level = client.level;
        if (lastLevel.get() != level) {
            reset();
            lastLevel = new WeakReference<>(level);
        }
    }

    private static boolean zoomHeld(Minecraft client) {
        KeyMapping key = LoupeClient.zoomKey();

        boolean inWorld = client.level != null && client.player != null && client.getWindow() != null;
        boolean screenOpen = client.gui != null && client.gui.screen() != null;
        boolean keyBound = key != null && !key.isUnbound();

        return ZoomMath.canZoom(inWorld, screenOpen, keyBound) && key.isDown();
    }
}
