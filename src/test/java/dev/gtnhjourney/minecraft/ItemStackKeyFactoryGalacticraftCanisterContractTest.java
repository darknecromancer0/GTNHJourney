package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class ItemStackKeyFactoryGalacticraftCanisterContractTest {

    @Test
    public void canisterFillMetadataIsCanonicalizedBeforeGenericDurabilityCollapse() throws IOException {
        String source = new String(
            Files.readAllBytes(Paths.get("src/main/java/dev/gtnhjourney/minecraft/ItemStackKeyFactory.java")),
            StandardCharsets.UTF_8);

        int researchMeta = source.indexOf("private static int researchMeta");
        int canister = source.indexOf("GalacticraftCanisterStatePolicy.matches(itemId)", researchMeta);
        int durability = source.indexOf("stack.getItem().isDamageable()", researchMeta);

        assertTrue(researchMeta >= 0, "research metadata method must exist");
        assertTrue(canister > researchMeta, "live ItemStack identity must use Galacticraft canister semantics");
        assertTrue(durability > canister, "canister fill metadata must be handled before generic wear collapse");
    }
}
