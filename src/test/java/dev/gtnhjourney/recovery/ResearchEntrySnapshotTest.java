package dev.gtnhjourney.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;

public class ResearchEntrySnapshotTest {

    @Test
    public void templateIsDefensivelyCopiedAndIndexSurvives() {
        ResearchKey key = new ResearchKey("minecraft:stone", 3, "{Mode:1b}");
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Mode", 1);

        ResearchEntrySnapshot snapshot = new ResearchEntrySnapshot(key, tag, 17);
        tag.setInteger("Mode", 99);

        assertEquals(key, snapshot.key());
        assertEquals(17, snapshot.timelineIndex());
        assertEquals(1, snapshot.template().getInteger("Mode"));
        assertNotSame(tag, snapshot.template());

        NBTTagCompound exposed = snapshot.template();
        exposed.setInteger("Mode", 55);
        assertEquals(1, snapshot.template().getInteger("Mode"));
    }
}
