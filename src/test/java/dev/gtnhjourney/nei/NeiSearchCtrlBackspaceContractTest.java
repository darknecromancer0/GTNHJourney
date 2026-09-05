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
    void recipeViewCannotTreatCtrlBackspaceAsPlainRecipeBack() throws IOException {
        String mixin = read("src/main/java/dev/gtnhjourney/mixin/GuiRecipeCtrlBackspaceMixin.java");
        String config = read("src/main/resources/mixins.gtnhjourney.json");

        assertTrue(mixin.contains("GuiRecipe"));
        assertTrue(mixin.contains("method = \"keyTyped\""));
        assertTrue(mixin.contains("KeyManager;isKeyDown"));
        assertTrue(mixin.contains("Keyboard.KEY_BACK"));
        assertTrue(mixin.contains("NEIClientUtils.controlKey()"));
        assertTrue(mixin.contains("ci.cancel()"));
        assertTrue(config.contains("GuiRecipeCtrlBackspaceMixin"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
