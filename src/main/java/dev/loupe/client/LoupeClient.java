package dev.loupe.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.loupe.config.ZoomConfig;
import net.minecraft.client.KeyMapping;

import java.nio.file.Path;

public final class LoupeClient {
    public static final String MOD_ID = "loupe";

    private static ZoomConfig config = new ZoomConfig();
    private static KeyMapping zoomKey;
    private static Path configPath;

    private LoupeClient() {
    }

    public static void init(Path configDir) {
        configPath = configDir.resolve(ZoomConfig.FILE_NAME);
        config = ZoomConfig.load(configPath);
    }

    public static KeyMapping createKey() {
        int defaultKey = InputConstants.getKey(DefaultKey.NAME).getValue();
        return KeyFactory.create("key.loupe.zoom", defaultKey);
    }

    public static void setZoomKey(KeyMapping key) {
        zoomKey = key;
    }

    public static ZoomConfig config() {
        return config;
    }

    public static KeyMapping zoomKey() {
        return zoomKey;
    }

    public static Path configPath() {
        return configPath;
    }

    public static void saveConfig() {
        config.saveQuietly(configPath);
    }
}
