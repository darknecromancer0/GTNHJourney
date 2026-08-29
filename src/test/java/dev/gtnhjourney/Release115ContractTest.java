package dev.gtnhjourney;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class Release115ContractTest {

    @Test
    public void runtimeMetadataBuildAndReadmeAgreeOn115() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/GTNHJourney.java");
        String mcmod = read("src/main/resources/mcmod.info");
        String build = read("build.gradle.kts");
        String readme = read("README.md");

        assertTrue(source.contains("public static final String VERSION = \"1.1.5\";"));
        assertTrue(mcmod.contains("\"version\": \"1.1.5\""));
        assertTrue(build.contains("version = \"1.1.5\""));
        assertTrue(readme.contains("Current release: `1.1.5`."));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
