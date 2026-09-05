package dev.gtnhjourney.command;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import dev.gtnhjourney.GTNHJourney;
import dev.gtnhjourney.config.JourneyConfig;
import dev.gtnhjourney.network.JourneyNetwork;
import dev.gtnhjourney.recovery.DeathInventoryReturnService;
import dev.gtnhjourney.recovery.JourneyActionKind;
import dev.gtnhjourney.recovery.JourneyUndoCoordinator;
import dev.gtnhjourney.time.JourneySpeedController;
import dev.gtnhjourney.time.JourneySpeedMode;

/** 1.1.24 command facade. Unchanged commands delegate to the proven 1.1.23 implementation. */
public final class CommandJourney1124 extends CommandBase {

    private final CommandJourney legacy = new CommandJourney();

    @Override public String getCommandName() { return legacy.getCommandName(); }
    @Override public String getCommandUsage(ICommandSender sender) { return legacy.getCommandUsage(sender); }
    @Override public int getRequiredPermissionLevel() { return legacy.getRequiredPermissionLevel(); }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args == null || args.length == 0) {
            legacy.processCommand(sender, args);
            return;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (!(sender instanceof EntityPlayerMP)) {
            legacy.processCommand(sender, args);
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) sender;

        if ("undo".equals(action) || "redo".equals(action)) {
            unifiedUndoRedo(player, args, "redo".equals(action));
            return;
        }
        if ("speed".equals(action)) {
            speed(player, args);
            return;
        }
        if ("explosions".equals(action)) {
            explosions(player, args);
            return;
        }
        if ("return".equals(action) && args.length >= 3 && "death".equalsIgnoreCase(args[1])
            && "inventory".equalsIgnoreCase(args[2])) {
            deathReturn(player);
            return;
        }
        if ("death".equals(action) && args.length >= 2 && "inventory".equalsIgnoreCase(args[1])) {
            deathInventory(player, args);
            return;
        }
        legacy.processCommand(sender, args);
    }

    private static void unifiedUndoRedo(EntityPlayerMP player, String[] args, boolean redo) {
        int count = JourneyCommandPolicy.parseUndoRedoCount(args.length > 1 ? args[1] : null);
        JourneyUndoCoordinator.Result result = redo ? GTNHJourney.UNDO.redo(player, count) : GTNHJourney.UNDO.undo(player, count);
        if (result.applied() > 0) syncResearch(player);
        tell(player, (redo ? "Redid " : "Undid ") + result.applied() + " action(s): " + result.researchApplied()
            + " research, " + result.actionApplied() + " runtime.");
    }

    private static void speed(EntityPlayerMP player, String[] args) {
        if (args.length == 1 || "status".equalsIgnoreCase(args[1])) {
            speedStatus(player);
            return;
        }
        if (!DebugToolPermissionPolicy.mayUse(player)) {
            tell(player, "Speed controls require the integrated-server owner or operator permission.");
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        if ("undo".equals(sub) || "redo".equals(sub)) {
            int count = JourneyCommandPolicy.parseUndoRedoCount(args.length > 2 ? args[2] : null);
            int changed = "redo".equals(sub)
                ? GTNHJourney.ACTIONS.redo(player, JourneyActionKind.SPEED, count)
                : GTNHJourney.ACTIONS.undo(player, JourneyActionKind.SPEED, count);
            tell(player, ("redo".equals(sub) ? "Redid " : "Undid ") + changed + " speed action(s).");
            speedStatus(player);
            return;
        }
        NBTTagCompound before = speedState();
        JourneySpeedController.Result result;
        if ("default".equals(sub)) {
            result = GTNHJourney.SPEED.set(JourneySpeedMode.MACHINES, 1);
        } else {
            JourneySpeedMode mode = JourneySpeedMode.parse(sub);
            int multiplier;
            if (mode != null) {
                if (args.length < 3) {
                    tell(player, speedUsage());
                    return;
                }
                multiplier = parseInt(args[2], -1);
            } else {
                mode = JourneySpeedMode.MACHINES;
                multiplier = parseInt(args[1], -1);
            }
            result = GTNHJourney.SPEED.set(mode, multiplier);
        }
        if (result.status() != JourneySpeedController.Status.APPLIED) {
            tell(player, "Speed change failed: " + result.status().name() + ". " + speedUsage());
            return;
        }
        NBTTagCompound after = speedState();
        GTNHJourney.MUTATIONS.notePassiveMutation(player);
        GTNHJourney.ACTIONS.record(player, JourneyActionKind.SPEED, "Speed change", before, after);
        speedStatus(player);
    }

    private static void explosions(EntityPlayerMP player, String[] args) {
        if (args.length == 1 || "status".equalsIgnoreCase(args[1])) {
            explosionStatus(player);
            return;
        }
        if (!DebugToolPermissionPolicy.mayUse(player)) {
            tell(player, "Explosion controls require the integrated-server owner or operator permission.");
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        if ("undo".equals(sub) || "redo".equals(sub)) {
            int count = JourneyCommandPolicy.parseUndoRedoCount(args.length > 2 ? args[2] : null);
            int changed = "redo".equals(sub)
                ? GTNHJourney.ACTIONS.redo(player, JourneyActionKind.EXPLOSIONS, count)
                : GTNHJourney.ACTIONS.undo(player, JourneyActionKind.EXPLOSIONS, count);
            tell(player, ("redo".equals(sub) ? "Redid " : "Undid ") + changed + " explosion action(s).");
            explosionStatus(player);
            return;
        }

        NBTTagCompound before = explosionState();
        boolean applied = true;
        if ("default".equals(sub)) {
            JourneyConfig.setExplosionsEnabled(true);
            applied = GTNHJourney.MACHINE_EXPLOSIONS.setEnabled(true);
        } else if ("machines".equals(sub)) {
            if (args.length < 3 || "status".equalsIgnoreCase(args[2])) {
                tell(player, "Machine explosions: " + onOff(GTNHJourney.MACHINE_EXPLOSIONS.isEnabled()) + ".");
                return;
            }
            String value = args[2].toLowerCase(Locale.ROOT);
            if (!"on".equals(value) && !"off".equals(value)) {
                tell(player, explosionUsage());
                return;
            }
            applied = GTNHJourney.MACHINE_EXPLOSIONS.setEnabled("on".equals(value));
        } else if ("on".equals(sub) || "off".equals(sub)) {
            JourneyConfig.setExplosionsEnabled("on".equals(sub));
        } else {
            tell(player, explosionUsage());
            return;
        }

        if (!applied) {
            applyExplosionState(before);
            tell(player, "GregTech machine-explosion switch is unavailable; no partial change was kept.");
            return;
        }
        NBTTagCompound after = explosionState();
        GTNHJourney.MUTATIONS.notePassiveMutation(player);
        GTNHJourney.ACTIONS.record(player, JourneyActionKind.EXPLOSIONS, "Explosion change", before, after);
        explosionStatus(player);
    }

    private static void deathInventory(EntityPlayerMP player, String[] args) {
        String sub = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "status";
        if ("return".equals(sub)) {
            deathReturn(player);
            return;
        }
        if ("undo".equals(sub) || "redo".equals(sub)) {
            int count = JourneyCommandPolicy.parseUndoRedoCount(args.length > 3 ? args[3] : null);
            int changed = "redo".equals(sub)
                ? GTNHJourney.ACTIONS.redo(player, JourneyActionKind.DEATH_INVENTORY_RETURN, count)
                : GTNHJourney.ACTIONS.undo(player, JourneyActionKind.DEATH_INVENTORY_RETURN, count);
            tell(player, ("redo".equals(sub) ? "Redid " : "Undid ") + changed + " death-inventory return action(s).");
            return;
        }
        if (!"status".equals(sub)) {
            tell(player, "/journey death inventory status|return|undo [n]|redo [n]");
            return;
        }
        DeathInventoryReturnService.Result status = GTNHJourney.DEATH_INVENTORY.status(player);
        tell(player, status.recoverable()
            ? "Death inventory recovery available; missing item units now: " + status.missing() + ". Use /journey return death inventory."
            : "No unresolved death inventory loss is currently detected.");
    }

    private static void deathReturn(EntityPlayerMP player) {
        DeathInventoryReturnService.Result result = GTNHJourney.DEATH_INVENTORY.restore(player);
        if (result.restored() <= 0) {
            tell(player, result.missing() <= 0 ? "Death inventory snapshot has no missing contents to return."
                : "Death inventory recovery could not return the missing contents safely.");
            return;
        }
        GTNHJourney.MUTATIONS.notePassiveMutation(player);
        tell(player, "Returned " + result.restored() + " missing item unit(s) from the last keepInventory death snapshot.");
    }

    private static NBTTagCompound speedState() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Mode", GTNHJourney.SPEED.mode().name());
        tag.setInteger("Multiplier", GTNHJourney.SPEED.multiplier());
        return tag;
    }

    private static NBTTagCompound explosionState() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("Global", JourneyConfig.explosionsEnabled());
        tag.setBoolean("Machines", GTNHJourney.MACHINE_EXPLOSIONS.isEnabled());
        return tag;
    }

    private static void applyExplosionState(NBTTagCompound tag) {
        if (tag == null) return;
        JourneyConfig.setExplosionsEnabled(tag.getBoolean("Global"));
        GTNHJourney.MACHINE_EXPLOSIONS.setEnabled(tag.getBoolean("Machines"));
    }

    private static void speedStatus(EntityPlayerMP player) {
        tell(player, "Speed: " + GTNHJourney.SPEED.mode().commandName() + " x" + GTNHJourney.SPEED.multiplier()
            + " (target " + GTNHJourney.SPEED.targetTps() + " TPS, server " + GTNHJourney.SPEED.serverTargetTps() + " TPS).");
    }

    private static void explosionStatus(EntityPlayerMP player) {
        tell(player, "Explosions: global=" + onOff(JourneyConfig.explosionsEnabled()) + ", machines="
            + onOff(GTNHJourney.MACHINE_EXPLOSIONS.isEnabled()) + ".");
    }

    private static String onOff(boolean value) { return value ? "on" : "off"; }
    private static String speedUsage() { return "/journey speed status|default|undo [n]|redo [n]|machines <1|2|4|8|16|32|64|128>|world <1|2|4|8|16|32|64|128>"; }
    private static String explosionUsage() { return "/journey explosions status|on|off|default|undo [n]|redo [n]|machines <status|on|off>"; }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (RuntimeException ignored) { return fallback; }
    }

    private static void syncResearch(EntityPlayerMP player) {
        JourneyNetwork.sendFullSync(
            player,
            GTNHJourney.RESEARCH.snapshotStacksInUnlockOrder(player),
            GTNHJourney.RESEARCH.snapshotActivityOrder(player));
    }

    private static void tell(EntityPlayerMP player, String text) {
        if (player != null) player.addChatMessage(new ChatComponentText("[Journey] " + text));
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args == null) return null;
        if (args.length == 1) {
            Set<String> values = new LinkedHashSet<String>();
            List legacyValues = legacy.addTabCompletionOptions(sender, args);
            if (legacyValues != null) for (Object value : legacyValues) if (value != null) values.add(String.valueOf(value));
            values.addAll(getListOfStringsMatchingLastWord(args, "return", "death"));
            return new ArrayList<String>(values);
        }
        String first = args[0].toLowerCase(Locale.ROOT);
        if ("undo".equals(first) || "redo".equals(first)) {
            if (args.length == 2) return getListOfStringsMatchingLastWord(args, "1", "5", "10");
        }
        if ("speed".equals(first)) {
            if (args.length == 2) return getListOfStringsMatchingLastWord(
                args, "status", "default", "undo", "redo", "machines", "world", "1", "2", "4", "8", "16", "32", "64", "128");
            if (args.length == 3 && ("machines".equalsIgnoreCase(args[1]) || "world".equalsIgnoreCase(args[1]))) {
                return getListOfStringsMatchingLastWord(args, "1", "2", "4", "8", "16", "32", "64", "128");
            }
            if (args.length == 3 && ("undo".equalsIgnoreCase(args[1]) || "redo".equalsIgnoreCase(args[1]))) {
                return getListOfStringsMatchingLastWord(args, "1", "5", "10");
            }
        }
        if ("explosions".equals(first)) {
            if (args.length == 2) return getListOfStringsMatchingLastWord(
                args, "status", "on", "off", "default", "undo", "redo", "machines");
            if (args.length == 3 && "machines".equalsIgnoreCase(args[1])) {
                return getListOfStringsMatchingLastWord(args, "status", "on", "off");
            }
            if (args.length == 3 && ("undo".equalsIgnoreCase(args[1]) || "redo".equalsIgnoreCase(args[1]))) {
                return getListOfStringsMatchingLastWord(args, "1", "5", "10");
            }
        }
        if ("return".equals(first)) {
            if (args.length == 2) return getListOfStringsMatchingLastWord(args, "death");
            if (args.length == 3 && "death".equalsIgnoreCase(args[1])) return getListOfStringsMatchingLastWord(args, "inventory");
        }
        if ("death".equals(first)) {
            if (args.length == 2) return getListOfStringsMatchingLastWord(args, "inventory");
            if (args.length == 3 && "inventory".equalsIgnoreCase(args[1])) return getListOfStringsMatchingLastWord(args, "status", "return", "undo", "redo");
            if (args.length == 4 && "inventory".equalsIgnoreCase(args[1])
                && ("undo".equalsIgnoreCase(args[2]) || "redo".equalsIgnoreCase(args[2]))) {
                return getListOfStringsMatchingLastWord(args, "1", "5", "10");
            }
        }
        return legacy.addTabCompletionOptions(sender, args);
    }
}
