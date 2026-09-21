package dev.loupe.nftest;

import dev.loupe.client.LoupeClient;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.renderer.GameRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Mod(value = "loupe_test", dist = Dist.CLIENT)
public final class LoupeSelfTest {
    private int ticks;
    private boolean done;

    public LoupeSelfTest() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, this::onTick);
    }

    private void onTick(ClientTickEvent.Post event) {
        if (done || ++ticks < 120) {
            return;
        }
        done = true;
        Minecraft client = Minecraft.getInstance();
        KeyMapping key = LoupeClient.zoomKey();

        boolean registered = key != null && Arrays.asList(client.options.keyMappings).contains(key);

        Set<String> merged = new HashSet<>();
        for (Class<?> target : new Class<?>[] {Camera.class, GameRenderer.class, MouseHandler.class}) {
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
        boolean pass = registered && merged.size() >= 3 && pressed && config;
        System.out.println("[Loupe selftest] key=" + (key == null ? "none" : key.saveString())
                + " registered=" + registered + " merged=" + merged + " pressed=" + pressed
                + " config=" + config + " RESULT=" + (pass ? "PASS" : "FAIL"));
        client.stop();
    }
}
