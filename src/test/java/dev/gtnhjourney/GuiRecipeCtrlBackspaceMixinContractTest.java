package dev.gtnhjourney;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class GuiRecipeCtrlBackspaceMixinContractTest {

    @Test
    void guardRunsBeforeNeiRecipeBackCheck() throws IOException {
        String source = compactWhitespace(read("src/main/java/dev/gtnhjourney/mixin/GuiRecipeCtrlBackspaceMixin.java"));

        assertTrue(source.contains("target=\"Lcodechicken/nei/KeyManager;isKeyDown(Ljava/lang/String;)Z\",ordinal=0"),
            "Ctrl+Backspace guard must inject before GuiRecipe's first KeyManager.isKeyDown call, which is recipe.back");
    }

    private static String compactWhitespace(String value) {
        return value.replaceAll("\\s+", "");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
