package dev.gtnhjourney.nei;

/** Pure policy for keeping NEI recipe navigation from stealing Backspace while search owns the keyboard. */
public final class JourneyRecipeBackGuard {

    private JourneyRecipeBackGuard() {}

    public static boolean shouldSuppress(boolean recipeBackDown, boolean controlDown, boolean mainSearchFocused) {
        return recipeBackDown && (controlDown || mainSearchFocused);
    }
}
