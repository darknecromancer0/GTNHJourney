package dev.gtnhjourney.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import org.junit.jupiter.api.Test;

import ic2.core.IC2Potion;

class PlayerCleanseServiceTest {

    @Test
    void removesNegativeEffectsAndPreservesPositiveEffects() {
        List<PotionEffect> effects = Arrays.asList(
            new PotionEffect(Potion.poison.id, 200, 0),
            new PotionEffect(Potion.moveSlowdown.id, 200, 0),
            new PotionEffect(Potion.moveSpeed.id, 200, 0),
            new PotionEffect(Potion.regeneration.id, 200, 0));
        List<Integer> removed = new ArrayList<Integer>();

        int count = PlayerCleanseService.cleanseEffects(effects, removed::add);

        assertEquals(2, count);
        assertTrue(removed.contains(Potion.poison.id));
        assertTrue(removed.contains(Potion.moveSlowdown.id));
        assertFalse(removed.contains(Potion.moveSpeed.id));
        assertFalse(removed.contains(Potion.regeneration.id));
    }

    @Test
    void ic2RadiationIsRecognizedAsNegative() {
        assertTrue(IC2Potion.radiation.isBadEffect());
        assertTrue(PlayerCleanseService.isNegativePotionEffect(new PotionEffect(IC2Potion.radiation.id, 200, 0)));
    }

    @Test
    void malformedPotionIdsAreIgnoredSafely() {
        PotionEffect malformed = new PotionEffect(Potion.potionTypes.length + 100, 200, 0);

        assertFalse(PlayerCleanseService.isNegativePotionEffect(malformed));
    }
}
