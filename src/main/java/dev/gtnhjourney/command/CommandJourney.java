package dev.gtnhjourney.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import dev.gtnhjourney.GTNHJourney;
import dev.gtnhjourney.acquisition.ManualInventoryResearchService;
import dev.gtnhjourney.network.JourneyNetwork;
import dev.gtnhjourney.recovery.JourneySnapshot;
import dev.gtnhjourney.research.ResearchKey;
import dev.gtnhjourney.time.JourneySpeedController;

/** Diagnostic, recovery and fallback UI for Journey research. */
public final class CommandJourney extends CommandBase {

    private static final String[] SUBCOMMANDS = { "help", "count", "stats", "inspect", "research", "rescan", "list",
        "newest", "get", "forget", "undo", "redo", "restore-deleted", "snapshot", "snapshots", "restore", "debug",
        "trace", "dump", "hotspots", "debugtool", "prune-missing", "clear", "backup", "explosions", "cleanse",
        "speed" };

    @Override
    public String getCommandName() {
        return "journey";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/journey help";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
        if (args.length == 2) {
            String action = args[0].toLowerCase(Locale.ROOT);
            if ("trace".equals(action)) return getListOfStringsMatchingLastWord(args, "on", "off");
            if ("backup".equals(action)) return getListOfStringsMatchingLastWord(args, "status", "now", "on", "off");
            if ("explosions".equals(action)) return getListOfStringsMatchingLastWord(args, "status", "on", "off");
            if ("speed".equals(action)) return getListOfStringsMatchingLastWord(args, "1", "2", "4", "8", "status");
            if ("clear".equals(action) || "prune-missing".equals(action)) {
                return getListOfStringsMatchingLastWord(args, "confirm");
            }
        }
        return Collections.emptyList();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.addChatMessage(new ChatComponentText("GTNH Journey: player-only command."));
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) sender;
        String action = args.length == 0 ? "count" : args[0].toLowerCase(Locale.ROOT);

