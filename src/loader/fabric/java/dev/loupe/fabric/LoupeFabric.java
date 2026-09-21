package dev.loupe.fabric;

import dev.loupe.client.KeyRegistrar;
import dev.loupe.client.LoupeClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class LoupeFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LoupeClient.init(FabricLoader.getInstance().getConfigDir());
        LoupeClient.setZoomKey(KeyRegistrar.register(LoupeClient.createKey()));
    }
}
