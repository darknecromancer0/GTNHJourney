package dev.gtnhjourney.debug;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** Read gate that refuses out-of-height or unloaded positions before touching world data. */
public final class LoadedWorldAccess {

    private LoadedWorldAccess() {}

    public static boolean canRead(int y, boolean chunkLoaded) {
        return y >= 0 && y < 256 && chunkLoaded;
    }

    public static boolean isLoaded(World world, int x, int y, int z) {
        if (world == null || y < 0 || y >= 256) return false;
        try {
            return canRead(y, world.getChunkProvider().chunkExists(x >> 4, z >> 4));
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    public static Block getBlockIfLoaded(World world, int x, int y, int z) {
        if (!isLoaded(world, x, y, z)) return null;
        try {
            Block block = world.getBlock(x, y, z);
            return block == null || block == Blocks.air ? null : block;
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    public static TileEntity getTileEntityIfLoaded(World world, int x, int y, int z) {
        if (!isLoaded(world, x, y, z)) return null;
        try {
            return world.getTileEntity(x, y, z);
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }
}
