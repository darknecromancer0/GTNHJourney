package dev.gtnhjourney.debug;

import java.lang.reflect.Field;
import java.util.Map;

import net.minecraft.block.Block;
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

    private static volatile Field entityNameToIdField;
    private static volatile boolean entityNameToIdUnavailable;

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
        try {
            if (world.isAirBlock(x, y, z)) return null;
            block = world.getBlock(x, y, z);
        } catch (RuntimeException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
        if (block == null) return null;

        if (block == Blocks.mob_spawner) return resolveMobSpawner(world, x, y, z);

        return resolve(new StackStrategy() {
            @Override public ItemStack resolve() {
                MovingObjectPosition target = new MovingObjectPosition(
                    x, y, z, side, Vec3.createVectorHelper(x + 0.5D, y + 0.5D, z + 0.5D));
                return block.getPickBlock(target, world, x, y, z, player);
            }
        }, new StackStrategy() {
            @Override public ItemStack resolve() {
                Item item = Item.getItemFromBlock(block);
                if (item == null) return null;
                return new ItemStack(item, 1, world.getBlockMetadata(x, y, z));
            }
        });
    }

    private static ItemStack resolveMobSpawner(World world, int x, int y, int z) {
        try {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!(tile instanceof TileEntityMobSpawner)) return null;
            String entityName = ((TileEntityMobSpawner) tile).func_145881_a().getEntityNameToSpawn();
            int entityMeta = resolveRegisteredEntityMeta(entityName);
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

    static int resolveRegisteredEntityMeta(String entityName) {
        if (entityName == null || entityName.length() == 0) return -1;
        Map<String, Integer> ids = entityNameToIdMapping();
        return MobSpawnerResearchIdentity.resolveEntityMeta(entityName, ids);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> entityNameToIdMapping() {
        if (entityNameToIdUnavailable) return java.util.Collections.emptyMap();
        try {
            Field field = entityNameToIdField;
            if (field == null) {
                field = findField(EntityList.class, "stringToIDMapping", "field_75624_e");
                field.setAccessible(true);
                entityNameToIdField = field;
            }
            Object value = field.get(null);
            return value instanceof Map ? (Map<String, Integer>) value : java.util.Collections.<String, Integer>emptyMap();
        } catch (Throwable ignored) {
            entityNameToIdUnavailable = true;
            return java.util.Collections.emptyMap();
        }
    }

    private static Field findField(Class<?> type, String... names) throws NoSuchFieldException {
        for (String name : names) {
            try { return type.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(type.getName());
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
