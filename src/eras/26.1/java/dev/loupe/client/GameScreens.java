package dev.loupe.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class GameScreens {
    private GameScreens() {
    }

    public static Screen current(Minecraft client) {
        return client.screen;
    }

    public static void open(Minecraft client, Screen screen) {
        client.setScreen(screen);
    }
}
