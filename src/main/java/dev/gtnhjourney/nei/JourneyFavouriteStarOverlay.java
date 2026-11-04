package dev.gtnhjourney.nei;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.renderer.Tessellator;
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

    private static final int GOLD = 0xFFC43C;
    private static final int SHADOW = 0x100A04;

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

        List<Rectangle4i> marked = new ArrayList<Rectangle4i>();
        for (ItemsPanelGridSlot slot : slots) {
            if (slot == null || slot.getItemStack() == null || !isFavourite(slot.getItemStack())) continue;
            Rectangle4i rect = grid.getSlotRect(slot.slotIndex);
            if (rect != null && rect.w >= 10 && rect.h >= 10) marked.add(rect);
        }
        if (marked.isEmpty()) return;

        // One GL batch for every visible star instead of many thousand GUI rectangle draw calls per frame.
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            for (Rectangle4i rect : marked) {
                int x = JourneyFavouriteStarShape.markerX(rect.x, rect.w);
                int y = JourneyFavouriteStarShape.markerY(rect.y);
                appendPixels(tessellator, x + 1, y + 1, SHADOW, 220);
                appendPixels(tessellator, x, y, GOLD, 255);
            }
            tessellator.draw();
        } finally {
            GL11.glPopAttrib();
        }
    }

    private static void appendPixels(Tessellator tessellator, int x, int y, int rgb, int alpha) {
        for (int row = 0; row < JourneyFavouriteStarShape.SIZE; row++) {
            for (int col = 0; col < JourneyFavouriteStarShape.SIZE; col++) {
                if (!JourneyFavouriteStarShape.isPixel(col, row)) continue;
                tessellator.setColorRGBA_I(rgb, alpha);
                double left = x + col;
                double top = y + row;
                tessellator.addVertex(left, top + 1, 0);
                tessellator.addVertex(left + 1, top + 1, 0);
                tessellator.addVertex(left + 1, top, 0);
                tessellator.addVertex(left, top, 0);
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
        // Bounded even if someone flips through thousands of native NEI pages without rebuilding the panel.
        if (membership.size() >= 4096) membership.clear();
        membership.put(stack, Boolean.valueOf(favourite));
        return favourite;
    }

    static boolean matchesFavourite(ResearchKey key) {
        return key != null && ClientFavouriteMirror.contains(ResearchFingerprint.of(key));
    }
}
