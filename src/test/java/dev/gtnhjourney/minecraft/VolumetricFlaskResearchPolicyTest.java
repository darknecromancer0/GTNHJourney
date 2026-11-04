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
    void legacyFlaskIdentityPrefilterRecognizesAllFourTypes() {
        org.junit.jupiter.api.Assertions.assertTrue(
            VolumetricFlaskResearchPolicy.mayBeFlaskId("gregtech:gt.Volumetric_Flask"));
        org.junit.jupiter.api.Assertions.assertTrue(
            VolumetricFlaskResearchPolicy.mayBeFlaskId("miscutils:gt.Volumetric_Flask_8k"));
        org.junit.jupiter.api.Assertions.assertTrue(
            VolumetricFlaskResearchPolicy.mayBeFlaskId("miscutils:gt.Volumetric_Flask_32k"));
        org.junit.jupiter.api.Assertions.assertTrue(
            VolumetricFlaskResearchPolicy.mayBeFlaskId("miscutils:gt.Klein_Bottle"));
        org.junit.jupiter.api.Assertions.assertFalse(
            VolumetricFlaskResearchPolicy.mayBeFlaskId("gregtech:gt.metaitem.01"));
    }

    @Test
    void emptyOrTooSmallContainerDoesNotUnlockUnrepresentableLitre() {
        assertEquals(0, VolumetricFlaskResearchPolicy.standardAmount(0, 1000));
        assertEquals(0, VolumetricFlaskResearchPolicy.standardAmount(-1, 1000));
        assertEquals(0, VolumetricFlaskResearchPolicy.standardAmount(144, 999));
    }
}
