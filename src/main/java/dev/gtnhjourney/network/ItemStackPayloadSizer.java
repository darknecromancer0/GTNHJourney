package dev.gtnhjourney.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;

/** Measures the actual Forge 1.7.10 ItemStack wire payload instead of guessing from textual NBT. */
public final class ItemStackPayloadSizer {
    private ItemStackPayloadSizer() {}

    public static boolean canSync(ItemStack stack) {
        int bytes = serializedBytes(stack);
        return bytes >= 0 && bytes <= ResearchSyncBudget.MAX_SINGLE_ENTRY_BYTES;
    }

    /** Returns -1 when a modded stack cannot be serialized safely. */
    public static int serializedBytes(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return -1;
        ByteBuf buffer = Unpooled.buffer();
        try {
            ByteBufUtils.writeItemStack(buffer, stack);
            return buffer.readableBytes();
        } catch (RuntimeException failure) {
            return -1;
        } catch (LinkageError failure) {
            return -1;
        } finally {
            try { buffer.release(); }
            catch (RuntimeException ignored) {}
        }
    }
}
