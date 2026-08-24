package dev.gtnhjourney.command;

import java.util.List;

import dev.gtnhjourney.GTNHJourney;
import dev.gtnhjourney.acquisition.PlayerInventoryScanner;
import dev.gtnhjourney.network.JourneyNetwork;
import dev.gtnhjourney.research.ResearchKey;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

/** Diagnostic and fallback UI for exact NBT states that NEI's stock item list does not expose. */
public final class CommandJourney extends CommandBase {
    @Override public String getCommandName() { return "journey"; }
    @Override public String getCommandUsage(ICommandSender sender) { return "/journey <count|stats|debug|trace|dump|hotspots|list|newest|get|forget|undo|inspect|rescan|prune-missing|clear>"; }
    @Override public int getRequiredPermissionLevel() { return 0; }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.addChatMessage(new ChatComponentText("GTNH Journey: player-only command."));
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) sender;
        String action = args.length == 0 ? "count" : args[0].toLowerCase();

        if ("count".equals(action)) {
            tell(player, "Researched: " + GTNHJourney.RESEARCH.snapshot(player).size());
            return;
        }
        if ("stats".equals(action)) {
            stats(player);
            return;
        }
        if ("debug".equals(action)) {
            for (String line : dev.gtnhjourney.diagnostics.RuntimeCompatibilityReport.lines()) tell(player, line);
            return;
        }
        if ("trace".equals(action)) {
            boolean enabled = args.length < 2
                ? !dev.gtnhjourney.diagnostics.ResearchTrace.enabled(player)
                : "on".equalsIgnoreCase(args[1]) || "true".equalsIgnoreCase(args[1]) || "1".equals(args[1]);
            dev.gtnhjourney.diagnostics.ResearchTrace.set(player, enabled);
            tell(player, "Research trace " + (enabled ? "enabled" : "disabled") + " for this server session.");
            return;
        }
        if ("dump".equals(action)) {
            try {
                java.io.File file = dev.gtnhjourney.diagnostics.JourneyDiagnosticDump.write(player);
                tell(player, "Diagnostic dump written: " + file.getPath());
            } catch (java.io.IOException failure) {
                tell(player, "Diagnostic dump failed: " + failure.getMessage());
            }
            return;
        }
        if ("hotspots".equals(action)) {
            int limit = args.length >= 2 ? Math.max(1, Math.min(50, parseInt(sender, args[1]))) : 10;
            hotspots(player, limit);
            return;
        }
        if ("list".equals(action)) {
            int page = args.length >= 2 ? Math.max(1, parseInt(sender, args[1])) : 1;
            list(player, page);
            return;
        }
        if ("newest".equals(action)) {
            int limit = args.length >= 2 ? Math.max(1, Math.min(100, parseInt(sender, args[1]))) : 20;
            newest(player, limit);
            return;
        }
        if ("get".equals(action)) {
            if (args.length < 2) { tell(player, "Usage: /journey get <index> [amount]"); return; }
            get(player, Math.max(1, parseInt(sender, args[1])), args.length >= 3 ? parseInt(sender, args[2]) : 64);
            return;
        }
        if ("forget".equals(action)) {
            if (args.length < 2) { tell(player, "Usage: /journey forget <index>"); return; }
            forget(player, Math.max(1, parseInt(sender, args[1])));
            return;
        }
        if ("undo".equals(action)) {
            int restored = GTNHJourney.RESEARCH.undo(player);
            if (restored <= 0) { tell(player, "No destructive Journey change to undo."); return; }
            JourneyNetwork.sendFullSync(player, GTNHJourney.RESEARCH.snapshotStacksInUnlockOrder(player));
            tell(player, "Undo restored " + restored + " researched states.");
            return;
        }
        if ("inspect".equals(action)) {
            inspect(player);
            return;
        }
        if ("prune-missing".equals(action)) {
            if (args.length < 2 || !"confirm".equalsIgnoreCase(args[1])) {
                tell(player, "Removes researched states whose registered item no longer exists. Use /journey prune-missing confirm");
                return;
            }
            int removed = GTNHJourney.RESEARCH.pruneUnavailable(player);
            JourneyNetwork.sendFullSync(player, GTNHJourney.RESEARCH.snapshotStacksInUnlockOrder(player));
            tell(player, "Pruned " + removed + " unavailable states. /journey undo can restore the previous snapshot.");
            return;
        }
        if ("rescan".equals(action)) {
            int before = GTNHJourney.RESEARCH.snapshot(player).size();
            final EntityPlayerMP scanPlayer = player;
            PlayerInventoryScanner.scan(player, new PlayerInventoryScanner.StackVisitor() {
                @Override public void visit(ItemStack stack) { GTNHJourney.RESEARCH.unlock(scanPlayer, stack); }
            });
            JourneyNetwork.sendFullSync(player, GTNHJourney.RESEARCH.snapshotStacksInUnlockOrder(player));
            tell(player, "Rescan complete. New: " + (GTNHJourney.RESEARCH.snapshot(player).size() - before));
            return;
        }
        if ("clear".equals(action)) {
            if (args.length < 2 || !"confirm".equalsIgnoreCase(args[1])) {
                tell(player, "This deletes your Journey research. Use /journey clear confirm");
                return;
            }
            int removed = GTNHJourney.RESEARCH.clear(player);
            JourneyNetwork.sendFullSync(player, GTNHJourney.RESEARCH.snapshotStacksInUnlockOrder(player));
            tell(player, "Cleared " + removed + " researched states.");
            return;
        }
        tell(player, getCommandUsage(sender));
    }

    private void hotspots(EntityPlayerMP player, int limit) {
        List<dev.gtnhjourney.diagnostics.ResearchHotspotAnalyzer.Hotspot> hotspots =
            dev.gtnhjourney.diagnostics.ResearchHotspotAnalyzer.top(GTNHJourney.RESEARCH.snapshot(player), limit);
        tell(player, "Top research state hotspots:");
        for (dev.gtnhjourney.diagnostics.ResearchHotspotAnalyzer.Hotspot hotspot : hotspots) {
            tell(player, hotspot.getStates() + " state(s): " + hotspot.getBaseId());
        }
    }

    private void list(EntityPlayerMP player, int page) {
        List<ResearchKey> keys = GTNHJourney.RESEARCH.snapshot(player);
        int perPage = 10;
        int pages = Math.max(1, (keys.size() + perPage - 1) / perPage);
        page = Math.min(page, pages);
        int from = (page - 1) * perPage;
        int to = Math.min(keys.size(), from + perPage);
        tell(player, "Journey " + page + "/" + pages + " (" + keys.size() + " states)");
        for (int i = from; i < to; i++) {
            ResearchKey key = keys.get(i);
            tell(player, (i + 1) + ". " + key.getItemId() + " @" + key.getMeta() + (key.getCanonicalNbt().isEmpty() ? "" : " [NBT]"));
        }
    }

    private void newest(EntityPlayerMP player, int limit) {
        List<ResearchKey> keys = GTNHJourney.RESEARCH.snapshotNewest(player, limit);
        tell(player, "Newest researched states (" + keys.size() + ")");
        for (int i = 0; i < keys.size(); i++) {
            ResearchKey key = keys.get(i);
            tell(player, (i + 1) + ". " + key.getItemId() + " @" + key.getMeta()
                + (key.getCanonicalNbt().isEmpty() ? "" : " [NBT]"));
        }
    }

    private void get(EntityPlayerMP player, int oneBasedIndex, int amount) {
        List<ResearchKey> keys = GTNHJourney.RESEARCH.snapshot(player);
        if (oneBasedIndex < 1 || oneBasedIndex > keys.size()) { tell(player, "No researched state at index " + oneBasedIndex); return; }
        ResearchKey key = keys.get(oneBasedIndex - 1);
        ItemStack stack = GTNHJourney.RESEARCH.retrieve(player, key, amount);
        if (stack == null) { tell(player, "Stored item is unavailable in this pack version: " + key.getItemId()); return; }
        player.inventory.addItemStackToInventory(stack);
        if (stack.stackSize > 0) player.dropPlayerItemWithRandomChoice(stack, false);
        player.inventoryContainer.detectAndSendChanges();
    }

    private void inspect(EntityPlayerMP player) {
        ItemStack stack = player.inventory.getCurrentItem();
        if (stack == null || stack.getItem() == null) { tell(player, "Hold an item to inspect its Journey identity."); return; }
        try {
            ResearchKey key = dev.gtnhjourney.minecraft.ItemStackKeyFactory.from(stack);
            boolean researched = GTNHJourney.RESEARCH.registry(player).contains(key);
            int estimate = dev.gtnhjourney.network.ResearchSyncBudget.estimateBytes(key);
            int wireBytes = dev.gtnhjourney.network.ItemStackPayloadSizer.serializedBytes(stack);
            int endpoints = dev.gtnhjourney.minecraft.ResearchStateExpander.expand(stack).size();
            tell(player, "Identity: " + key.getItemId() + " @" + key.getMeta()
                + (key.getCanonicalNbt().isEmpty() ? " [no semantic NBT]" : " [semantic NBT]")
                + ", researched=" + researched + ", syncEstimate=" + estimate + " B"
                + ", wire=" + wireBytes + " B, clientSync=" + dev.gtnhjourney.network.ItemStackPayloadSizer.canSync(stack));
            tell(player, "Semantic: GT-charge=" + dev.gtnhjourney.minecraft.GtChargeStatePolicy.describe(stack)
                + ", IC2-charge=" + dev.gtnhjourney.minecraft.Ic2ChargeStatePolicy.describe(stack)
                + ", CoFH-charge=" + dev.gtnhjourney.minecraft.CofhChargeStatePolicy.describe(stack)
                + ", GT-tool=" + dev.gtnhjourney.minecraft.GtToolStatePolicy.isVerifiedTool(stack)
                + ", TCon-tool=" + dev.gtnhjourney.minecraft.TconToolStatePolicy.isVerifiedTool(stack)
                + ", observed endpoints=" + endpoints);
        } catch (IllegalArgumentException failure) {
            tell(player, "Cannot build Journey identity: " + failure.getMessage());
        }
    }

    private void stats(EntityPlayerMP player) {
        List<ResearchKey> keys = GTNHJourney.RESEARCH.snapshot(player);
        java.util.HashSet<String> baseItems = new java.util.HashSet<String>();
        int nbtStates = 0;
        int serverOnlyStates = 0;
        int unavailableStates = 0;
        for (ResearchKey key : keys) {
            baseItems.add(key.getItemId() + "@" + key.getMeta());
            if (!key.getCanonicalNbt().isEmpty()) nbtStates++;
            ItemStack diagnosticStack = GTNHJourney.RESEARCH.retrieve(player, key, 1);
            if (diagnosticStack == null) unavailableStates++;
            else if (!dev.gtnhjourney.network.ItemStackPayloadSizer.canSync(diagnosticStack)) serverOnlyStates++;
        }
        tell(player, "States: " + keys.size() + ", base item/meta variants: " + baseItems.size()
            + ", NBT states: " + nbtStates + ", server-only oversized states: " + serverOnlyStates
            + ", unavailable: " + unavailableStates + ", undo snapshot: " + GTNHJourney.RESEARCH.undoSize(player));
    }

    private void forget(EntityPlayerMP player, int oneBasedIndex) {
        List<ResearchKey> keys = GTNHJourney.RESEARCH.snapshot(player);
        if (oneBasedIndex < 1 || oneBasedIndex > keys.size()) {
            tell(player, "No researched state at index " + oneBasedIndex);
            return;
        }
        ResearchKey key = keys.get(oneBasedIndex - 1);
        if (!GTNHJourney.RESEARCH.forget(player, key)) {
            tell(player, "State was already absent.");
            return;
        }
        JourneyNetwork.sendFullSync(player, GTNHJourney.RESEARCH.snapshotStacksInUnlockOrder(player));
        tell(player, "Forgot " + key.getItemId() + " @" + key.getMeta());
    }

    private static void tell(EntityPlayerMP player, String text) { player.addChatMessage(new ChatComponentText("[Journey] " + text)); }
}
