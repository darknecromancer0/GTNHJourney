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
    void recipeViewGuardsPhysicalBackStateInsteadOfOnlyCurrentBackspaceEvent() throws IOException {
        String mixin = read("src/main/java/dev/gtnhjourney/mixin/GuiRecipeCtrlBackspaceMixin.java");
        String config = read("src/main/resources/mixins.gtnhjourney.json");

        assertTrue(mixin.contains("GuiRecipe"));
        assertTrue(mixin.contains("method = \"keyTyped\""));
        assertTrue(mixin.contains("KeyManager;isKeyDown"));
        assertTrue(mixin.contains("KeyManager.isKeyDown(\"recipe.back\")"));
        assertTrue(mixin.contains("LayoutManager.searchField.focused()"));
        assertTrue(mixin.contains("NEIClientUtils.controlKey()"));
        assertTrue(mixin.contains("JourneyRecipeBackGuard.shouldSuppress"));
        assertTrue(mixin.contains("ci.cancel()"));
        assertTrue(config.contains("GuiRecipeCtrlBackspaceMixin"));
    }

    @Test
    void recipeViewRemapsVanillaOverrideButNotNeiCallsiteAndTargetsRecipeBack() throws IOException {
        String mixin = compactWhitespace(
            read("src/main/java/dev/gtnhjourney/mixin/GuiRecipeCtrlBackspaceMixin.java"));

        // GuiRecipe.keyTyped overrides a vanilla GuiScreen method and therefore changes name in the production reobf jar.
        assertTrue(mixin.contains("method=\"keyTyped\""));
        assertTrue(mixin.contains("cancellable=true,remap=true"));

        // KeyManager is NEI-owned, so its invocation descriptor must remain literal while the enclosing method is remapped.
        assertTrue(mixin.contains("target=\"Lcodechicken/nei/KeyManager;isKeyDown(Ljava/lang/String;)Z\",ordinal=2,remap=false"));
    }

    private static String compactWhitespace(String value) {
        return value.replaceAll("\\s+", "");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
