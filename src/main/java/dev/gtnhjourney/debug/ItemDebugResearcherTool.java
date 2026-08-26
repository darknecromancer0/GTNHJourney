package dev.gtnhjourney.debug;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import dev.gtnhjourney.GTNHJourney;
import dev.gtnhjourney.diagnostics.JourneyRuntimeCounters;
import dev.gtnhjourney.network.JourneyNetwork;

/** Admin migration tool that researches existing world blocks and inventory contents without mutating them. */
public final class ItemDebugResearcherTool extends Item {

    public ItemDebugResearcherTool() {
        setMaxStackSize(1);
        setUnlocalizedName("debugResearcherTool");
        setTextureName("minecraft:stick");
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return "Debug Researcher Tool";
    }

    @Override
    public boolean hasEffect(ItemStack stack, int pass) {
        return true;
    }

    @Override
    public boolean onItemUseFirst(
        ItemStack stack,
        EntityPlayer player,
        World world,
        int x,
        int y,
        int z,
        int side,
        float hitX,
        float hitY,
        float hitZ) {
        DebugResearchMode mode = DebugResearchToolState.read(stack);
        InteractionDecision decision = route(mode, player != null && player.isSneaking(), true);
        if (world == null) return false;
        if (world.isRemote) return consumeClientBlockUse();
        if (!(player instanceof EntityPlayerMP)) return false;

        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        if (decision.action() == InteractionAction.CYCLE_MODE) {
            cycleMode(serverPlayer, stack, mode);
            return true;
        }

        if (decision.action() == InteractionAction.EXECUTE) {
            if (mode == DebugResearchMode.AREA_16) {
                execute(serverPlayer, stack, mode, playerX(serverPlayer), playerY(serverPlayer), playerZ(serverPlayer), side);
            } else {
                execute(serverPlayer, stack, mode, x, y, z, side);
            }
        }
        return true;
    }

    /** Client must not consume the targeted use before Forge sends the corresponding server interaction. */
    public static boolean consumeClientBlockUse() {
        return false;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world == null || world.isRemote || !(player instanceof EntityPlayerMP)) return stack;

        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        DebugResearchMode mode = DebugResearchToolState.read(stack);
        InteractionDecision decision = route(mode, player.isSneaking(), false);
        if (decision.action() == InteractionAction.CYCLE_MODE) {
            cycleMode(serverPlayer, stack, mode);
            return stack;
        }
        if (decision.action() == InteractionAction.NO_TARGET) {
            tell(serverPlayer, "Debug Researcher Tool: no target.");
            return stack;
        }

        execute(serverPlayer, stack, mode, playerX(serverPlayer), playerY(serverPlayer), playerZ(serverPlayer), 1);
        return stack;
    }

    public static InteractionDecision route(DebugResearchMode mode, boolean sneaking, boolean hasTarget) {
        DebugResearchMode selected = mode == null ? DebugResearchMode.BLOCK : mode;
        if (sneaking) return new InteractionDecision(InteractionAction.CYCLE_MODE, true);
        if (hasTarget || selected == DebugResearchMode.AREA_16) {
            return new InteractionDecision(InteractionAction.EXECUTE, true);
        }
        return new InteractionDecision(InteractionAction.NO_TARGET, true);
    }

    private static void cycleMode(EntityPlayerMP player, ItemStack stack, DebugResearchMode current) {
        DebugResearchMode next = (current == null ? DebugResearchMode.BLOCK : current).next();
        DebugResearchToolState.write(stack, next);
        tell(player, "Debug Researcher Tool: " + next.name());
    }

    private static void execute(
        final EntityPlayerMP player,
        ItemStack tool,
        final DebugResearchMode mode,
        int x,
        int y,
        int z,
        int side) {
        if (player == null || tool == null || GTNHJourney.MUTATIONS == null) return;
        DebugResearchScanService scanner = DebugResearchScanService.forPlayer(player, side);
        scanner.apply(
            mode,
            x,
            y,
            z,
            new DebugResearchScanService.MutationAdapter() {

                @Override
                public void createSafetySnapshot(String name) {
                    GTNHJourney.MUTATIONS.createSafetySnapshot(player, name);
                }

                @Override
                public int applyBulkAdd(List<ItemStack> candidates, String description) {
                    return GTNHJourney.MUTATIONS.applyBulkAdd(player, candidates, description);
                }
            },
            new DebugResearchScanService.ActionEffects() {

                @Override
                public void sync() {
                    JourneyNetwork.sendFullSync(
                        player,
                        GTNHJourney.RESEARCH.snapshotStacksInUnlockOrder(player),
                        GTNHJourney.RESEARCH.snapshotActivityOrder(player));
                }

                @Override
                public void summary(DebugResearchMode selected, DebugResearchScanResult result) {
                    JourneyRuntimeCounters.debugResearchScan(
                        result.getPositionsVisited(),
                        result.getInventoriesVisited(),
                        result.getUniqueCandidates(),
                        result.getNewlyUnlockedStates());
                    tell(player, formatSummary(selected, result));
                }
            });
    }

    static String formatSummary(DebugResearchMode mode, DebugResearchScanResult result) {
        DebugResearchMode selected = mode == null ? DebugResearchMode.BLOCK : mode;
        DebugResearchScanResult safe = result == null
            ? new DebugResearchScanResult(java.util.Collections.<ItemStack>emptyList(), 0, 0, 0, 0, 0)
            : result;
        int added = safe.getNewlyUnlockedStates();
        String states = added == 1 ? " state" : " states";
        switch (selected) {
            case CONTENTS:
                return "Migration CONTENTS: " + safe.getRawStacks() + " stacks scanned, +" + added + states;
            case AREA_16:
                return "Migration AREA_16: " + safe.getPositionsVisited() + " positions, " + safe.getBlockCandidates()
                    + " block candidates, " + safe.getInventoriesVisited() + " inventories, +" + added + states;
            case BLOCK:
            default:
                return "Migration BLOCK: +" + added + states;
        }
    }

    private static int playerX(EntityPlayer player) {
        return MathHelper.floor_double(player.posX);
    }

    private static int playerY(EntityPlayer player) {
        return MathHelper.floor_double(player.posY);
    }

    private static int playerZ(EntityPlayer player) {
        return MathHelper.floor_double(player.posZ);
    }

    private static void tell(EntityPlayerMP player, String text) {
        player.addChatMessage(new ChatComponentText("[Journey] " + text));
    }

    public enum InteractionAction {
        CYCLE_MODE,
        EXECUTE,
        NO_TARGET
    }

    public static final class InteractionDecision {

        private final InteractionAction action;
        private final boolean consumed;

        InteractionDecision(InteractionAction action, boolean consumed) {
            this.action = action;
            this.consumed = consumed;
        }

        public InteractionAction action() {
            return action;
        }

        public boolean consumed() {
            return consumed;
        }
    }
}
