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
    public void knownTransientPolicyDropsUltraTerminalClipboardAndGraviRuntimeState() throws Exception {
        String source = read("src/main/java/dev/gtnhjourney/minecraft/KnownTransientItemStatePolicy.java");
        assertTrue(source.contains("wireless_ultra_terminal") && source.contains("crafting"),
            "AE2FC Ultra Terminal crafting-grid cache must not create research variants");
        assertTrue(source.contains("BiblioClipboard") && source.contains("currentPage"),
            "BiblioCraft clipboard page/work state must not create research variants");
        assertTrue(source.contains("GraviSuite:advJetpack") && source.contains("isFlyActive")
            && source.contains("isHoverActive") && source.contains("toggleTimer"),
            "GraviSuite flight-mode runtime flags must not create research variants");
    }

    @Test
    public void thaumcraftJarPolicyCanonicalizesEssentiaAmountWithoutMergingAspectType() throws Exception {
        String source = read("src/main/java/dev/gtnhjourney/minecraft/ThaumcraftJarStatePolicy.java");
        assertTrue(source.contains("BlockJarFilledItem"), "filled Thaumcraft jars need a dedicated semantic policy");
        assertTrue(source.contains("AspectFilter"), "jar label/filter identity must survive normalization");
        assertTrue(source.contains("64"), "positive essentia amount must canonicalize to the full jar endpoint");
    }

    @Test
    public void galaxySpaceJetplateUsesOnlyEmptyAndFullChargeEndpoints() throws Exception {
        String source = read("src/main/java/dev/gtnhjourney/minecraft/GalaxySpaceChargeStatePolicy.java");
        assertTrue(source.contains("item.spacesuit_jetplate"), "GalaxySpace jetplate needs a verified endpoint policy");
        assertTrue(source.contains("electricity") && source.contains("100000"),
            "jetplate charge identity must collapse to the verified 0/100% endpoints");
        String expander = read("src/main/java/dev/gtnhjourney/minecraft/ResearchStateExpander.java");
        String keys = compactWhitespace(read("src/main/java/dev/gtnhjourney/minecraft/ItemStackKeyFactory.java"));
        assertTrue(expander.contains("GalaxySpaceChargeStatePolicy.expand"),
            "positive jetplate charge must prove both endpoint states");
        assertTrue(keys.contains("GalaxySpaceChargeStatePolicy.identityStack"),
            "jetplate research keys must never retain intermediate electricity");
    }

    @Test
    public void runtimeAndPersistedIdentityApplyNewSemanticPolicies() throws Exception {
        String runtime = compactWhitespace(
            read("src/main/java/dev/gtnhjourney/minecraft/ResearchTemplateNormalizer.java"));
        String identity = compactWhitespace(
            read("src/main/java/dev/gtnhjourney/minecraft/ResearchNbtIdentity.java"));
        String persisted = compactWhitespace(
            read("src/main/java/dev/gtnhjourney/minecraft/PersistedResearchEntryResolver.java"));
        assertTrue(runtime.contains("ThaumcraftJarStatePolicy.normalize"),
            "new observations must normalize Thaumcraft jar amounts");
        assertTrue(identity.contains("ThaumcraftJarStatePolicy.normalize"),
            "research identity must normalize Thaumcraft jar amounts");
        assertTrue(persisted.contains("ThaumcraftJarStatePolicy.normalize"),
            "old persisted jar amount variants must collapse on load");
        assertTrue(persisted.contains("GalaxySpaceChargeStatePolicy"),
            "old jetplate charge variants must recanonicalize on load");
        assertTrue(runtime.contains("VanillaEquipmentStatePolicy.normalize"),
            "new vanilla equipment observations must drop enchant-only duplicate state");
        assertTrue(identity.contains("VanillaEquipmentStatePolicy.normalize"),
            "vanilla equipment research identity must ignore enchant-only duplicate state");
        assertTrue(persisted.contains("VanillaEquipmentStatePolicy.normalize"),
            "old enchanted-equipment duplicate states must collapse on load");

        String keys = read("src/main/java/dev/gtnhjourney/minecraft/ItemStackKeyFactory.java");
        assertTrue(keys.contains("KnownMetadataAliasPolicy.canonicalMeta"),
            "new invalid BOP hive metadata must canonicalize before research storage");
        assertTrue(persisted.contains("KnownMetadataAliasPolicy.canonicalMeta"),
            "persisted invalid BOP hive metadata must migrate to the canonical block state");
    }

    private static String compactWhitespace(String value) {
        return value.replaceAll("\\s+", "");
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
