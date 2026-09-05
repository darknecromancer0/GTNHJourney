package dev.gtnhjourney.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.gui.GuiTextField;

import dev.gtnhjourney.network.Journey1124Network;

/** Client-side state for server-authoritative slash-command completions. */
public final class ClientCommandSuggestionState {

    private static final int MAX_SUGGESTIONS = 64;

    private static long nextRequestId = 1L;
    private static long pendingRequestId;
    private static String pendingPrefix = "";
    private static List<String> suggestions = Collections.emptyList();
    private static int selectedIndex;

    private ClientCommandSuggestionState() {}

    /** Request new completions only when the command prefix up to the cursor changes. */
    public static synchronized void requestForInput(GuiTextField input) {
        if (input == null) {
            clear();
            return;
        }
        String text = input.getText();
        int cursor = Math.max(0, Math.min(input.getCursorPosition(), text == null ? 0 : text.length()));
        String prefix = text == null ? "" : text.substring(0, cursor);
        if (!prefix.startsWith("/")) {
            clear();
            return;
        }
        if (prefix.equals(pendingPrefix)) return;

        pendingPrefix = prefix;
        suggestions = Collections.emptyList();
        selectedIndex = 0;
        pendingRequestId = nextRequestId++;
        Journey1124Network.requestCommandSuggestions(pendingRequestId, prefix);
    }

    public static synchronized void apply(long requestId, String prefix, List<String> values) {
        if (requestId != pendingRequestId || prefix == null || !prefix.equals(pendingPrefix)) return;
        Set<String> unique = new LinkedHashSet<String>();
        if (values != null) {
            for (String value : values) {
                if (value == null || value.isEmpty()) continue;
                unique.add(value);
                if (unique.size() >= MAX_SUGGESTIONS) break;
            }
        }
        suggestions = Collections.unmodifiableList(new ArrayList<String>(unique));
        selectedIndex = suggestions.isEmpty() ? 0 : Math.min(selectedIndex, suggestions.size() - 1);
        CommandHintDiagnostics.recordSuggestionCount(suggestions.size());
    }

    public static synchronized List<String> snapshot() {
        return suggestions;
    }

    public static synchronized int selectedIndex() {
        if (suggestions.isEmpty()) return -1;
        return Math.max(0, Math.min(selectedIndex, suggestions.size() - 1));
    }

    public static synchronized boolean hasSuggestions() {
        return !suggestions.isEmpty();
    }

    public static synchronized boolean moveSelection(int delta) {
        if (suggestions.isEmpty() || delta == 0) return false;
        int size = suggestions.size();
        selectedIndex = (selectedIndex + delta) % size;
        if (selectedIndex < 0) selectedIndex += size;
        return true;
    }

    /** Replace only the active whitespace-delimited command token and preserve text after the cursor. */
    public static synchronized boolean acceptSelected(GuiTextField input) {
        if (input == null || suggestions.isEmpty()) return false;
        int index = selectedIndex();
        if (index < 0) return false;
        String replacement = suggestions.get(index);
        String text = input.getText();
        if (text == null) text = "";
        int cursor = Math.max(0, Math.min(input.getCursorPosition(), text.length()));
        int tokenStart = cursor;
        while (tokenStart > 0 && !Character.isWhitespace(text.charAt(tokenStart - 1))) tokenStart--;
        if (tokenStart == 0 && text.startsWith("/")) tokenStart = 1;

        String updated = text.substring(0, tokenStart) + replacement + text.substring(cursor);
        int newCursor = tokenStart + replacement.length();
        input.setText(updated);
        input.setCursorPosition(newCursor);

        pendingPrefix = "";
        suggestions = Collections.emptyList();
        selectedIndex = 0;
        return true;
    }

    public static synchronized void clear() {
        pendingRequestId = 0L;
        pendingPrefix = "";
        suggestions = Collections.emptyList();
        selectedIndex = 0;
        CommandHintDiagnostics.recordSuggestionCount(0);
    }
}
