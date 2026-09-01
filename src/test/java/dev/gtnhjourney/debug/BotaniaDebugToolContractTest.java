package dev.gtnhjourney.debug;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class BotaniaDebugToolContractTest {

    @Test
    public void journeyRegistersAndGrantsDedicatedBotaniaManaTool() throws IOException {
        String mod = read("src/main/java/dev/gtnhjourney/GTNHJourney.java");
        String command = read("src/main/java/dev/gtnhjourney/command/CommandJourney.java");
        String policy = read("src/main/java/dev/gtnhjourney/acquisition/ResearchObservationPolicy.java");

        assertTrue(mod.contains("BOTANIA_MANA_DEBUG_TOOL"));
        assertTrue(mod.contains("new ItemBotaniaManaDebugTool()"));
        assertTrue(mod.contains("GameRegistry.registerItem(BOTANIA_MANA_DEBUG_TOOL, \"botania_mana_debug_tool\")"));
        assertTrue(command.contains("\"botania\".equals(action)"));
        assertTrue(command.contains("\"debug\".equalsIgnoreCase(args[1])"));
        assertTrue(command.contains("\"tool\".equalsIgnoreCase(args[2])"));
        assertTrue(command.contains("GTNHJourney.BOTANIA_MANA_DEBUG_TOOL"));
        assertTrue(policy.contains("ItemBotaniaManaDebugTool"));
    }

    @Test
    public void toolReadsPoolsSpreadersAndManaStoringFlowersWithoutHardBotaniaDependency() throws IOException {
        String tool = read("src/main/java/dev/gtnhjourney/debug/ItemBotaniaManaDebugTool.java");
        String inspector = read("src/main/java/dev/gtnhjourney/debug/BotaniaManaPoolInspector.java");

        assertTrue(tool.contains("world.getTileEntity(x, y, z)"));
        assertTrue(tool.contains("BotaniaManaPoolInspector.inspect"));
        assertTrue(inspector.contains("vazkii.botania.api.mana.IManaPool"));
        assertTrue(inspector.contains("vazkii.botania.api.mana.IManaCollector"));
        assertTrue(inspector.contains("vazkii.botania.common.block.tile.TileSpecialFlower"));
        assertTrue(inspector.contains("vazkii.botania.api.subtile.SubTileGenerating"));
        assertTrue(inspector.contains("vazkii.botania.api.subtile.SubTileFunctional"));
        assertTrue(inspector.contains("getCurrentMana"));
        assertTrue(inspector.contains("getAvailableSpaceForMana"));
        assertTrue(inspector.contains("getMaxMana"));
        assertTrue(inspector.contains("writeToPacketNBTInternal"));
        assertFalse(tool.contains("target is not a compatible Mana Pool"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
