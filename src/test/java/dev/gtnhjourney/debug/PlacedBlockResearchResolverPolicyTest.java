package dev.gtnhjourney.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class PlacedBlockResearchResolverPolicyTest {

    @Test
    public void pickRepresentationWinsWhenValid() {
        final Item pickItem = new Item();
        final Item fallbackItem = new Item();
        ItemStack resolved = PlacedBlockResearchResolver.resolve(
            new PlacedBlockResearchResolver.StackStrategy() {

                @Override
                public ItemStack resolve() {
                    return new ItemStack(pickItem, 1, 3);
                }
            },
            new PlacedBlockResearchResolver.StackStrategy() {

                @Override
                public ItemStack resolve() {
                    return new ItemStack(fallbackItem, 1, 4);
                }
            });

        assertEquals(pickItem, resolved.getItem());
        assertEquals(3, resolved.getItemDamage());
    }

    @Test
    public void invalidOrBrokenPickFallsBackSafely() {
        final Item fallbackItem = new Item();
        ItemStack resolved = PlacedBlockResearchResolver.resolve(
            new PlacedBlockResearchResolver.StackStrategy() {

                @Override
                public ItemStack resolve() {
                    throw new IllegalStateException("broken mod pick hook");
                }
            },
            new PlacedBlockResearchResolver.StackStrategy() {

                @Override
                public ItemStack resolve() {
                    return new ItemStack(fallbackItem, 1, 7);
                }
            });

        assertEquals(fallbackItem, resolved.getItem());
        assertEquals(7, resolved.getItemDamage());
    }

    @Test
    public void bothFailuresReturnNull() {
        ItemStack resolved = PlacedBlockResearchResolver.resolve(
            new PlacedBlockResearchResolver.StackStrategy() {

                @Override
                public ItemStack resolve() {
                    return null;
                }
            },
            new PlacedBlockResearchResolver.StackStrategy() {

                @Override
                public ItemStack resolve() {
                    throw new LinkageError("optional mod disappeared");
                }
            });

        assertNull(resolved);
    }
}
