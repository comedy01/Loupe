package dev.loupe.gametest;

import net.minecraft.client.Minecraft;

final class HotbarAccess {
    private HotbarAccess() {
    }

    static int selected(Minecraft client) {
        return client.player.getInventory().getSelectedSlot();
    }
}
