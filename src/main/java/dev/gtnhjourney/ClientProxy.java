package dev.gtnhjourney;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import dev.gtnhjourney.client.ClientConnectionTracker;
import dev.gtnhjourney.client.ClientNetworkQueue;

/** Client lifecycle that must exist before NEI config discovery or the first server research sync. */
public final class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(new ClientConnectionTracker());
        FMLCommonHandler.instance()
            .bus()
            .register(new ClientNetworkQueue());
    }
}
