package dev.gtnhjourney.nei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import codechicken.nei.Button;

/** Small native-looking dropdown used by the header Group and Order selectors. */
final class JourneySortDropdown {

    enum Kind { GROUP, ORDER }
    private static final int OPTION_WIDTH = 108;
    private static final int OPTION_HEIGHT = 16;
    private static final int OPTION_GAP = 1;
    private final Kind kind;
    private final Button mainButton;
    private final List<OptionButton> options = new ArrayList<OptionButton>();
    private boolean open;
    private JourneyViewState.Mode openedForView;

    JourneySortDropdown(final Kind kind) {
        if (kind == null) throw new IllegalArgumentException("kind must not be null");
        this.kind = kind;
        this.mainButton = new Button("") {
            @Override public boolean onButtonPress(boolean rightclick) {
                open = !open;
                openedForView = JourneyViewState.mode();
                rebuildOptions();
                return true;
            }
            @Override public String getRenderLabel() { return activeAbbreviation(); }
            @Override public String getButtonTip() { return mainTooltip(); }
        };
    }

    void place(JourneyHeaderLayout.Slot slot) {
        if (slot == null) return;
        mainButton.x = slot.x; mainButton.y = slot.y; mainButton.w = slot.w; mainButton.h = slot.h;
        rebuildOptionGeometry();
    }
    void drawMain(int mousex, int mousey) { closeIfViewChanged(); mainButton.draw(mousex, mousey); }
    void drawOverlay(int mousex, int mousey) { closeIfViewChanged(); if (open) for (OptionButton option : options) option.draw(mousex, mousey); }
    boolean click(int mousex, int mousey, int mouseButton) {
        if (open) {
            for (OptionButton option : options) {
                if (option.contains(mousex, mousey)) { option.handleClick(mousex, mousey, mouseButton); return true; }
            }
            if (containsPopup(mousex, mousey)) return true;
        }
        if (mainButton.contains(mousex, mousey)) { mainButton.handleClick(mousex, mousey, mouseButton); return true; }
        if (open) close();
        return false;
    }
    void tooltip(int mousex, int mousey, List<String> currenttip) {
        mainButton.handleTooltip(mousex, mousey, currenttip);
        if (open) for (OptionButton option : options) option.handleTooltip(mousex, mousey, currenttip);
    }
    boolean isOpen() { return open; }
    boolean containsOpenPopup(int mousex, int mousey) { return containsPopup(mousex, mousey); }
    void close() { open = false; options.clear(); }
    List<String> optionLabelsForTests() {
        rebuildOptions();
        List<String> labels = new ArrayList<String>();
        for (OptionButton option : options) labels.add(option.labelText);
        return Collections.unmodifiableList(labels);
    }
    private void closeIfViewChanged() { if (open && openedForView != JourneyViewState.mode()) close(); }
    private boolean containsPopup(int mousex, int mousey) {
        if (!open || options.isEmpty()) return false;
        OptionButton first = options.get(0); OptionButton last = options.get(options.size() - 1);
        return mousex >= first.x && mousex < first.x + OPTION_WIDTH && mousey >= first.y && mousey < last.y + last.h;
    }
    private String activeAbbreviation() {
        JourneyViewState.Mode view = JourneyViewState.mode();
        return kind == Kind.GROUP ? JourneySortState.group(view).abbreviation() : JourneySortState.order(view).abbreviation();
    }
    private String mainTooltip() {
        JourneyViewState.Mode view = JourneyViewState.mode();
        if (kind == Kind.GROUP) return "Group: " + JourneySortState.group(view).label() + ". Click to choose None/N/M/T/K.";
        String extra = view == JourneyViewState.Mode.FAVOURITE ? "/F+" : "";
        return "Order: " + JourneySortState.order(view).label() + ". Click to choose None/U/I/A" + extra + ".";
    }
    private void rebuildOptions() {
        options.clear();
        JourneyViewState.Mode view = JourneyViewState.mode();
        if (kind == Kind.GROUP) {
            for (JourneyGroupMode value : JourneyGroupMode.values()) options.add(new OptionButton(value.abbreviation() + "  " + value.label(), value, null));
        } else {
            for (JourneyOrderMode value : JourneyOrderMode.values()) {
                if (value == JourneyOrderMode.FAVOURITE_ADDED && view != JourneyViewState.Mode.FAVOURITE) continue;
                options.add(new OptionButton(value.abbreviation() + "  " + value.label(), null, value));
            }
        }
        rebuildOptionGeometry();
    }
    private void rebuildOptionGeometry() {
        int y = mainButton.y + mainButton.h + OPTION_GAP;
        for (OptionButton option : options) {
            option.x = mainButton.x; option.y = y; option.w = OPTION_WIDTH; option.h = OPTION_HEIGHT; option.z = 300;
            y += OPTION_HEIGHT + OPTION_GAP;
        }
    }
    private final class OptionButton extends Button {
        final String labelText;
        final JourneyGroupMode groupValue;
        final JourneyOrderMode orderValue;
        OptionButton(String labelText, JourneyGroupMode groupValue, JourneyOrderMode orderValue) {
            super(labelText); this.labelText = labelText; this.groupValue = groupValue; this.orderValue = orderValue;
        }
        @Override public boolean onButtonPress(boolean rightclick) {
            JourneyViewState.Mode view = JourneyViewState.mode();
            if (kind == Kind.GROUP && groupValue != null) {
                JourneyGroupMode current = JourneySortState.group(view);
                JourneySortState.setGroup(view, current == groupValue && groupValue != JourneyGroupMode.NONE ? JourneyGroupMode.NONE : groupValue);
            } else if (kind == Kind.ORDER && orderValue != null) {
                JourneyOrderMode current = JourneySortState.order(view);
                JourneySortState.setOrder(view, current == orderValue && orderValue != JourneyOrderMode.NONE ? JourneyOrderMode.NONE : orderValue);
            }
            close();
            return true;
        }
        @Override public String getButtonTip() { return labelText; }
    }
}
