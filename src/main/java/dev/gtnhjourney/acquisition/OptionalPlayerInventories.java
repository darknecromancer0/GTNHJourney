package dev.gtnhjourney.acquisition;

import java.lang.reflect.Method;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * Optional real-player inventory adapters that do not create hard mod dependencies.
 *
 * <p>
 * Only inventories that are authoritative storage owned by the player belong here. GUI recipe outputs,
 * machine slots and arbitrary open containers are intentionally excluded.
 * </p>
 */
public final class OptionalPlayerInventories {

    public interface StackConsumer {

        void accept(ItemStack stack);
    }

    public interface SlotStackConsumer {

        void accept(String slotId, ItemStack stack);
    }

    private static volatile boolean baublesResolved;
    private static volatile Method baublesGetter;

    private OptionalPlayerInventories() {}

    public static void scan(EntityPlayerMP player, final StackConsumer consumer) {
        if (player == null || consumer == null) return;
        scanSlots(player, new SlotStackConsumer() {

            @Override
            public void accept(String slotId, ItemStack stack) {
                consumer.accept(stack);
            }
        });
    }

    public static void scanSlots(EntityPlayerMP player, SlotStackConsumer consumer) {
        if (player == null || consumer == null) return;
        scanBaubles(player, consumer);
    }

    private static void scanBaubles(EntityPlayer player, SlotStackConsumer consumer) {
        Method getter = baublesGetter();
        if (getter == null) return;
        try {
            Object value = getter.invoke(null, player);
            if (!(value instanceof IInventory)) return;
            IInventory inventory = (IInventory) value;
            int size = Math.max(0, inventory.getSizeInventory());
            for (int slot = 0; slot < size; slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack != null) consumer.accept("baubles:" + slot, stack);
            }
        } catch (ReflectiveOperationException ignored) {
            // Optional integration. A broken/missing Baubles API must never break Journey's main inventory scan.
        } catch (RuntimeException ignored) {
            // Defensive boundary around third-party inventory implementations.
        }
    }

    private static Method baublesGetter() {
        if (baublesResolved) return baublesGetter;
        synchronized (OptionalPlayerInventories.class) {
            if (baublesResolved) return baublesGetter;
            baublesResolved = true;
            try {
                Class<?> api = Class.forName("baubles.api.BaublesApi");
                baublesGetter = api.getMethod("getBaubles", EntityPlayer.class);
            } catch (ClassNotFoundException ignored) {
                baublesGetter = null;
            } catch (NoSuchMethodException ignored) {
                baublesGetter = null;
            } catch (LinkageError ignored) {
                baublesGetter = null;
            }
            return baublesGetter;
        }
    }
}
