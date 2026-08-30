package dev.gtnhjourney;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class Release116ContractTest {

    @Test
    public void release116DocumentsSaveEnabledStagingRegression() throws IOException {
        Path path = Paths.get("docs/v1.1.6-live-test.md");
        assertTrue(Files.isRegularFile(path), "missing v1.1.6 live-test checklist");
        if (!Files.isRegularFile(path)) return;

        String document = read(path.toString()).toLowerCase();
        assertTrue(document.contains("saving"));
        assertTrue(document.contains("backup"));
        assertTrue(document.contains("staging"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
