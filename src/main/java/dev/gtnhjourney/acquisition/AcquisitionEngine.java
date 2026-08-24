package dev.gtnhjourney.acquisition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.gtnhjourney.research.ResearchKey;
import dev.gtnhjourney.research.ResearchRegistry;

/** Applies trustworthy observed inventory keys to one player's research registry. */
public final class AcquisitionEngine {

    private AcquisitionEngine() {}

    public static List<ResearchKey> unlockPresent(ResearchRegistry registry, Iterable<ResearchKey> observedKeys) {
        if (registry == null) throw new IllegalArgumentException("registry must not be null");
        if (observedKeys == null) return Collections.emptyList();

        List<ResearchKey> newlyUnlocked = new ArrayList<ResearchKey>();
        for (ResearchKey key : observedKeys) {
            if (key != null && registry.unlock(key)) newlyUnlocked.add(key);
        }
        Collections.sort(newlyUnlocked);
        return Collections.unmodifiableList(newlyUnlocked);
    }
}
