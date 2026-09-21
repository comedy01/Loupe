package dev.loupe.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.loupe.client.GameScreens;
import dev.loupe.client.LoupeClient;
import dev.loupe.client.gui.LoupeSettingsScreen;
import dev.loupe.config.ZoomConfig;
import dev.loupe.zoom.ZoomMath;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Mth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import java.io.IOException;
import java.nio.file.Files;

public class LoupeClientGameTest implements FabricClientGameTest {
    private static final float STEP = (float) ZoomMath.SCROLL_STEP;
    private static final float EXACT = 1.0e-6F;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            context.waitTicks(20);
            runInWorld(context);
        }
        log("ALL CHECKS PASSED");
    }

    private void runInWorld(ClientGameTestContext context) {
        TestInput input = context.getInput();
        ZoomConfig config = LoupeClient.config();
        config.resetToDefaults();

        float base = fov(context);
        log("baseline FOV = " + base);
        check(base > 30.0F && base < 120.0F, "unexpected baseline FOV " + base);

        context.waitTicks(10);
        checkNear(fov(context), base, 0.001F, "FOV changed with the key up");

        input.holdKey(code("key.keyboard.c"));
        int ticks = waitForFov(context, base / 4.0F, 0.05F, "zoom in to 4x");
        log("zoomed in to " + fov(context) + " (target " + base / 4.0F + ") after " + ticks + " ticks");

        int slot = slot(context);
        for (int i = 0; i < 3; i++) {
            input.scroll(1.0);
            context.waitTick();
        }
        waitForFov(context, base / (4.0F * STEP * STEP * STEP), 0.05F, "3 notches in");
        check(slot(context) == slot, "hotbar scrolled while zooming (slot " + slot + " -> " + slot(context) + ")");
        log("scroll up x3 -> " + fov(context) + ", hotbar slot unchanged at " + slot);

        for (int i = 0; i < 3; i++) {
            input.scroll(-1.0);
            context.waitTick();
        }
        waitForFov(context, base / 4.0F, 0.05F, "3 notches back out");
        input.scroll(-2.0);
        context.waitTick();
        waitForFov(context, base / (4.0F * (float) Math.pow(ZoomMath.SCROLL_STEP, -2.0)), 0.05F, "scroll down");
        log("scroll down -> " + fov(context) + ", hotbar slot " + slot(context));
        check(slot(context) == slot, "hotbar scrolled while zooming out");

        input.releaseKey(code("key.keyboard.c"));
        waitForFov(context, base, EXACT, "zoom out on release");
        log("released -> " + fov(context));

        input.scroll(1.0);
        context.waitTicks(2);
        check(slot(context) != slot, "the wheel did not scroll the hotbar with the zoom key up");
        log("key up, wheel scrolls hotbar: slot " + slot + " -> " + slot(context));
        input.scroll(-1.0);
        context.waitTicks(2);
        check(slot(context) == slot, "could not restore the hotbar slot");

        input.holdKey(code("key.keyboard.c"));
        waitForFov(context, base / 4.0F, 0.05F, "zoom in before scrolling");
        for (int i = 0; i < 4; i++) {
            input.scroll(1.0);
            context.waitTick();
        }
        waitForFov(context, base / (4.0F * (float) Math.pow(ZoomMath.SCROLL_STEP, 4.0)), 0.05F, "scrolled in");
        input.releaseKey(code("key.keyboard.c"));
        waitForFov(context, base, EXACT, "zoom out after scrolling");
        input.holdKey(code("key.keyboard.c"));
        waitForFov(context, base / 4.0F, 0.05F, "second zoom starts at the configured amount");
        log("second press returned to configured amount: " + fov(context));
        input.releaseKey(code("key.keyboard.c"));
        waitForFov(context, base, EXACT, "release");

        config.setScrollToZoom(false);
        input.holdKey(code("key.keyboard.c"));
        waitForFov(context, base / 4.0F, 0.05F, "zoom in, scroll disabled");
        input.scroll(1.0);
        context.waitTicks(3);
        check(slot(context) != slot, "wheel was swallowed although scroll-to-zoom is off");
        checkNear(fov(context), base / 4.0F, 0.05F, "FOV moved although scroll-to-zoom is off");
        log("scroll-to-zoom off: wheel reached the hotbar, FOV stayed " + fov(context));
        input.scroll(-1.0);
        context.waitTicks(2);
        input.releaseKey(code("key.keyboard.c"));
        waitForFov(context, base, EXACT, "release");
        config.setScrollToZoom(true);

        config.setZoomAmount(2.0);
        input.holdKey(code("key.keyboard.c"));
        waitForFov(context, base / 2.0F, 0.05F, "2x amount");
        log("amount 2.0 -> " + fov(context));
        input.releaseKey(code("key.keyboard.c"));
        waitForFov(context, base, EXACT, "release");
        config.resetToDefaults();

        context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
        context.waitTicks(5);
        input.holdKey(code("key.keyboard.c"));
        context.waitTicks(30);
        checkNear(fov(context), base, 0.001F, "zoomed behind an open inventory");
        log("inventory open, C held -> FOV " + fov(context) + " (no zoom)");
        input.releaseKey(code("key.keyboard.c"));
        context.setScreen(() -> null);
        context.waitTicks(5);

        KeyMapping key = LoupeClient.zoomKey();
        check(key != null, "zoom key was not registered");
        rebind(context, key, InputConstants.UNKNOWN);
        input.holdKey(code("key.keyboard.c"));
        context.waitTicks(30);
        checkNear(fov(context), base, 0.001F, "zoomed although the key is unbound");
        log("key unbound, C held -> FOV " + fov(context) + " (no zoom)");
        input.releaseKey(code("key.keyboard.c"));

        rebind(context, key, InputConstants.getKey("key.mouse.middle"));
        input.holdMouse(code("key.mouse.middle"));
        waitForFov(context, base / 4.0F, 0.05F, "zoom on a mouse button");
        log("bound to middle mouse -> FOV " + fov(context));
        input.releaseMouse(code("key.mouse.middle"));
        waitForFov(context, base, EXACT, "release mouse button");

        rebind(context, key, InputConstants.getKey("key.keyboard.v"));
        input.holdKey(code("key.keyboard.c"));
        context.waitTicks(20);
        checkNear(fov(context), base, 0.001F, "old key still zooms after rebinding");
        input.releaseKey(code("key.keyboard.c"));
        input.holdKey(code("key.keyboard.v"));
        waitForFov(context, base / 4.0F, 0.05F, "zoom on rebound key V");
        log("rebound to V -> FOV " + fov(context) + ", C no longer zooms");
        input.releaseKey(code("key.keyboard.v"));
        waitForFov(context, base, EXACT, "release V");
        rebind(context, key, key.getDefaultKey());
        check(key.getDefaultKey().getValue() == code("key.keyboard.c"), "default key is not C");

        input.moveCursor(1.0, 0.0);
        context.waitTicks(3);
        float[] direct = sweep(context, 200.0, 30);
        float directTotal = Math.abs(direct[direct.length - 1]);
        log("look sweep, not zoomed: " + describe(direct));
        check(directTotal > 1.0F, "the mouse did not turn the view at all (" + directTotal + " degrees); is the mouse grabbed?");
        check(Math.abs(direct[0]) >= 0.9F * directTotal, "look was eased although not zoomed: " + describe(direct));
        sweep(context, -200.0, 10);

        input.holdKey(code("key.keyboard.c"));
        waitForFov(context, base / 4.0F, 0.05F, "zoom in for look smoothing");
        float[] eased = sweep(context, 200.0, 40);
        log("look sweep, zoomed 4x: " + describe(eased));
        check(Math.abs(eased[0]) < 0.5F * directTotal, "the view jumped to the mouse while zoomed: " + describe(eased));
        for (int i = 1; i < eased.length; i++) {
            check(Math.abs(eased[i]) >= Math.abs(eased[i - 1]) - 0.001F, "the view moved back while gliding: " + describe(eased));
        }
        checkNear(Math.abs(eased[eased.length - 1]), directTotal, directTotal * 0.03F, "eased look did not end where the mouse put it");
        sweep(context, -200.0, 40);

        config.setLookSmoothing(0.0);
        float[] off = sweep(context, 200.0, 30);
        check(Math.abs(off[0]) >= 0.9F * directTotal, "look was eased with smoothing off: " + describe(off));
        log("look sweep, zoomed with smoothing off: " + describe(off));
        config.resetToDefaults();
        sweep(context, -200.0, 10);
        input.releaseKey(code("key.keyboard.c"));
        waitForFov(context, base, EXACT, "release after look smoothing");

        config.setZoomAmount(9.0);
        config.setZoomSpeed(3.0);
        config.setScrollToZoom(false);
        context.setScreen(() -> new LoupeSettingsScreen(null, Minecraft.getInstance().options));
        context.waitForScreen(LoupeSettingsScreen.class);
        context.waitTicks(5);
        log("screenshot: " + context.takeScreenshot("loupe-settings"));
        clickButton(context, "loupe.options.reset");
        context.waitTicks(5);
        check(config.zoomAmount() == ZoomMath.DEFAULT_AMOUNT, "reset did not restore the amount");
        check(config.zoomSpeed() == ZoomMath.DEFAULT_SPEED, "reset did not restore the speed");
        check(config.scrollToZoom(), "reset did not restore scroll-to-zoom");
        log("screenshot: " + context.takeScreenshot("loupe-settings-after-reset"));

        config.setZoomAmount(6.5);
        context.setScreen(() -> null);
        context.waitTicks(5);
        String saved = readConfigFile();
        check(saved.contains("\"zoomAmount\": 6.5"), "settings were not saved on close: " + saved);
        log("saved on close: " + saved.replace('\n', ' '));
        config.resetToDefaults();
        LoupeClient.saveConfig();
    }

    private static float fov(ClientGameTestContext context) {
        return context.computeOnClient(client -> CameraAccess.fov(client));
    }

    private static float yaw(ClientGameTestContext context) {
        return context.computeOnClient(client -> client.player.getYRot());
    }

    private static float[] sweep(ClientGameTestContext context, double pixels, int ticks) {
        float start = yaw(context);
        context.getInput().moveCursor(pixels, 0.0);
        float[] turned = new float[ticks];
        for (int i = 0; i < ticks; i++) {
            context.waitTick();
            turned[i] = Mth.wrapDegrees(yaw(context) - start);
        }
        return turned;
    }

    private static String describe(float[] turned) {
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < turned.length; i++) {
            if (i == 0 || i == 1 || i == 2 || i == 4 || i == 9 || i == turned.length - 1) {
                text.append(String.format(java.util.Locale.ROOT, "t%d=%.2f ", i + 1, turned[i]));
            }
        }
        return text.toString().trim() + "]";
    }

    private static int slot(ClientGameTestContext context) {
        return context.computeOnClient(client -> client.player.getInventory().getSelectedSlot());
    }

    private static int waitForFov(ClientGameTestContext context, float target, float tolerance, String what) {
        try {
            return context.waitFor(
                    client -> Math.abs(CameraAccess.fov(client) - target) <= tolerance,
                    400);
        } catch (RuntimeException | AssertionError e) {
            throw new AssertionError("FOV never reached " + target + " (+/- " + tolerance + ") for: " + what
                    + "; it is " + fov(context), e);
        }
    }

    private static int code(String name) {
        return InputConstants.getKey(name).getValue();
    }

    private static void rebind(ClientGameTestContext context, KeyMapping key, InputConstants.Key to) {
        context.runOnClient(client -> {
            key.setKey(to);
            KeyMapping.resetMapping();
        });
    }

    private static void clickButton(ClientGameTestContext context, String translationKey) {
        double[] center = context.computeOnClient(client -> {
            Button button = findButton(GameScreens.current(client), translationKey);
            if (button == null) {
                throw new AssertionError("no button '" + translationKey + "' on the current screen");
            }
            double scale = client.getWindow().getGuiScale();
            return new double[] {
                    (button.getX() + button.getWidth() / 2.0) * scale,
                    (button.getY() + button.getHeight() / 2.0) * scale};
        });
        context.getInput().setCursorPos(center[0], center[1]);
        context.waitTick();
        context.getInput().pressMouse(code("key.mouse.left"));
    }

    private static Button findButton(GuiEventListener node, String translationKey) {
        if (node instanceof Button button
                && button.getMessage().getContents() instanceof TranslatableContents contents
                && contents.getKey().equals(translationKey)) {
            return button;
        }
        if (node instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                Button found = findButton(child, translationKey);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static String readConfigFile() {
        try {
            return Files.readString(LoupeClient.configPath());
        } catch (IOException e) {
            throw new AssertionError("could not read " + LoupeClient.configPath(), e);
        }
    }

    private static void checkNear(float actual, float expected, float tolerance, String message) {
        if (Math.abs(actual - expected) > tolerance) {
            throw new AssertionError(message + ": expected " + expected + " but was " + actual);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void log(String message) {
        System.out.println("[Loupe gametest] " + message);
    }
}
