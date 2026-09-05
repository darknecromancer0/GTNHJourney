package dev.gtnhjourney.debug;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/** Resolves a placed block into an item representation without breaking or mutating the world. */
public final class PlacedBlockResearchResolver {

    private PlacedBlockResearchResolver() {}

    public static ItemStack resolve(StackStrategy pick, StackStrategy fallback) {
        ItemStack picked = safelyResolve(pick);
        return validCopy(picked != null ? picked : safelyResolve(fallback));
    }

    public static ItemStack resolve(
        final World world,
        final EntityPlayer player,
        final int x,
        final int y,
        final int z,
        final int side) {
        if (world == null || player == null) return null;
        final Block block;
        final int meta;
        try {
            if (world.isAirBlock(x, y, z)) return null;
            block = world.getBlock(x, y, z);
            meta = world.getBlockMetadata(x, y, z);
        } catch (RuntimeException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
        if (block == null) return null;

        if (block == Blocks.mob_spawner) return resolveMobSpawner(world, x, y, z);

        // Prefer the mod's own pick representation whenever it exists. Thaumcraft aura nodes deliberately return air
        // from their generic getItem path, so only after pick fails do we consult the narrow registry-known fallback.
        ItemStack picked = safelyResolve(new StackStrategy() {
            @Override public ItemStack resolve() {
                MovingObjectPosition target = new MovingObjectPosition(
                    x, y, z, side, Vec3.createVectorHelper(x + 0.5D, y + 0.5D, z + 0.5D));
                return block.getPickBlock(target, world, x, y, z, player);
            }
        });
        if (picked != null) return picked;

        ItemStack known = KnownPlacedBlockResearchFallback.resolve(block, meta);
        if (known != null) return validCopy(known);

        return safelyResolve(new StackStrategy() {
            @Override public ItemStack resolve() {
                Item item = Item.getItemFromBlock(block);
                if (item == null) return null;
                return new ItemStack(item, 1, meta);
            }
        });
    }

    private static ItemStack resolveMobSpawner(World world, int x, int y, int z) {
        try {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!(tile instanceof TileEntityMobSpawner)) return null;
            String entityName = ((TileEntityMobSpawner) tile).func_145881_a().getEntityNameToSpawn();
            Entity entity = EntityList.createEntityByName(entityName, world);
            if (entity == null) return null;
            int entityMeta = EntityList.getEntityID(entity);
            if (entityMeta <= 0) return null;

            Item item = Item.getItemFromBlock(Blocks.mob_spawner);
            if (item == null) return null;
            return new ItemStack(item, 1, entityMeta);
        } catch (RuntimeException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }

    private static ItemStack safelyResolve(StackStrategy strategy) {
        if (strategy == null) return null;
        try {
            return validCopy(strategy.resolve());
        } catch (RuntimeException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }

    private static ItemStack validCopy(ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) return null;
        ItemStack copy = stack.copy();
        copy.stackSize = 1;
        return copy;
    }

    public interface StackStrategy { ItemStack resolve(); }
}
