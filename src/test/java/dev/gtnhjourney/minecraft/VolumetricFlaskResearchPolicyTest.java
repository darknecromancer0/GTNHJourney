package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VolumetricFlaskResearchPolicyTest {

    @Test
    void allPositiveFlaskQuantitiesProveOneLitreWhenSupported() {
        assertEquals(1000, VolumetricFlaskResearchPolicy.standardAmount(1, 1000));
        assertEquals(1000, VolumetricFlaskResearchPolicy.standardAmount(144, 1000));
        assertEquals(1000, VolumetricFlaskResearchPolicy.standardAmount(500, 16000));
        assertEquals(1000, VolumetricFlaskResearchPolicy.standardAmount(1000, 256000));
    }

    @Test
    void emptyOrTooSmallContainerDoesNotUnlockUnrepresentableLitre() {
        assertEquals(0, VolumetricFlaskResearchPolicy.standardAmount(0, 1000));
        assertEquals(0, VolumetricFlaskResearchPolicy.standardAmount(-1, 1000));
        assertEquals(0, VolumetricFlaskResearchPolicy.standardAmount(144, 999));
    }
}
