package dev.loupe.selftest.fabric;

import dev.loupe.selftest.SelfTest;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class LoupeSelfTestFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SelfTest test = new SelfTest();
        ClientTickEvents.END_CLIENT_TICK.register(client -> test.tick());
    }
}
