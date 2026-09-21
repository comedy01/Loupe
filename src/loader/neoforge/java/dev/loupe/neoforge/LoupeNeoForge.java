package dev.loupe.neoforge;

import dev.loupe.client.LoupeClient;
import dev.loupe.client.gui.LoupeSettingsScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = LoupeClient.MOD_ID, dist = Dist.CLIENT)
public final class LoupeNeoForge {
    public LoupeNeoForge(IEventBus modBus, ModContainer container) {
        LoupeClient.init(FMLPaths.CONFIGDIR.get());

        KeyMapping key = LoupeClient.createKey();
        LoupeClient.setZoomKey(key);
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> event.register(key));

        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> new LoupeSettingsScreen(parent, Minecraft.getInstance().options));
    }
}
