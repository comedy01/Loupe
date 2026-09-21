package dev.loupe.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;

final class KeyFactory {
    private KeyFactory() {
    }

    static KeyMapping create(String name, int code) {
        KeyMapping.Category category =
                KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath(LoupeClient.MOD_ID, "main"));
        return new KeyMapping(name, code, category);
    }
}
