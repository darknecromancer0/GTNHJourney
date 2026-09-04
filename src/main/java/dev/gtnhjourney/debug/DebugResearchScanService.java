package dev.gtnhjourney.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import dev.gtnhjourney.minecraft.ItemStackKeyFactory;

/** Read-only migration scans plus one-action transactional application hooks for the Debug Researcher Tool. */
public final class DebugResearchScanService {

    private static final int AREA_RADIUS = 16;

    private final WorldAdapter world;
    private final ResearchCandidateDeduplicator.IdentityResolver identityResolver;

    public DebugResearchScanService(
        WorldAdapter world,
        ResearchCandidateDeduplicator.IdentityResolver identityResolver) {
        this.world = world;
        this.identityResolver = identityResolver;
    }

    public static DebugResearchScanService forPlayer(EntityPlayer player) {
        return forPlayer(player, 1);
    }

    public static DebugResearchScanService forPlayer(final EntityPlayer player, final int side) {
        final World actualWorld = player == null ? null : player.worldObj;
        return new DebugResearchScanService(new WorldAdapter() {

            @Override
            public boolean canRead(int x, int y, int z) {
                return LoadedWorldAccess.isLoaded(actualWorld, x, y, z);
            }

            @Override
            public ItemStack blockCandidate(int x, int y, int z) {
                if (!LoadedWorldAccess.isLoaded(actualWorld, x, y, z)) return null;
                return PlacedBlockResearchResolver.resolve(actualWorld, player, x, y, z, side);
            }

            @Override
            public List<ItemStack> inventoryContents(int x, int y, int z) {
                TileEntity tile = LoadedWorldAccess.getTileEntityIfLoaded(actualWorld, x, y, z);
                if (!(tile instanceof IInventory)) return null;
                return InventoryResearchCollector.collect((IInventory) tile);
            }

            @Override
            @SuppressWarnings("unchecked")
            public List<ItemStack> droppedItemsInArea(
                int minX,
                int minY,
                int minZ,
                int maxX,
                int maxY,
                int maxZ) {
                if (actualWorld == null) return Collections.emptyList();
                AxisAlignedBB bounds = AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
                List<EntityItem> entities = actualWorld.getEntitiesWithinAABB(EntityItem.class, bounds);
                if (entities == null || entities.isEmpty()) return Collections.emptyList();
                List<ItemStack> stacks = new ArrayList<ItemStack>(entities.size());
                for (EntityItem entity : entities) {
                    if (entity == null || entity.isDead) continue;
                    ItemStack stack = entity.getEntityItem();
                    if (isValid(stack)) stacks.add(stack.copy());
                }
                return stacks;
            }
        }, new ResearchCandidateDeduplicator.IdentityResolver() {

            @Override
            public dev.gtnhjourney.research.ResearchKey identity(ItemStack stack) {
                return ItemStackKeyFactory.from(stack);
            }
        });
    }

    /** Applies one physical tool action as one bulk mutation, then emits exactly one sync and one summary callback. */
    public DebugResearchScanResult apply(
        DebugResearchMode mode,
        int x,
        int y,
        int z,
        MutationAdapter mutation,
        ActionEffects effects) {
        DebugResearchMode selected = mode == null ? DebugResearchMode.BLOCK : mode;
        DebugResearchScanResult scanned = scan(selected, x, y, z);
        int added = 0;

        if (mutation != null) {
            if (selected == DebugResearchMode.AREA_16) mutation.createSafetySnapshot("before-migration-area16");
            added = mutation.applyBulkAdd(scanned.getCandidates(), "Migration " + selected.name());
        }

        DebugResearchScanResult completed = scanned.withNewlyUnlockedStates(added);
        if (effects != null) {
            effects.sync();
            effects.summary(selected, completed);
        }
        return completed;
    }

    public DebugResearchScanResult scanBlock(int x, int y, int z) {
        if (!canRead(x, y, z)) return emptyResult(0);

        ItemStack candidate = safeBlockCandidate(x, y, z);
        if (!isValid(candidate)) return emptyResult(0);

        List<ItemStack> raw = Collections.singletonList(candidate.copy());
        return result(raw, 0, 1, 0, 0, 1);
    }

    public DebugResearchScanResult scanContents(int x, int y, int z) {
        if (!canRead(x, y, z)) return emptyResult(0);

        List<ItemStack> contents = safeInventoryContents(x, y, z);
        if (contents == null) return emptyResult(0);

        List<ItemStack> raw = validCopies(contents);
        return result(raw, 0, 0, 1, 0, raw.size());
    }

