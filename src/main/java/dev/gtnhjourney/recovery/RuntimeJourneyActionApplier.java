package dev.gtnhjourney.recovery;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import dev.gtnhjourney.GTNHJourney;
import dev.gtnhjourney.config.JourneyConfig;
import dev.gtnhjourney.network.Journey1124Network;
import dev.gtnhjourney.persistence.JourneyFavouriteData;
import dev.gtnhjourney.research.ResearchFingerprint;
import dev.gtnhjourney.time.JourneySpeedMode;
import dev.gtnhjourney.time.JourneySpeedController;

/** Runtime replay adapter for the persistent non-research action journal. */
public final class RuntimeJourneyActionApplier implements JourneyReversibleActionService.ActionApplier {

    @Override
    public boolean apply(EntityPlayerMP player, JourneyActionTransaction transaction, boolean forward) {
        if (player == null || transaction == null) return false;
        NBTTagCompound target = forward ? transaction.after() : transaction.before();
        switch (transaction.kind()) {
            case SPEED:
                return applySpeed(target);
            case EXPLOSIONS:
                return applyExplosions(target);
            case FAVOURITE:
                return applyFavourite(player, target);
            case DEATH_INVENTORY_RETURN:
                return applyDeathInventory(player, transaction, forward);
            default:
                return false;
        }
    }

    private static boolean applySpeed(NBTTagCompound target) {
        if (GTNHJourney.SPEED == null || target == null) return false;
        JourneySpeedMode mode;
        try {
            mode = JourneySpeedMode.valueOf(target.getString("Mode"));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
        int multiplier = target.getInteger("Multiplier");
        JourneySpeedController.Result result = GTNHJourney.SPEED.set(mode, multiplier);
        return result.status() == JourneySpeedController.Status.APPLIED;
    }

    private static boolean applyExplosions(NBTTagCompound target) {
        if (target == null || GTNHJourney.MACHINE_EXPLOSIONS == null) return false;
        boolean oldGlobal = JourneyConfig.explosionsEnabled();
        boolean oldMachines = GTNHJourney.MACHINE_EXPLOSIONS.isEnabled();
        boolean wantedGlobal = target.getBoolean("Global");
        boolean wantedMachines = target.getBoolean("Machines");
        JourneyConfig.setExplosionsEnabled(wantedGlobal);
        if (!GTNHJourney.MACHINE_EXPLOSIONS.setEnabled(wantedMachines)) {
            JourneyConfig.setExplosionsEnabled(oldGlobal);
            GTNHJourney.MACHINE_EXPLOSIONS.setEnabled(oldMachines);
            return false;
        }
        return JourneyConfig.explosionsEnabled() == wantedGlobal
            && GTNHJourney.MACHINE_EXPLOSIONS.isEnabled() == wantedMachines;
    }

    private static boolean applyFavourite(EntityPlayerMP player, NBTTagCompound target) {
        byte[] bytes = target == null ? null : target.getByteArray("Fingerprint");
        if (bytes == null || bytes.length != ResearchFingerprint.BYTE_LENGTH) return false;
        ResearchFingerprint fingerprint = ResearchFingerprint.fromBytes(bytes);
        boolean value = target.getBoolean("Value");
        if (value && (GTNHJourney.RESEARCH == null || GTNHJourney.RESEARCH.resolve(player, fingerprint) == null)) return false;
        World root = DimensionManager.getWorld(0);
        JourneyFavouriteData data = JourneyFavouriteData.get(root == null ? player.worldObj : root);
        boolean current = data.contains(player.getUniqueID(), fingerprint);
        if (current != value) data.set(player.getUniqueID(), fingerprint, value);
        Journey1124Network.sendFavourites(player, data.snapshot(player.getUniqueID()));
        return data.contains(player.getUniqueID(), fingerprint) == value;
    }

    private static boolean applyDeathInventory(
        EntityPlayerMP player,
        JourneyActionTransaction transaction,
        boolean forward) {
        DeathInventorySnapshot expected = DeathInventorySnapshot.fromNbt(forward ? transaction.before() : transaction.after());
        DeathInventorySnapshot target = DeathInventorySnapshot.fromNbt(forward ? transaction.after() : transaction.before());
        return DeathInventorySnapshot.applyExactIfCurrentMatches(player, expected, target);
    }
}
