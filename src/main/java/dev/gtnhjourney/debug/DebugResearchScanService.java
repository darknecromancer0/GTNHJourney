package dev.gtnhjourney.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import dev.gtnhjourney.minecraft.ItemStackKeyFactory;

/** Read-only migration scan service for BLOCK, CONTENTS and bounded AREA_16 observations. */
public final class DebugResearchScanService {

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
        }, new ResearchCandidateDeduplicator.IdentityResolver() {

            @Override
            public dev.gtnhjourney.research.ResearchKey identity(ItemStack stack) {
                return ItemStackKeyFactory.from(stack);
            }
        });
    }

    public DebugResearchScanResult scanBlock(int x, int y, int z) {
        if (!canRead(x, y, z)) return emptyResult(0);

        ItemStack candidate = safeBlockCandidate(x, y, z);
        if (!isValid(candidate)) return emptyResult(0);

        List<ItemStack> raw = Collections.singletonList(candidate.copy());
        return result(raw, 0, 1, 0, 1);
    }

    public DebugResearchScanResult scanContents(int x, int y, int z) {
        if (!canRead(x, y, z)) return emptyResult(0);

        List<ItemStack> contents = safeInventoryContents(x, y, z);
        if (contents == null) return emptyResult(0);

        List<ItemStack> raw = validCopies(contents);
        return result(raw, 0, 0, 1, raw.size());
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

        return result(raw, positions.size(), blockCandidates, inventoriesVisited, raw.size());
    }

    private DebugResearchScanResult result(
        List<ItemStack> raw,
        int positionsVisited,
        int blockCandidates,
        int inventoriesVisited,
        int rawStacks) {
        List<ItemStack> unique = ResearchCandidateDeduplicator.deduplicate(raw, identityResolver);
        return new DebugResearchScanResult(
            unique,
            positionsVisited,
            blockCandidates,
            inventoriesVisited,
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
    }
}
