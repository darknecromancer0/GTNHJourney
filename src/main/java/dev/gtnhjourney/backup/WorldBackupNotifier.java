package dev.gtnhjourney.backup;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

import dev.gtnhjourney.command.JourneyAdminPermissionPolicy;

/** Reports completed automatic/manual Journey backups to the integrated owner or online operators. */
public final class WorldBackupNotifier {

    public void notifyCompletion(MinecraftServer server, WorldBackupResult result, long durationMillis) {
        if (server == null || result == null || server.getConfigurationManager() == null) return;
        String text = "[Journey] " + completionMessage(result, durationMillis);
        for (Object candidate : server.getConfigurationManager().playerEntityList) {
            if (!(candidate instanceof EntityPlayerMP)) continue;
            EntityPlayerMP player = (EntityPlayerMP) candidate;
            if (JourneyAdminPermissionPolicy.mayMutate(player)) player.addChatMessage(new ChatComponentText(text));
        }
    }

    static String completionMessage(WorldBackupResult result, long durationMillis) {
        if (result == null) return "Backup failed: no result. (" + formatDuration(durationMillis) + ")";
        String message = result.getMessage();
        if (message == null) message = "";
        if (result.isSuccess()) {
            String completed = message.startsWith("Backup completed") ? message : "Backup completed: " + message;
            return completed + " (" + formatDuration(durationMillis) + ")";
        }
        String failure = message.startsWith("Backup failed") ? message : "Backup failed: " + message;
        return failure + " (" + formatDuration(durationMillis) + ")";
    }

    static String formatDuration(long durationMillis) {
        long seconds = Math.max(0L, durationMillis) / 1000L;
        long minutes = seconds / 60L;
        long remainder = seconds % 60L;
        return minutes > 0L ? minutes + "m " + remainder + "s" : remainder + "s";
    }
}
