package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TconGeneratedDisplayNamePolicyTest {

    @Test
    public void generatedShovelNameCannotRemainHatchet() {
        assertEquals(
            "§fSteel Shovel",
            TconToolStatePolicy.normalizeGeneratedDisplayName("TConstruct:shovel", "§fSteel Hatchet"));
    }

    @Test
    public void correctGeneratedAndCustomNamesRemainUntouched() {
        assertEquals(
            "§fSteel Shovel",
            TconToolStatePolicy.normalizeGeneratedDisplayName("TConstruct:shovel", "§fSteel Shovel"));
        assertEquals(
            "Excavator 9000",
            TconToolStatePolicy.normalizeGeneratedDisplayName("TConstruct:shovel", "Excavator 9000"));
        assertEquals(
            "§fSteel Hatchet",
            TconToolStatePolicy.normalizeGeneratedDisplayName("TConstruct:hatchet", "§fSteel Hatchet"));
    }

    @Test
    public void duplicateWhiteFormattingIsStillCollapsed() {
        assertEquals(
            "§fSteel Shovel",
            TconToolStatePolicy.normalizeGeneratedDisplayName("TConstruct:shovel", "§f§fSteel Shovel"));
    }
}
