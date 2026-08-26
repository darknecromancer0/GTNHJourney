package dev.gtnhjourney.recovery;

import net.minecraft.item.ItemStack;

import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.retrieval.ItemStackTemplateFactory;

/** Strict runtime recovery policy: persisted key/template pairs must still recreate the same current-pack item state. */
public enum RuntimeRecoveryRestorePolicy implements RecoveryRestorePolicy {
    INSTANCE;

    @Override
    public boolean canRestore(ResearchEntrySnapshot entry) {
        if (entry == null || entry.key() == null) return false;
        try {
            ItemStack rebuilt = ItemStackTemplateFactory.create(entry.key(), entry.template(), 1);
            return rebuilt != null && rebuilt.getItem() != null && entry.key().equals(ItemStackKeyFactory.from(rebuilt));
        } catch (IllegalArgumentException failure) {
            return false;
        } catch (RuntimeException failure) {
            return false;
        } catch (LinkageError failure) {
            return false;
        }
    }
}
