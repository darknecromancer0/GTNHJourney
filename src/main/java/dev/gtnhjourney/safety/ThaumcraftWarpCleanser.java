package dev.gtnhjourney.safety;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import cpw.mods.fml.common.Loader;

/**
 * Uses Thaumcraft 4's own registered server command, preserving its player knowledge persistence and warp-sync
 * hooks rather than modifying player NBT or private Thaumcraft fields. This must only be called for an authorized
 * player, on the authoritative server command thread.
 */
public final class ThaumcraftWarpCleanser {

    private ThaumcraftWarpCleanser() {}

    static String[] resetCommands(String playerName) {
        if (playerName == null || !playerName.matches("[A-Za-z0-9_]{1,16}")) return new String[0];
        return new String[] {
            "tc warp " + playerName + " set 0",
            "tc warp " + playerName + " set 0 TEMP",
            "tc warp " + playerName + " set 0 PERM"
        };
    }

    /** Returns how many of Thaumcraft's three Warp categories were successfully reset (0..3). */
    public static int clear(EntityPlayerMP player) {
        if (player == null || !Loader.isModLoaded("Thaumcraft")) return 0;
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getCommandManager() == null) return 0;

        String[] commands = resetCommands(player.getCommandSenderName());
        int succeeded = 0;
        for (String command : commands) {
            try {
                if (server.getCommandManager().executeCommand(server, command) > 0) succeeded++;
            } catch (RuntimeException | LinkageError incompatibleThaumcraft) {
                // Do not report unverified categories as cleared.
            }
        }
        return succeeded;
    }
}
