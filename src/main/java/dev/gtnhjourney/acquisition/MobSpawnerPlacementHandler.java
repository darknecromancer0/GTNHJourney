package dev.gtnhjourney.acquisition;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import dev.gtnhjourney.minecraft.MobSpawnerStatePolicy;
import net.minecraft.init.Blocks;
import net.minecraftforge.event.world.BlockEvent;

/** Applies Journey's exact mob-spawner entity id after vanilla 1.7.10 creates its default Pig tile entity. */
public final class MobSpawnerPlacementHandler {

    @SubscribeEvent
    public void onPlace(BlockEvent.PlaceEvent event) {
        if (event == null || event.world == null || event.world.isRemote || event.placedBlock != Blocks.mob_spawner) return;
        MobSpawnerStatePolicy.applyPlacedSpawner(event.world, event.x, event.y, event.z, event.itemInHand);
    }
}
