package dev.gtnhjourney.nei;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.Gui;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import codechicken.lib.vec.Rectangle4i;
import codechicken.nei.ItemList;
import codechicken.nei.ItemsPanelGrid;
import codechicken.nei.ItemsPanelGrid.ItemsPanelGridSlot;
import dev.gtnhjourney.client.ClientFavouriteMirror;
import dev.gtnhjourney.research.ResearchFingerprint;
import dev.gtnhjourney.research.ResearchKey;

/**
 * Gold markers drawn after NEI's cached item grid, so favourite changes show up immediately without rebuilding its
 * framebuffer. Does not change item NBT, position, tooltip, click handling or native NEI bookmark decorations.
 */
public final class JourneyFavouriteStarOverlay {

    private static final int GOLD = 0xFFFFC43C;
    private static final int SHADOW = 0xE0100A04;

    private static final Map<ItemStack, Boolean> membership = new IdentityHashMap<ItemStack, Boolean>();
    private static List<ItemStack> observedPanel;
    private static long observedFavouriteRevision = -1L;

    private JourneyFavouriteStarOverlay() {}

    public static void render(ItemsPanelGrid grid) {
        if (grid == null || !ItemList.loadFinished || ClientFavouriteMirror.size() == 0) return;
        if (observedPanel != grid.getItems() || observedFavouriteRevision != ClientFavouriteMirror.revision()) {
            membership.clear();
            observedPanel = grid.getItems();
            observedFavouriteRevision = ClientFavouriteMirror.revision();
        }
        List<ItemsPanelGridSlot> slots = grid.getMask();
        if (slots == null || slots.isEmpty()) return;

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_BLEND);
            for (ItemsPanelGridSlot slot : slots) {
                if (slot == null || slot.getItemStack() == null || !isFavourite(slot.getItemStack())) continue;
                Rectangle4i rect = grid.getSlotRect(slot.slotIndex);
                if (rect == null || rect.w < 10 || rect.h < 10) continue;
                int x = JourneyFavouriteStarShape.markerX(rect.x, rect.w);
                int y = JourneyFavouriteStarShape.markerY(rect.y);
                drawPixels(x + 1, y + 1, SHADOW);
                drawPixels(x, y, GOLD);
            }
        } finally {
            GL11.glPopAttrib();
        }
    }

    private static void drawPixels(int x, int y, int argb) {
        for (int row = 0; row < JourneyFavouriteStarShape.SIZE; row++) {
            for (int col = 0; col < JourneyFavouriteStarShape.SIZE; col++) {
                if (JourneyFavouriteStarShape.isPixel(col, row)) {
                    Gui.drawRect(x + col, y + row, x + col + 1, y + row + 1, argb);
                }
            }
        }
    }

    private static boolean isFavourite(ItemStack stack) {
        Boolean cached = membership.get(stack);
        if (cached != null) return cached.booleanValue();
        boolean favourite = false;
        try {
            ResearchKey key = JourneyPresentationKeyResolver.keyOf(stack);
            favourite = matchesFavourite(key);
        } catch (RuntimeException | LinkageError optionalModFailure) {
            // An incompatible optional item should not break drawing the rest of the NEI item panel.
        }
        membership.put(stack, Boolean.valueOf(favourite));
        return favourite;
    }

    static boolean matchesFavourite(ResearchKey key) {
        return key != null && ClientFavouriteMirror.contains(ResearchFingerprint.of(key));
    }
}
