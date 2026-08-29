package dev.gtnhjourney;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import dev.gtnhjourney.client.ClientConnectionTracker;
import dev.gtnhjourney.client.ClientNetworkQueue;
import dev.gtnhjourney.client.CommandHintDiagnostics;
import dev.gtnhjourney.client.JourneyCommandHintOverlay;
import dev.gtnhjourney.nei.JourneyCreativeInventorySafety;

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
        FMLCommonHandler.instance()
            .bus()
            .register(new JourneyCreativeInventorySafety());
        MinecraftForge.EVENT_BUS.register(new JourneyCommandHintOverlay());
        CommandHintDiagnostics.markRegistered();
    }
}
