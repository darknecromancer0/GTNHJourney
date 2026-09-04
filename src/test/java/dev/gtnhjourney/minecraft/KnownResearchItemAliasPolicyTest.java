package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class KnownResearchItemAliasPolicyTest {

    @Test
    public void randomThingsTilledFertilizedDirtCanonicalizesToUsableBlock() {
        assertEquals(
            "RandomThings:fertilizedDirt",
            KnownResearchItemAliasPolicy.canonicalItemId("RandomThings:fertilizedDirt_tilled"));
    }

    @Test
    public void similarlyNamedForeignItemsRemainUntouched() {
        assertEquals(
            "example:fertilizedDirt_tilled",
            KnownResearchItemAliasPolicy.canonicalItemId("example:fertilizedDirt_tilled"));
    }
}
