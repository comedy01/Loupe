package dev.loupe.selftest.forge;

import dev.loupe.selftest.SelfTest;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("loupe_selftest")
public final class LoupeSelfTestForge {
    private final SelfTest test = new SelfTest();

    public LoupeSelfTestForge() {
        MinecraftForge.EVENT_BUS.addListener(this::onTick);
    }

    private void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            test.tick();
        }
    }
}
