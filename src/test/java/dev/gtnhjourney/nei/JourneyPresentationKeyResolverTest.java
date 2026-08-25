package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class JourneyPresentationKeyResolverTest {

    @AfterEach
    public void cleanup() {
        JourneyPresentationKeyResolver.clear();
    }

    @Test
    public void registeredPresentationResolvesToAuthoritativeKeyUntilCleared() {
        ItemStack display = new ItemStack(new Item(), 1, 0);
        ResearchKey key = new ResearchKey("test:presentation", 0, "");

        JourneyPresentationKeyResolver.register(display, key);

        assertTrue(JourneyPresentationKeyResolver.isPresentation(display));
        assertEquals(key, JourneyPresentationKeyResolver.keyOf(display));

        JourneyPresentationKeyResolver.clear();
        assertFalse(JourneyPresentationKeyResolver.isPresentation(display));
    }

    @Test
    public void safePresentationAlwaysReturnsDefensiveCopy() {
        ItemStack authoritative = new ItemStack(new Item(), 3, 0);

        ItemStack display = JourneyPresentationSafety.forNei(authoritative);

        assertNotSame(authoritative, display);
        assertEquals(3, authoritative.stackSize);
        assertEquals(1, display.stackSize);
    }
}
