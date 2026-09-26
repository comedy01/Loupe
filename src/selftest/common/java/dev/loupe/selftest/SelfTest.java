package dev.loupe.selftest;

import dev.loupe.client.LoupeClient;
import dev.loupe.client.gui.LoupeSettingsScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// Dev-only launch check: mixins merged, key registered and pressable, config written,
// settings screen opens and its Key Binds button opens the vanilla key screen.
public final class SelfTest {
    private int ticks;
    private boolean done;
    private String checks = "";
    private boolean pass;

    public void tick() {
        if (done) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        ticks++;
        if (ticks == 120) {
            checks = startupChecks(client);
            client.setScreen(new LoupeSettingsScreen(null, client.options));
        } else if (ticks == 160) {
            boolean settings = client.screen instanceof LoupeSettingsScreen;
            Button keyButton = settings ? findButton(client, "loupe.options.key") : null;
            checks += " settingsScreen=" + settings + " keyButton=" + (keyButton != null);
            pass &= settings && keyButton != null;
            if (keyButton != null) {
                keyButton.onPress();
            }
        } else if (ticks == 200) {
            String shown = client.screen == null ? "none" : client.screen.getClass().getSimpleName();
            boolean keyScreen = shown.equals("KeyBindsScreen");
            checks += " keyBindsScreen=" + keyScreen + "(" + shown + ")";
            pass &= keyScreen;
            done = true;
            System.out.println("[Loupe selftest]" + checks + " RESULT=" + (pass ? "PASS" : "FAIL"));
            client.stop();
        }
    }

    private String startupChecks(Minecraft client) {
        KeyMapping key = LoupeClient.zoomKey();
        boolean registered = key != null && Arrays.asList(client.options.keyMappings).contains(key);

        Set<String> merged = new HashSet<>();
        for (Class<?> target : new Class<?>[] {GameRenderer.class, MouseHandler.class}) {
            for (Method method : target.getDeclaredMethods()) {
                if (method.getName().contains("loupe$")) {
                    merged.add(method.getName());
                }
            }
        }

        boolean pressed = false;
        if (key != null) {
            KeyMapping.set(key.getKey(), true);
            pressed = key.isDown();
            KeyMapping.set(key.getKey(), false);
        }

        boolean config = LoupeClient.configPath() != null && Files.exists(LoupeClient.configPath());
        pass = registered && merged.size() >= 3 && pressed && config;
        return " key=" + (key == null ? "none" : key.saveString()) + " registered=" + registered
                + " merged=" + merged + " pressed=" + pressed + " config=" + config;
    }

    private static Button findButton(Minecraft client, String translationKey) {
        for (GuiEventListener child : client.screen.children()) {
            if (child instanceof Button button
                    && button.getMessage().getContents() instanceof TranslatableContents contents
                    && contents.getKey().equals(translationKey)) {
                return button;
            }
        }
        return null;
    }
}
