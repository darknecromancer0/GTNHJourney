package dev.gtnhjourney.safety;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.IntConsumer;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

/** Removes only currently active harmful potion effects from one player. */
public final class PlayerCleanseService {

    public int cleanse(final EntityPlayerMP player) {
        if (player == null) return 0;
        Collection<?> active = player.getActivePotionEffects();
        if (active == null || active.isEmpty()) return 0;

        List<PotionEffect> snapshot = new ArrayList<PotionEffect>(active.size());
        for (Object value : active) {
            if (value instanceof PotionEffect) snapshot.add((PotionEffect) value);
        }
        return cleanseEffects(snapshot, new IntConsumer() {

            @Override
            public void accept(int potionId) {
                player.removePotionEffect(potionId);
            }
        });
    }

    static int cleanseEffects(Collection<PotionEffect> effects, IntConsumer remover) {
        if (effects == null || effects.isEmpty() || remover == null) return 0;
        int removed = 0;
        List<PotionEffect> snapshot = new ArrayList<PotionEffect>(effects);
        for (PotionEffect effect : snapshot) {
            if (!isNegativePotionEffect(effect)) continue;
            remover.accept(effect.getPotionID());
            removed++;
        }
        return removed;
    }

    static boolean isNegativePotionEffect(PotionEffect effect) {
        if (effect == null) return false;
        int id = effect.getPotionID();
        if (id < 0 || id >= Potion.potionTypes.length) return false;
        Potion potion = Potion.potionTypes[id];
        return potion != null && potion.isBadEffect();
    }
}
