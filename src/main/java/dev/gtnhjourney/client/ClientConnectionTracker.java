package dev.gtnhjourney.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import dev.gtnhjourney.config.JourneyConfig;
import dev.gtnhjourney.minecraft.ResearchCompatibilityOptions;
import dev.gtnhjourney.nei.JourneyNEIRefreshTracker;
import dev.gtnhjourney.nei.JourneyViewState;

/** Prevents research and remote-server identity rules from leaking into the next client world/server. */
public final class ClientConnectionTracker {
    @SubscribeEvent
    public void onConnect(ClientConnectedToServerEvent event) {
        ClientNetworkQueue.beginSession(new Runnable() {
            @Override public void run() { resetClientSessionState(); }
        });
    }

    @SubscribeEvent
    public void onDisconnect(ClientDisconnectionFromServerEvent event) {
        ClientNetworkQueue.endSession(new Runnable() {
            @Override public void run() { resetClientSessionState(); }
        });
    }

    private static void resetClientSessionState() {
        ClientStackMirror.clear();
        JourneyViewState.setMode(JourneyViewState.Mode.ALL);
        JourneyNEIRefreshTracker.clearInjectedVariants();
        // Remote server identity rules are client-only session state. Restore local config so a later integrated
        // server in the same JVM cannot inherit another server's semantic policy.
        ResearchCompatibilityOptions.configure(
            JourneyConfig.normalizeGtTransientIdentity(),
            JourneyConfig.resetGtToolTemplateState(),
            JourneyConfig.normalizeGtChargeEndpoints(),
            JourneyConfig.normalizeIc2ChargeEndpoints(),
            JourneyConfig.normalizeTconToolWear(),
            JourneyConfig.normalizeCofhChargeEndpoints());
    }
}
