package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class NeiSearchCtrlBackspaceContractTest {

    @Test
    void focusedNeiSearchConsumesCtrlBackspaceBeforeGlobalKeyDispatch() throws IOException {
        String mixin = read("src/main/java/dev/gtnhjourney/mixin/GuiContainerSearchCtrlBackspaceMixin.java");
        String config = read("src/main/resources/mixins.gtnhjourney.json");

        assertTrue(mixin.contains("GuiContainerManager"));
        assertTrue(mixin.contains("handleKeyboardInput"));
        assertTrue(mixin.contains("Keyboard.KEY_BACK"));
        assertTrue(mixin.contains("LayoutManager.searchField.focused()"));
        assertTrue(mixin.contains("manager.keyTyped"));
        assertTrue(mixin.contains("ci.cancel()"));
        assertTrue(config.contains("GuiContainerSearchCtrlBackspaceMixin"));
    }

    @Test
    void recipeViewDelegatesPhysicalBackStateToGuardPolicy() throws IOException {
        String mixin = compactWhitespace(
            read("src/main/java/dev/gtnhjourney/mixin/GuiRecipeCtrlBackspaceMixin.java"));
        String config = read("src/main/resources/mixins.gtnhjourney.json");

        assertTrue(mixin.contains("@Mixin(value=GuiRecipe.class,remap=false)"), "must target GuiRecipe");
        assertTrue(mixin.contains("method=\"keyTyped\""), "must intercept GuiRecipe.keyTyped");
        assertTrue(
            mixin.contains(
                "JourneyRecipeBackGuard.shouldSuppress(KeyManager.isKeyDown(\"recipe.back\"),NEIClientUtils.controlKey(),mainSearchFocused)"),
            "guard must use physical recipe.back state, Ctrl state, and main-search focus");
        assertTrue(mixin.contains("ci.cancel();"), "guarded path must consume navigation");
        assertTrue(config.contains("GuiRecipeCtrlBackspaceMixin"), "mixin must remain registered");
    }

    @Test
    void recipeViewRemapsVanillaOverrideButNotNeiCallsiteAndGuardsBeforeRecipeBack() throws IOException {
        String mixin = compactWhitespace(
            read("src/main/java/dev/gtnhjourney/mixin/GuiRecipeCtrlBackspaceMixin.java"));

        // GuiRecipe.keyTyped overrides a vanilla GuiScreen method and therefore changes name in the production reobf jar.
        assertTrue(mixin.contains("method=\"keyTyped\""));
        assertTrue(mixin.contains("cancellable=true,remap=true"));

        // KeyManager is NEI-owned, so its invocation descriptor stays literal. The first invocation in GuiRecipe.keyTyped
        // is recipe.back, so the guard must inject at ordinal 0 before navigation can change the active screen.
        assertTrue(mixin.contains("target=\"Lcodechicken/nei/KeyManager;isKeyDown(Ljava/lang/String;)Z\",ordinal=0,remap=false"));
    }

    private static String compactWhitespace(String value) {
        return value.replaceAll("\\s+", "");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
