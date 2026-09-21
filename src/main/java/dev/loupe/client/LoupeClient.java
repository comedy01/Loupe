package dev.loupe.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.loupe.config.ZoomConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;

public final class LoupeClient implements ClientModInitializer {
    public static final String MOD_ID = "loupe";

    private static ZoomConfig config = new ZoomConfig();
    private static KeyMapping zoomKey;

    @Override
    public void onInitializeClient() {
        config = ZoomConfig.load(configPath());

        KeyMapping.Category category =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        int defaultKey = InputConstants.getKey("key.keyboard.c").getValue();
        zoomKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.loupe.zoom", defaultKey, category));
    }

    public static ZoomConfig config() {
        return config;
    }

    public static KeyMapping zoomKey() {
        return zoomKey;
    }

    public static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(ZoomConfig.FILE_NAME);
    }

    public static void saveConfig() {
        config.saveQuietly(configPath());
    }
}
