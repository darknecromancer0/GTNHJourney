package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemStackKeyFactoryFailureTest {

    @Test
    public void unexpectedModdedStackRuntimeIsConvertedToSafeIdentityFailure() {
        ItemStack broken = new ItemStack(new Item(), 1, 0) {

            @Override
            public Item getItem() {
                throw new IllegalStateException("broken modded stack");
            }
        };

        IllegalArgumentException failure = assertThrows(
            IllegalArgumentException.class,
            () -> ItemStackKeyFactory.from(broken));

        assertInstanceOf(IllegalStateException.class, failure.getCause());
    }
}
