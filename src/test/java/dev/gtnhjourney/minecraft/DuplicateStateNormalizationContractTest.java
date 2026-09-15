package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

/** Regression contract for high-volume mutable/container NBT that must not become separate Journey states. */
public class DuplicateStateNormalizationContractTest {

    @Test
    public void embeddedInventoryPolicyKnowsAeStoragePayloads() throws Exception {
        String source = read("src/main/java/dev/gtnhjourney/minecraft/EmbeddedInventoryPolicy.java");
        assertTrue(source.contains("normalizeAeStorageCellContents"),
            "AE2 and AE2FC storage-cell contents must be stripped from Journey retrieval identity");
        assertTrue(source.contains("#") && source.contains("@"),
            "AE cell slot payload and count tags must be normalized");
    }

    @Test
    public void knownTransientPolicyDropsUltraTerminalCraftingCacheAndClipboardWorkState() throws Exception {
        String source = read("src/main/java/dev/gtnhjourney/minecraft/KnownTransientItemStatePolicy.java");
        assertTrue(source.contains("wireless_ultra_terminal") && source.contains("crafting"),
            "AE2FC Ultra Terminal crafting-grid cache must not create research variants");
        assertTrue(source.contains("BiblioClipboard") && source.contains("currentPage"),
            "BiblioCraft clipboard page/work state must not create research variants");
    }

    @Test
    public void thaumcraftJarPolicyCanonicalizesEssentiaAmountWithoutMergingAspectType() throws Exception {
        String source = read("src/main/java/dev/gtnhjourney/minecraft/ThaumcraftJarStatePolicy.java");
        assertTrue(source.contains("BlockJarFilledItem"), "filled Thaumcraft jars need a dedicated semantic policy");
        assertTrue(source.contains("AspectFilter"), "jar label/filter identity must survive normalization");
        assertTrue(source.contains("64"), "positive essentia amount must canonicalize to the full jar endpoint");
    }

    @Test
    public void researchNormalizerAppliesJarPolicyForRuntimeAndPersistedEntries() throws Exception {
        String runtime = read("src/main/java/dev/gtnhjourney/minecraft/ResearchTemplateNormalizer.java");
        String persisted = read("src/main/java/dev/gtnhjourney/minecraft/PersistedResearchEntryResolver.java");
        assertTrue(runtime.contains("ThaumcraftJarStatePolicy.normalize"),
            "new observations must normalize Thaumcraft jar amounts");
        assertTrue(persisted.contains("ThaumcraftJarStatePolicy.normalize"),
            "old persisted jar amount variants must collapse on load");
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