    public DebugResearchScanResult scanArea16(int centerX, int centerY, int centerZ) {
        List<Area16Planner.Position> positions = Area16Planner.plan(centerX, centerY, centerZ);
        List<ItemStack> raw = new ArrayList<ItemStack>();
        int blockCandidates = 0;
        int inventoriesVisited = 0;

        for (Area16Planner.Position position : positions) {
            int x = position.getX();
            int y = position.getY();
            int z = position.getZ();
            if (!canRead(x, y, z)) continue;

            ItemStack block = safeBlockCandidate(x, y, z);
            if (isValid(block)) {
                raw.add(block.copy());
                blockCandidates++;
            }

            List<ItemStack> contents = safeInventoryContents(x, y, z);
            if (contents == null) continue;
            inventoriesVisited++;
            raw.addAll(validCopies(contents));
        }

        List<ItemStack> dropped = safeDroppedItemsInArea(
            centerX - AREA_RADIUS,
            centerY - AREA_RADIUS,
            centerZ - AREA_RADIUS,
            centerX + AREA_RADIUS,
            centerY + AREA_RADIUS,
            centerZ + AREA_RADIUS);
        List<ItemStack> droppedCopies = validCopies(dropped);
        raw.addAll(droppedCopies);

        return result(raw, positions.size(), blockCandidates, inventoriesVisited, droppedCopies.size(), raw.size());
    }

    private DebugResearchScanResult scan(DebugResearchMode mode, int x, int y, int z) {
        switch (mode) {
            case CONTENTS:
                return scanContents(x, y, z);
            case AREA_16:
                return scanArea16(x, y, z);
            case BLOCK:
            default:
                return scanBlock(x, y, z);
        }
    }

    private DebugResearchScanResult result(
        List<ItemStack> raw,
        int positionsVisited,
        int blockCandidates,
        int inventoriesVisited,
        int droppedItemsVisited,
        int rawStacks) {
        List<ItemStack> unique = ResearchCandidateDeduplicator.deduplicate(raw, identityResolver);
        return new DebugResearchScanResult(
            unique,
            positionsVisited,
            blockCandidates,
            inventoriesVisited,
            droppedItemsVisited,
            rawStacks,
            0);
    }

    private DebugResearchScanResult emptyResult(int positionsVisited) {
        return new DebugResearchScanResult(
            Collections.<ItemStack>emptyList(),
            positionsVisited,
            0,
            0,
            0,
            0,
            0);
    }

    private boolean canRead(int x, int y, int z) {
        if (world == null) return false;
        try {
            return world.canRead(x, y, z);
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private ItemStack safeBlockCandidate(int x, int y, int z) {
        try {
            return world.blockCandidate(x, y, z);
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private List<ItemStack> safeInventoryContents(int x, int y, int z) {
        try {
            return world.inventoryContents(x, y, z);
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private List<ItemStack> safeDroppedItemsInArea(
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ) {
        if (world == null) return Collections.emptyList();
        try {
            List<ItemStack> stacks = world.droppedItemsInArea(minX, minY, minZ, maxX, maxY, maxZ);
            return stacks == null ? Collections.<ItemStack>emptyList() : stacks;
        } catch (RuntimeException | LinkageError ignored) {
            return Collections.emptyList();
        }
    }

    private static List<ItemStack> validCopies(List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) return Collections.emptyList();
        List<ItemStack> copies = new ArrayList<ItemStack>(stacks.size());
        for (ItemStack stack : stacks) {
            if (isValid(stack)) copies.add(stack.copy());
        }
        return copies;
    }

    private static boolean isValid(ItemStack stack) {
        return stack != null && stack.getItem() != null && stack.stackSize > 0;
    }

    public interface WorldAdapter {

        boolean canRead(int x, int y, int z);

        ItemStack blockCandidate(int x, int y, int z);

        /** Returns null when the position has no readable IInventory, otherwise a defensive stack list. */
        List<ItemStack> inventoryContents(int x, int y, int z);

        /** Returns defensive copies of all dropped item stacks inside the inclusive AREA_16 bounds. */
        default List<ItemStack> droppedItemsInArea(
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ) {
            return Collections.emptyList();
        }
    }

    public interface MutationAdapter {

        void createSafetySnapshot(String name);

        int applyBulkAdd(List<ItemStack> candidates, String description);
    }

    public interface ActionEffects {

        void sync();

        void summary(DebugResearchMode mode, DebugResearchScanResult result);
    }
}
