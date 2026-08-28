package dev.gtnhjourney.command;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

import dev.gtnhjourney.GTNHJourney;
import dev.gtnhjourney.backup.WorldBackupResult;
import dev.gtnhjourney.config.JourneyConfig;

/** Command glue for backup, explosion and cleanse safety features. */
final class JourneySafetyCommandHandler {

    private JourneySafetyCommandHandler() {}

    static boolean handle(EntityPlayerMP player, String[] args) {
        if (player == null || args == null || args.length == 0) return false;
        String family = args[0].toLowerCase(java.util.Locale.ROOT);
        if ("backup".equals(family)) {
            handleBackup(player, args);
            return true;
        }
        if ("explosions".equals(family)) {
            handleExplosions(player, args);
            return true;
        }
        if ("cleanse".equals(family)) {
            int removed = GTNHJourney.CLEANSE.cleanse(player);
            tell(player, "Removed " + removed + " negative effect(s).");
            return true;
        }
        return false;
    }

    private static void handleBackup(EntityPlayerMP player, String[] args) {
        String action = args.length >= 2 ? args[1].toLowerCase(java.util.Locale.ROOT) : "status";
        if (!JourneySafetyCommandPolicy.isBackupAction(action)) {
            tell(player, "Usage: /journey backup status|now|on|off");
            return;
        }
        if (JourneySafetyCommandPolicy.requiresAdmin("backup", action) && !JourneyAdminPermissionPolicy.mayMutate(player)) {
            tell(player, "World safety changes require the integrated-server owner or operator permission.");
            return;
        }
        if ("status".equals(action)) {
            tell(
                player,
                "Automatic world backups " + (JourneyConfig.worldBackupsEnabled() ? "enabled" : "disabled") + " ("
                    + JourneyConfig.worldBackupIntervalSeconds() + "s, keep " + JourneyConfig.worldBackupRetention() + ").");
            return;
        }
        if ("on".equals(action)) {
            JourneyConfig.setWorldBackupsEnabled(true);
            tell(player, "Automatic world backups enabled.");
            return;
        }
        if ("off".equals(action)) {
            JourneyConfig.setWorldBackupsEnabled(false);
            tell(player, "Automatic world backups disabled. Manual /journey backup now remains available.");
            return;
        }

        WorldBackupResult result = GTNHJourney.WORLD_BACKUPS.tryBackup(MinecraftServer.getServer(), true);
        tell(player, result.getMessage());
    }

    private static void handleExplosions(EntityPlayerMP player, String[] args) {
        String action = args.length >= 2 ? args[1].toLowerCase(java.util.Locale.ROOT) : "status";
        if (!JourneySafetyCommandPolicy.isExplosionAction(action)) {
            tell(player, "Usage: /journey explosions status|on|off");
            return;
        }
        if (JourneySafetyCommandPolicy.requiresAdmin("explosions", action)
            && !JourneyAdminPermissionPolicy.mayMutate(player)) {
            tell(player, "World safety changes require the integrated-server owner or operator permission.");
            return;
        }
        if ("status".equals(action)) {
            tell(player, "Explosions are " + (JourneyConfig.explosionsEnabled() ? "enabled" : "disabled globally") + ".");
            return;
        }
        boolean enabled = "on".equals(action);
        JourneyConfig.setExplosionsEnabled(enabled);
        tell(player, enabled ? "Explosions enabled." : "Explosions disabled globally.");
    }

    private static void tell(EntityPlayerMP player, String text) {
        player.addChatMessage(new ChatComponentText("[Journey] " + text));
    }
}
