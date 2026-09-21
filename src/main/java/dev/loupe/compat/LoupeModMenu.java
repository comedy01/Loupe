package dev.loupe.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.loupe.client.gui.LoupeSettingsScreen;
import net.minecraft.client.Minecraft;

// only loaded when mod menu is installed
public final class LoupeModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new LoupeSettingsScreen(parent, Minecraft.getInstance().options);
    }
}
