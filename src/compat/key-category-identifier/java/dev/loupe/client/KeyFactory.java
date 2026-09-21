package dev.loupe.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

final class KeyFactory {
    private KeyFactory() {
    }

    static KeyMapping create(String name, int code) {
        KeyMapping.Category category =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(LoupeClient.MOD_ID, "main"));
        return new KeyMapping(name, code, category);
    }
}
