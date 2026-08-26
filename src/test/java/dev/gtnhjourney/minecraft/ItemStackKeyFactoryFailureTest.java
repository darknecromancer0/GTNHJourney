package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItemStackKeyFactoryFailureTest {

    @Test
    public void unexpectedModdedStackRuntimeIsConvertedToSafeIdentityFailure() {
        ItemStack broken = new ItemStack(Items.stick, 1, 0);
        broken.setTagCompound(new BrokenCopyTag(new IllegalStateException("broken modded stack NBT")));

        IllegalArgumentException failure = assertThrows(
            IllegalArgumentException.class,
            () -> ItemStackKeyFactory.from(broken));

        assertTrue(failure.getCause() instanceof IllegalStateException);
    }

    private static final class BrokenCopyTag extends NBTTagCompound {

        private final RuntimeException failure;

        private BrokenCopyTag(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public NBTTagCompound copy() {
            throw failure;
        }
    }
}
