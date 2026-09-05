package dev.gtnhjourney.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import dev.gtnhjourney.config.JourneyConfig;
import dev.gtnhjourney.diagnostics.JourneyRuntimeCounters;
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
        ClientFavouriteMirror.clear();
        JourneyRuntimeCounters.reset();
        JourneyViewState.setMode(JourneyViewState.Mode.ALL);
        JourneyNEIRefreshTracker.resetJourneyPanel();
        ResearchCompatibilityOptions.configure(
            JourneyConfig.normalizeGtTransientIdentity(),
            JourneyConfig.resetGtToolTemplateState(),
            JourneyConfig.normalizeGtChargeEndpoints(),
            JourneyConfig.normalizeIc2ChargeEndpoints(),
            JourneyConfig.normalizeTconToolWear(),
            JourneyConfig.normalizeCofhChargeEndpoints());
    }
}
