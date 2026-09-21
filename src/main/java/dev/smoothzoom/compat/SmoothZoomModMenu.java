package dev.smoothzoom.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.smoothzoom.client.gui.SmoothZoomSettingsScreen;
import net.minecraft.client.Minecraft;

// only loaded when mod menu is installed
public final class SmoothZoomModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new SmoothZoomSettingsScreen(parent, Minecraft.getInstance().options);
    }
}
