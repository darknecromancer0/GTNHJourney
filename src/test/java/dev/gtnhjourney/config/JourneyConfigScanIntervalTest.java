package dev.gtnhjourney.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileWriter;

import org.junit.jupiter.api.Test;

public class JourneyConfigScanIntervalTest {

    @Test
    public void newConfigDefaultsToFiveTicks() throws Exception {
        File file = File.createTempFile("gtnhjourney-config-default", ".cfg");
        if (!file.delete()) throw new IllegalStateException("could not prepare temp config path");
        try {
            JourneyConfig.load(file);
            assertEquals(5, JourneyConfig.inventoryScanIntervalTicks());
        } finally {
            file.delete();
        }
    }

    @Test
    public void legacyTwoTickConfigIsRaisedToFiveTicks() throws Exception {
        File file = File.createTempFile("gtnhjourney-config-legacy", ".cfg");
        try {
            FileWriter writer = new FileWriter(file);
            try {
                writer.write("research {\n    I:inventoryScanIntervalTicks=2\n}\n");
            } finally {
                writer.close();
            }
            JourneyConfig.load(file);
            assertEquals(5, JourneyConfig.inventoryScanIntervalTicks());
        } finally {
            file.delete();
        }
    }
}