        if (JourneySafetyCommandHandler.handle(player, args)) return;
        if ("help".equals(action)) {
            help(player);
            return;
        }
        if ("count".equals(action)) {
            tell(player, "Researched: " + GTNHJourney.RESEARCH.snapshot(player).size());
            return;
        }
        if ("stats".equals(action)) {
            stats(player);
            return;
        }
        if ("speed".equals(action)) {
            speed(player, args);
            return;
        }
        if ("debug".equals(action)) {
            for (String line : dev.gtnhjourney.diagnostics.RuntimeCompatibilityReport.lines()) tell(player, line);
            return;
        }
        if ("debugtool".equals(action)) {
            if (!DebugToolPermissionPolicy.mayUse(player)) {
                tell(player, "Debug Researcher Tool requires the integrated-server owner or operator permission.");
                return;
            }
            giveDebugTool(player);
            return;
        }
        if ("trace".equals(action)) {
            boolean enabled = args.length < 2 ? !dev.gtnhjourney.diagnostics.ResearchTrace.enabled(player)
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
            if (args.length < 2) {
                tell(player, "Usage: /journey get <index> [amount]");
                return;
            }
            get(player, Math.max(1, parseInt(sender, args[1])), args.length >= 3 ? parseInt(sender, args[2]) : 64);
            return;
        }
        if ("forget".equals(action)) {
            if (args.length < 2) {
                tell(player, "Usage: /journey forget <index>");
                return;
            }
            forget(player, Math.max(1, parseInt(sender, args[1])));
            return;
        }
        if ("undo".equals(action)) {
            int requested = JourneyCommandPolicy.parseUndoRedoCount(args.length >= 2 ? args[1] : null);
            int applied = GTNHJourney.MUTATIONS.undo(player, requested);
            if (applied <= 0) {
                tell(player, "Nothing to undo.");
                return;
            }
            sync(player);
            tell(player, "Undid " + applied + " Journey transaction(s).");
            return;
        }
        if ("redo".equals(action)) {
            int requested = JourneyCommandPolicy.parseUndoRedoCount(args.length >= 2 ? args[1] : null);
            int applied = GTNHJourney.MUTATIONS.redo(player, requested);
            if (applied <= 0) {
                tell(player, "Nothing to redo.");
                return;
            }
            sync(player);
            tell(player, "Redid " + applied + " Journey transaction(s).");
            return;
        }
        if ("restore-deleted".equals(action)) {
            int requested = JourneyCommandPolicy.parseRestoreDeletedCount(args.length >= 2 ? args[1] : null);
            int restored = GTNHJourney.MUTATIONS.restoreDeleted(player, requested);
            if (restored <= 0) {
                tell(player, "No deleted Journey states could be restored.");
                return;
            }
            sync(player);
            tell(player, "Restored " + restored + " deleted researched state(s).");
            return;
        }
        if ("snapshot".equals(action)) {
            String name = args.length >= 2 ? args[1] : null;
            JourneySnapshot snapshot = GTNHJourney.MUTATIONS.createManualSnapshot(player, name);
            if (snapshot == null) {
                tell(player, "Snapshot could not be created.");
                return;
            }
            tell(
                player,
                "Snapshot #" + snapshot.id() + " " + snapshot.name() + ": " + snapshot.state().size() + " state(s).");
            return;
        }
        if ("snapshots".equals(action)) {
            snapshots(player);
            return;
        }
        if ("restore".equals(action)) {
            if (args.length < 2) {
                tell(player, "Usage: /journey restore <snapshot-id-or-name>");
                return;
            }
            JourneySnapshot snapshot = GTNHJourney.MUTATIONS.findSnapshot(player, args[1]);
            if (snapshot == null) {
                tell(player, "Snapshot not found: " + args[1]);
                return;
            }
            if (!GTNHJourney.MUTATIONS.restoreSnapshot(player, snapshot)) {
                tell(player, "Research already matches that snapshot or restore failed safely.");
                return;
            }
            sync(player);
            tell(
                player,
                "Restored snapshot #" + snapshot.id() + " " + snapshot.name() + " (" + snapshot.state().size()
                    + " states). /journey undo reverses this restore.");
            return;
        }
        if ("inspect".equals(action)) {
            inspect(player);
            return;
        }
        if ("research".equals(action)) {
            List<ItemStack> candidates = HeldItemResearchCommand.candidates(player.inventory.getCurrentItem());
            if (candidates.isEmpty()) {
                tell(player, "Hold an item to research it.");
                return;
            }
            int added = GTNHJourney.MUTATIONS.applyBulkAdd(player, candidates, "Research held item");
            // This command is also an explicit refresh path. Sync even when the semantic state already existed.
            sync(player);
            tell(player, "Held item research refreshed. New: " + added);
            return;
        }
        if ("prune-missing".equals(action)) {
            if (args.length < 2 || !"confirm".equalsIgnoreCase(args[1])) {
                tell(
                    player,
                    "Removes researched states whose registered item no longer exists. Use /journey prune-missing confirm");
                return;
            }
            List<ResearchKey> unavailable = unavailableKeys(player);
            int removed = 0;
            if (!unavailable.isEmpty()) {
                GTNHJourney.MUTATIONS.createSafetySnapshot(player, "before-prune-missing");
                removed = GTNHJourney.MUTATIONS.deleteMany(player, unavailable, "Prune unavailable research");
            }
            if (removed > 0) sync(player);
            tell(player, JourneyCommandPolicy.pruneMissingResult(removed));
            return;
        }
        if ("rescan".equals(action)) {
            ManualInventoryResearchService.Result result = ManualInventoryResearchService.scan(
                player,
                GTNHJourney.RESEARCH,
                GTNHJourney.MUTATIONS);
            tell(player, result.summary());
            return;
        }
        if ("clear".equals(action)) {
            if (args.length < 2 || !"confirm".equalsIgnoreCase(args[1])) {
                tell(player, "This deletes your Journey research. Use /journey clear confirm");
                return;
            }
            List<ResearchKey> keys = new ArrayList<ResearchKey>(GTNHJourney.RESEARCH.snapshot(player));
            int removed = 0;
            if (!keys.isEmpty()) {
                GTNHJourney.MUTATIONS.createSafetySnapshot(player, "before-clear");
                removed = GTNHJourney.MUTATIONS.deleteMany(player, keys, "Clear Journey research");
            }
            if (removed > 0) sync(player);
            tell(player, "Cleared " + removed + " researched states. /journey undo reverses the operation.");
            return;
        }
        tell(player, "Unknown Journey command: " + action);
        help(player);
    }

    private void speed(EntityPlayerMP player, String[] args) {
        if (args.length < 2 || "status".equalsIgnoreCase(args[1])) {
            tell(
                player,
                "Journey speed: " + GTNHJourney.SPEED.multiplier() + "x (target " + GTNHJourney.SPEED.targetTps()
                    + " TPS). Hook: " + (GTNHJourney.SPEED.supported() ? "supported" : "unsupported") + ".");
            return;
        }
        if (!JourneyAdminPermissionPolicy.mayMutate(player)) {
            tell(player, "Journey speed changes require the integrated-server owner or operator permission.");
            return;
        }
        Integer multiplier = JourneySpeedCommandPolicy.parseMultiplier(args[1]);
        if (multiplier == null) {
            tell(player, "Usage: /journey speed <1|2|4|8|status>");
            return;
        }
        JourneySpeedController.Result result = GTNHJourney.SPEED.setMultiplier(multiplier.intValue());
        if (result.status() == JourneySpeedController.Status.APPLIED) {
            tell(
                player,
                "Journey speed set to " + result.multiplier() + "x (target " + result.targetTps()
                    + " TPS) for this server session.");
            return;
        }
        if (result.status() == JourneySpeedController.Status.UNSUPPORTED) {
            tell(player, "Journey speed hook is unavailable in this runtime. Speed remains 1x.");
            return;
        }
        if (result.status() == JourneySpeedController.Status.FAILED) {
            tell(player, "Journey speed change failed safely. Speed restored to 1x.");
            return;
        }
        tell(player, "Usage: /journey speed <1|2|4|8|status>");
    }

    private void help(EntityPlayerMP player) {
        for (String line : JourneyHelpText.lines()) tell(player, line);
    }

    private void hotspots(EntityPlayerMP player, int limit) {
        List<dev.gtnhjourney.diagnostics.ResearchHotspotAnalyzer.Hotspot> hotspots = dev.gtnhjourney.diagnostics.ResearchHotspotAnalyzer
            .top(GTNHJourney.RESEARCH.snapshot(player), limit);
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
            tell(
                player,
                (i + 1) + ". " + key.getItemId() + " @" + key.getMeta()
                    + (key.getCanonicalNbt().isEmpty() ? "" : " [NBT]"));
        }
    }

    private void newest(EntityPlayerMP player, int limit) {
        List<ResearchKey> keys = GTNHJourney.RESEARCH.snapshotNewest(player, limit);
        tell(player, "Newest researched states (" + keys.size() + ")");
        for (int i = 0; i < keys.size(); i++) {
            ResearchKey key = keys.get(i);
            tell(
                player,
                (i + 1) + ". " + key.getItemId() + " @" + key.getMeta()
                    + (key.getCanonicalNbt().isEmpty() ? "" : " [NBT]"));
        }
    }

    private void snapshots(EntityPlayerMP player) {
        List<JourneySnapshot> snapshots = GTNHJourney.MUTATIONS.snapshots(player);
        if (snapshots.isEmpty()) {
            tell(player, "No Journey snapshots yet.");
            return;
        }
        tell(player, "Journey snapshots (newest first):");
        for (JourneySnapshot snapshot : snapshots) {
            tell(
                player,
                "#" + snapshot.id() + " " + snapshot.kind().name() + " " + snapshot.name() + " - "
                    + snapshot.state().size() + " state(s), tick " + snapshot.worldTick());
        }
    }

    private void get(EntityPlayerMP player, int oneBasedIndex, int amount) {
        List<ResearchKey> keys = GTNHJourney.RESEARCH.snapshot(player);
        if (oneBasedIndex < 1 || oneBasedIndex > keys.size()) {
            tell(player, "No researched state at index " + oneBasedIndex);
            return;
        }
        ResearchKey key = keys.get(oneBasedIndex - 1);
        ItemStack stack = GTNHJourney.RESEARCH.retrieve(player, key, amount);
        if (stack == null) {
            tell(player, "Stored item is unavailable in this pack version: " + key.getItemId());
            return;
        }
        player.inventory.addItemStackToInventory(stack);
        if (stack.stackSize > 0) player.dropPlayerItemWithRandomChoice(stack, false);
        player.inventoryContainer.detectAndSendChanges();
    }

    private void giveDebugTool(EntityPlayerMP player) {
        if (GTNHJourney.DEBUG_RESEARCHER_TOOL == null) {
            tell(player, "Debug Researcher Tool is not registered.");
            return;
        }
        ItemStack tool = new ItemStack(GTNHJourney.DEBUG_RESEARCHER_TOOL, 1, 0);
        player.inventory.addItemStackToInventory(tool);
        if (tool.stackSize > 0) player.dropPlayerItemWithRandomChoice(tool, false);
        player.inventoryContainer.detectAndSendChanges();
        tell(player, "Debug Researcher Tool granted. Shift+right-click cycles BLOCK / CONTENTS / AREA_16.");
    }

    private void inspect(EntityPlayerMP player) {
        ItemStack stack = player.inventory.getCurrentItem();
        if (stack == null || stack.getItem() == null) {
            tell(player, "Hold an item to inspect its Journey identity.");
            return;
        }
        try {
            ResearchKey key = dev.gtnhjourney.minecraft.ItemStackKeyFactory.from(stack);
            boolean researched = GTNHJourney.RESEARCH.registry(player).contains(key);
            int estimate = dev.gtnhjourney.network.ResearchSyncBudget.estimateBytes(key);
            int wireBytes = dev.gtnhjourney.network.ItemStackPayloadSizer.serializedBytes(stack);
            int endpoints = dev.gtnhjourney.minecraft.ResearchStateExpander.expand(stack).size();
            tell(
                player,
                "Identity: " + key.getItemId() + " @" + key.getMeta()
                    + (key.getCanonicalNbt().isEmpty() ? " [no semantic NBT]" : " [semantic NBT]")
                    + ", researched=" + researched + ", syncEstimate=" + estimate + " B" + ", wire=" + wireBytes
                    + " B, clientSync=" + dev.gtnhjourney.network.ItemStackPayloadSizer.canSync(stack));
            dev.gtnhjourney.diagnostics.SemanticDiagnosticSnapshot semantic = new dev.gtnhjourney.diagnostics.SemanticDiagnosticSnapshot(
                dev.gtnhjourney.minecraft.GtChargeStatePolicy.describe(stack),
                dev.gtnhjourney.minecraft.Ic2ChargeStatePolicy.describe(stack),
                dev.gtnhjourney.minecraft.CofhChargeStatePolicy.describe(stack),
                dev.gtnhjourney.minecraft.OpenComputersChargeStatePolicy.describe(stack),
                dev.gtnhjourney.minecraft.GtToolStatePolicy.isVerifiedTool(stack),
                dev.gtnhjourney.minecraft.TconToolStatePolicy.isVerifiedTool(stack),
                dev.gtnhjourney.minecraft.BotaniaTransientStatePolicy.isVerifiedMagnetRing(stack),
                dev.gtnhjourney.minecraft.DraconicTransientStatePolicy.isVerifiedTool(stack),
                dev.gtnhjourney.minecraft.WearableTransientStatePolicy.matches(stack),
                endpoints);
            tell(player, semantic.inspectLine());
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
        tell(
            player,
            "States: " + keys.size() + ", base item/meta variants: " + baseItems.size() + ", NBT states: " + nbtStates
                + ", server-only oversized states: " + serverOnlyStates + ", unavailable: " + unavailableStates
                + ", undo=" + GTNHJourney.MUTATIONS.undoDepth(player) + ", redo=" + GTNHJourney.MUTATIONS.redoDepth(player)
                + ", deleted=" + GTNHJourney.MUTATIONS.activeDeletionCount(player) + "/"
                + GTNHJourney.MUTATIONS.deletionCount(player));
    }

    private void forget(EntityPlayerMP player, int oneBasedIndex) {
        List<ResearchKey> keys = GTNHJourney.RESEARCH.snapshot(player);
        if (oneBasedIndex < 1 || oneBasedIndex > keys.size()) {
            tell(player, "No researched state at index " + oneBasedIndex);
            return;
        }
        ResearchKey key = keys.get(oneBasedIndex - 1);
        if (!GTNHJourney.MUTATIONS.deleteExact(player, key, "Forget command")) {
            tell(player, "State was already absent.");
            return;
        }
        sync(player);
        tell(player, "Forgot " + key.getItemId() + " @" + key.getMeta() + ". /journey undo reverses it.");
    }

    private List<ResearchKey> unavailableKeys(EntityPlayerMP player) {
        List<ResearchKey> unavailable = new ArrayList<ResearchKey>();
        for (ResearchKey key : GTNHJourney.RESEARCH.snapshot(player)) {
            if (GTNHJourney.RESEARCH.retrieve(player, key, 1) == null) unavailable.add(key);
        }
        return unavailable;
    }

    private static void sync(EntityPlayerMP player) {
        JourneyNetwork.sendFullSync(player, GTNHJourney.RESEARCH.snapshotStacksInUnlockOrder(player));
    }

    private static void tell(EntityPlayerMP player, String text) {
        player.addChatMessage(new ChatComponentText("[Journey] " + text));
    }
}
