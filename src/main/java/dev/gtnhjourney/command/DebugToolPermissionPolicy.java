package dev.gtnhjourney.command;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

/** Narrow permission policy for the admin-only migration tool without raising harmless /journey commands. */
public final class DebugToolPermissionPolicy {

    private DebugToolPermissionPolicy() {}

    public static boolean mayUse(boolean integratedSingleplayer, boolean isIntegratedOwner, boolean hasOperatorPermission) {
        return hasOperatorPermission || (integratedSingleplayer && isIntegratedOwner);
    }

    public static boolean mayUse(EntityPlayerMP player) {
        if (player == null) return false;
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return false;

        boolean integrated = server.isSinglePlayer();
        String owner = server.getServerOwner();
        boolean ownerMatch = integrated && owner != null && owner.equalsIgnoreCase(player.getCommandSenderName());
        boolean operator = player.canCommandSenderUseCommand(2, "journey");
        return mayUse(integrated, ownerMatch, operator);
    }
}
