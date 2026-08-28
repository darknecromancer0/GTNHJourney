package dev.gtnhjourney.safety;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

/** Best-effort human-readable source naming for cancelled explosions. */
public final class ExplosionSourceResolver {

    public String describe(World world, Explosion explosion) {
        if (explosion == null) return "Explosion";

        try {
            Entity exploder = explosion.exploder;
            if (exploder != null) {
                String name = exploder.getCommandSenderName();
                if (useful(name)) return name;
                name = exploder.getClass().getSimpleName();
                if (useful(name)) return name;
            }
        } catch (RuntimeException ignored) {}
        catch (LinkageError ignored) {}

        if (world != null) {
            int x = MathHelper.floor_double(explosion.explosionX);
            int y = MathHelper.floor_double(explosion.explosionY);
            int z = MathHelper.floor_double(explosion.explosionZ);

            try {
                Block block = world.getBlock(x, y, z);
                if (block != null && block != Blocks.air) {
                    String name = block.getLocalizedName();
                    if (useful(name)) return name;
                }
            } catch (RuntimeException ignored) {}
            catch (LinkageError ignored) {}

            try {
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile != null) {
                    String name = tile.getClass().getSimpleName();
                    if (useful(name)) return name;
                }
            } catch (RuntimeException ignored) {}
            catch (LinkageError ignored) {}
        }

        return "Explosion";
    }

    private static boolean useful(String value) {
        return value != null && value.trim().length() > 0;
    }
}
