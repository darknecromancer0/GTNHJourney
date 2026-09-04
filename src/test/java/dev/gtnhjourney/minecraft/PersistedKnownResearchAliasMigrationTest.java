package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class PersistedKnownResearchAliasMigrationTest {

    @Test
    public void randomThingsTilledFertilizedDirtMigratesWithoutRuntimeRegistryLookup() {
        PersistedResearchEntryResolver.ResolvedEntry resolved = PersistedResearchEntryResolver.resolveEntry(
            "RandomThings:fertilizedDirt_tilled",
            0,
            "",
            null);

        assertNotNull(resolved);
        assertEquals("RandomThings:fertilizedDirt", resolved.key().getItemId());
    }
}
