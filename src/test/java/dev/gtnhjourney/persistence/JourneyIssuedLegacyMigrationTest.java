package dev.gtnhjourney.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class JourneyIssuedLegacyMigrationTest {

    @Test
    public void versionOneActivityLoadsWithoutInventingPastIssuedHistory() {
        UUID player = new UUID(101L, 202L);
        ResearchKey old = key("old");
        ResearchKey recent = key("recent");

        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("Version", 1);
        NBTTagList players = new NBTTagList();
        NBTTagCompound playerTag = new NBTTagCompound();
        playerTag.setLong("UuidMost", player.getMostSignificantBits());
        playerTag.setLong("UuidLeast", player.getLeastSignificantBits());
        NBTTagList entries = new NBTTagList();
        entries.appendTag(entry(old));
        entries.appendTag(entry(recent));
        playerTag.setTag("Entries", entries);
        players.appendTag(playerTag);
        root.setTag("Players", players);

        JourneyActivityData data = new JourneyActivityData("test_activity");
        data.readFromNBT(root);

        assertEquals(Arrays.asList(old, recent), data.snapshotReconciled(player, Arrays.asList(old, recent)));
        assertTrue(data.snapshotIssuedOldestFirst(player).isEmpty(),
            "legacy merged activity must never be misrepresented as exact issuance history");
    }

    private static NBTTagCompound entry(ResearchKey key) {
        NBTTagCompound entry = new NBTTagCompound();
        entry.setString("ItemId", key.getItemId());
        entry.setInteger("Meta", key.getMeta());
        entry.setString("CanonicalNbt", key.getCanonicalNbt());
        return entry;
    }

    private static ResearchKey key(String name) {
        return new ResearchKey("test:" + name, 0, "");
    }
}
