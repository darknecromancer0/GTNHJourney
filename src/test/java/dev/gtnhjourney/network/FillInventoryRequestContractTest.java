package dev.gtnhjourney.network;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class FillInventoryRequestContractTest {

    @Test
    public void shiftRightClickUsesOneDedicatedServerRequest() throws IOException {
        String input = read("src/main/java/dev/gtnhjourney/nei/JourneyNEIInputHandler.java");
        String network = read("src/main/java/dev/gtnhjourney/network/JourneyNetwork.java");
        String queue = read("src/main/java/dev/gtnhjourney/network/ServerRequestQueue.java");

        assertTrue(input.contains("JourneyRetrieveClickPolicy.shouldFillInventory(button, shiftDown())"));
        assertTrue(input.contains("JourneyNetwork.requestFillInventory(key)"));
        assertTrue(network.contains("FillInventoryRequestMessage.Handler.class"));
        assertTrue(network.contains("requestFillInventory(ResearchKey key)"));
        assertTrue(queue.contains("FILL_INVENTORY"));
        assertTrue(queue.contains("MainInventoryFillService.fillEmptyMainSlots"));
    }

    private static String read(String path) throws IOException {
        Path file = Paths.get(path);
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }
}
