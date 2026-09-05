package dev.gtnhjourney.minecraft;

import java.util.function.IntFunction;

import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.world.World;

/** Bridges NEI's legacy spawner meta representation to the TileEntity entity id required by 1.7.10 placement. */
public final class MobSpawnerStatePolicy {

    public static final String ENTITY_TAG = "GTNHJourneySpawnerEntity";
    private static final String SPAWNER_ID = "minecraft:mob_spawner";

    private MobSpawnerStatePolicy() {}

    public static String resolveEntityName(int meta, String taggedEntity, IntFunction<String> legacyResolver) {
        if (taggedEntity != null && !taggedEntity.trim().isEmpty()) return taggedEntity;
        if (meta <= 0 || legacyResolver == null) return null;
        String resolved = legacyResolver.apply(meta);
        return resolved == null || resolved.trim().isEmpty() ? null : resolved;
    }

    /** The placement marker is transport state, not a second research identity beside the existing entity meta. */
    public static void normalizeIdentity(String canonicalItemId, NBTTagCompound identityTag) {
        if (identityTag == null || !SPAWNER_ID.equals(canonicalItemId)) return;
        identityTag.removeTag(ENTITY_TAG);
    }

    /** Adds a placement marker to both newly researched and legacy meta-only spawners. */
    public static void ensurePlacementMarker(ItemStack stack) {
        if (!isVanillaSpawner(stack)) return;
        NBTTagCompound tag = stack.getTagCompound();
        String tagged = tag == null ? null : tag.getString(ENTITY_TAG);
        String entityName = resolveEntityName(stack.getItemDamage(), tagged, EntityList::getStringFromID);
        if (!isRegisteredEntity(entityName)) return;
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setString(ENTITY_TAG, entityName);
    }

    public static String placementEntity(ItemStack stack) {
        if (!isVanillaSpawner(stack)) return null;
        NBTTagCompound tag = stack.getTagCompound();
        String tagged = tag == null ? null : tag.getString(ENTITY_TAG);
        String entityName = resolveEntityName(stack.getItemDamage(), tagged, EntityList::getStringFromID);
        return isRegisteredEntity(entityName) ? entityName : null;
    }

    public static boolean applyPlacedSpawner(World world, int x, int y, int z, ItemStack sourceStack) {
        if (world == null || world.isRemote || world.getBlock(x, y, z) != Blocks.mob_spawner) return false;
        String entityName = placementEntity(sourceStack);
        if (entityName == null) return false;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityMobSpawner)) return false;
        TileEntityMobSpawner spawner = (TileEntityMobSpawner) tile;
        spawner.func_145881_a().setEntityName(entityName);
        spawner.markDirty();
        world.markBlockForUpdate(x, y, z);
        return true;
    }

    private static boolean isVanillaSpawner(ItemStack stack) {
        return stack != null && stack.getItem() != null && stack.getItem() == Item.getItemFromBlock(Blocks.mob_spawner);
    }

    private static boolean isRegisteredEntity(String entityName) {
        return entityName != null && EntityList.stringToClassMapping.containsKey(entityName);
    }
}
