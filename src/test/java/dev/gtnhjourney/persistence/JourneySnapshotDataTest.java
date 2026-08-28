package dev.gtnhjourney.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.minecraft.NbtCanonicalizer;
import dev.gtnhjourney.recovery.JourneySnapshot;
import dev.gtnhjourney.recovery.ResearchEntrySnapshot;
import dev.gtnhjourney.recovery.ResearchStateSnapshot;
import dev.gtnhjourney.recovery.SnapshotKind;
import dev.gtnhjourney.research.ResearchKey;

public class JourneySnapshotDataTest {

    @Test
    public void snapshotsRoundTripExactOrderTemplatesAndMetadata() {
        UUID player = UUID.randomUUID();
        JourneySnapshotData original = new JourneySnapshotData();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("FluidName", "water");
        ResearchStateSnapshot state = new ResearchStateSnapshot(
            Arrays.asList(
                new ResearchEntrySnapshot(new ResearchKey("minecraft:stone", 0, ""), null, 0),
                new ResearchEntrySnapshot(new ResearchKey("IC2:itemFluidCell", 0, "{FluidName=water}"), tag, 1)));
        original.add(player, new JourneySnapshot(9L, "base-before-migration", 12345L, SnapshotKind.MANUAL, state));

        NBTTagCompound root = new NBTTagCompound();
        original.writeToNBT(root);

        JourneySnapshotData restored = new JourneySnapshotData();
        restored.readFromNBT(root);
        JourneySnapshot snapshot = restored.manualSnapshots(player).get(0);

        assertEquals(9L, snapshot.id());
        assertEquals("base-before-migration", snapshot.name());
        assertEquals(12345L, snapshot.worldTick());
        assertEquals(SnapshotKind.MANUAL, snapshot.kind());
        assertEquals(2, snapshot.state().size());
        assertEquals("minecraft:stone", snapshot.state().entries().get(0).key().getItemId());
        assertEquals("water", snapshot.state().entries().get(1).template().getString("FluidName"));
    }

    @Test
    public void legacySemanticDuplicatesAreRecanonicalizedAndDeduplicatedOnLoad() {
        UUID player = UUID.randomUUID();
        JourneySnapshotData original = new JourneySnapshotData();
        ResearchStateSnapshot state = new ResearchStateSnapshot(
            Arrays.asList(legacyChiselEntry("minecraft:stone", 0), legacyChiselEntry("minecraft:dirt", 1)));
        original.add(player, new JourneySnapshot(10L, "legacy-chisel-target", 12346L, SnapshotKind.MANUAL, state));

        NBTTagCompound root = new NBTTagCompound();
        original.writeToNBT(root);

        JourneySnapshotData restored = new JourneySnapshotData();
        restored.readFromNBT(root);
        JourneySnapshot snapshot = restored.manualSnapshots(player).get(0);

        assertEquals(1, snapshot.state().size());
        assertEquals("chisel:chisel", snapshot.state().entries().get(0).key().getItemId());
        assertEquals(0, snapshot.state().entries().get(0).key().getMeta());
        assertEquals("", snapshot.state().entries().get(0).key().getCanonicalNbt());
        assertNull(snapshot.state().entries().get(0).template());
        assertEquals(0, snapshot.state().entries().get(0).timelineIndex());
    }

    private static ResearchEntrySnapshot legacyChiselEntry(String targetId, int timelineIndex) {
        NBTTagCompound target = new NBTTagCompound();
        target.setString("id", targetId);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("chiselTarget", target);
        return new ResearchEntrySnapshot(
            new ResearchKey("chisel:chisel", 0, NbtCanonicalizer.canonicalize(tag)),
            tag,
            timelineIndex);
    }
}
