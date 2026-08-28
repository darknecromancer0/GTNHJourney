package dev.gtnhjourney.command;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

/** Permission policy for Journey settings that affect the whole running server/world. */
public final class JourneyAdminPermissionPolicy {

    private JourneyAdminPermissionPolicy() {}

    public static boolean mayMutate(boolean integratedSingleplayer, boolean isIntegratedOwner, boolean hasOperatorPermission) {
        return hasOperatorPermission || (integratedSingleplayer && isIntegratedOwner);
    }

    public static boolean mayMutate(EntityPlayerMP player) {
        if (player == null) return false;
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return false;

        boolean integrated = server.isSinglePlayer();
        String owner = server.getServerOwner();
        boolean ownerMatch = integrated && owner != null && owner.equalsIgnoreCase(player.getCommandSenderName());
        boolean operator = player.canCommandSenderUseCommand(2, "journey");
        return mayMutate(integrated, ownerMatch, operator);
    }
}
