package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class JourneyNativeFilterInvalidationContractTest {

    @Test
    public void journeyObservesOnlyNeisCanonicalUpdateFilterTask() throws IOException {
        String mixin = read("src/main/java/dev/gtnhjourney/mixin/RestartableTaskJourneyFilterMixin.java");
        String config = read("src/main/resources/mixins.gtnhjourney.json");

        assertTrue(mixin.contains("@Mixin(value = RestartableTask.class"));
        assertTrue(mixin.contains("@Inject(method = \"restart\""));
        assertTrue(mixin.contains("(Object) this == ItemList.updateFilter"));
        assertTrue(mixin.contains("JourneyNeiFilterRevision.invalidate()"));
        assertTrue(config.contains("\"RestartableTaskJourneyFilterMixin\""));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
