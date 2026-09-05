package dev.gtnhjourney.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.client.event.GuiScreenEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Draws server-authoritative slash-command suggestions above the 1.7.10 chat input. */
public final class JourneyCommandHintOverlay {

    private static final int MAX_VISIBLE = 10;

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiChat)) {
            ClientCommandSuggestionState.clear();
            return;
        }
        GuiTextField chatInput = ChatInputResolver.resolve((GuiChat) event.gui);
        if (chatInput == null) {
            ClientCommandSuggestionState.clear();
            return;
        }

        ClientCommandSuggestionState.requestForInput(chatInput);
        List<String> suggestions = ClientCommandSuggestionState.snapshot();
        if (suggestions.isEmpty()) return;

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.fontRenderer == null) return;
        FontRenderer font = minecraft.fontRenderer;
        int selected = Math.max(0, ClientCommandSuggestionState.selectedIndex());
        int start = visibleWindowStart(suggestions.size(), selected, MAX_VISIBLE);
        int end = Math.min(suggestions.size(), start + MAX_VISIBLE);
        int shown = Math.max(0, end - start);
        boolean clippedAbove = start > 0;
        boolean clippedBelow = end < suggestions.size();
        int metaLines = (clippedAbove ? 1 : 0) + (clippedBelow ? 1 : 0);
        int lineHeight = font.FONT_HEIGHT + 2;
        int lines = shown + metaLines;
        int maxWidth = 0;
        for (int i = start; i < end; i++) maxWidth = Math.max(maxWidth, font.getStringWidth(suggestions.get(i)));
        if (clippedAbove) maxWidth = Math.max(maxWidth, font.getStringWidth("^ " + start + " more"));
        if (clippedBelow) maxWidth = Math.max(maxWidth, font.getStringWidth("v " + (suggestions.size() - end) + " more"));

        int left = 3;
        int bottom = event.gui.height - 17;
        int top = Math.max(3, bottom - lines * lineHeight - 4);
        Gui.drawRect(left - 2, top - 2, left + maxWidth + 5, bottom, 0xA0000000);
        int y = top;
        if (clippedAbove) {
            font.drawStringWithShadow("^ " + start + " more", left, y, 0xFF909090);
            y += lineHeight;
        }
        for (int i = start; i < end; i++) {
            if (i == selected) Gui.drawRect(left - 1, y - 1, left + maxWidth + 4, y + font.FONT_HEIGHT + 1, 0x80606060);
            font.drawStringWithShadow(suggestions.get(i), left, y, i == selected ? 0xFFFFFFFF : 0xFFE0E0E0);
            y += lineHeight;
        }
        if (clippedBelow) font.drawStringWithShadow("v " + (suggestions.size() - end) + " more", left, y, 0xFF909090);
    }

    static int visibleWindowStart(int total, int selected, int maxVisible) {
        if (total <= 0 || maxVisible <= 0 || total <= maxVisible) return 0;
        int safeSelected = Math.max(0, Math.min(selected, total - 1));
        int start = safeSelected - maxVisible / 2;
        if (start < 0) start = 0;
        int maxStart = total - maxVisible;
        if (start > maxStart) start = maxStart;
        return start;
    }
}
