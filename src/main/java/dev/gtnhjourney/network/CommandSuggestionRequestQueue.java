package dev.gtnhjourney.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.command.ICommandManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Resolves chat completions through the actual server command manager on the server thread. */
public final class CommandSuggestionRequestQueue {

    private static final int MAX_REQUESTS_PER_TICK = 64;
    private static final int MAX_SUGGESTIONS = 64;
    private static final Queue<Request> REQUESTS = new ConcurrentLinkedQueue<Request>();

    static void enqueue(EntityPlayerMP player, long requestId, String prefix) {
        if (player == null || prefix == null) return;
        REQUESTS.add(new Request(player, requestId, prefix));
    }

    public static void clear() {
        REQUESTS.clear();
    }

    @SubscribeEvent
    @SuppressWarnings("rawtypes")
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Request request;
        int processed = 0;
        while (processed++ < MAX_REQUESTS_PER_TICK && (request = REQUESTS.poll()) != null) {
            EntityPlayerMP player = request.player;
            if (player == null || player.isDead || player.playerNetServerHandler == null) continue;
            String commandPrefix = request.prefix.startsWith("/") ? request.prefix.substring(1) : request.prefix;
            List<String> suggestions = Collections.emptyList();
            try {
                MinecraftServer server = MinecraftServer.getServer();
                ICommandManager manager = server == null ? null : server.getCommandManager();
                if (manager != null) {
                    List raw = manager.getPossibleCommands(player, commandPrefix);
                    if (raw != null && !raw.isEmpty()) {
                        ArrayList<String> values = new ArrayList<String>(Math.min(raw.size(), MAX_SUGGESTIONS));
                        for (Object value : raw) {
                            if (value == null) continue;
                            String text = String.valueOf(value);
                            if (text.isEmpty()) continue;
                            values.add(text);
                            if (values.size() >= MAX_SUGGESTIONS) break;
                        }
                        suggestions = values;
                    }
                }
            } catch (RuntimeException ignored) {
                suggestions = Collections.emptyList();
            } catch (LinkageError ignored) {
                suggestions = Collections.emptyList();
            }
            Journey1124Network.sendCommandSuggestions(player, request.requestId, request.prefix, suggestions);
        }
    }

    private static final class Request {
        final EntityPlayerMP player;
        final long requestId;
        final String prefix;
        Request(EntityPlayerMP player, long requestId, String prefix) {
            this.player = player;
            this.requestId = requestId;
            this.prefix = prefix;
        }
    }
}
