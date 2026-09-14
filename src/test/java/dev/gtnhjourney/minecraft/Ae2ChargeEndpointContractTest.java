package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

/** Regression contract for AE2 powered terminals: Journey keeps only the real 0% and 100% charge endpoints. */
public class Ae2ChargeEndpointContractTest {

    @Test
    public void ae2PolicyCollapsesRuntimeChargeAndSearchText() throws Exception {
        String source = read("src/main/java/dev/gtnhjourney/minecraft/Ae2ChargeStatePolicy.java");
        assertTrue(source.contains("internalCurrentPower"), "AE2 runtime power must be normalized");
        assertTrue(source.contains("searchString"), "terminal search text must never create research variants");
        assertTrue(source.contains("getAEMaxPower"), "100% endpoint must use the item's real AE capacity");
        assertTrue(source.contains("toFull"), "positive AE charge must synthesize a real 100% endpoint");
        assertTrue(source.contains("withoutCharge"), "0% endpoint must be represented explicitly");
    }

    @Test
    public void everyResearchIdentityPathUsesAe2ChargePolicy() throws Exception {
        String expander = read("src/main/java/dev/gtnhjourney/minecraft/ResearchStateExpander.java");
        String keyFactory = read("src/main/java/dev/gtnhjourney/minecraft/ItemStackKeyFactory.java");
        String persisted = read("src/main/java/dev/gtnhjourney/minecraft/PersistedResearchEntryResolver.java");

        assertTrue(expander.contains("Ae2ChargeStatePolicy.expand"),
            "new observations must expand AE2 powered items to endpoints");
        assertTrue(keyFactory.contains("Ae2ChargeStatePolicy.identityStack"),
            "research keys must not retain intermediate AE2 charge");
        assertTrue(persisted.contains("Ae2ChargeStatePolicy.identityStack"),
            "legacy persisted partial-charge variants must recanonicalize on load");
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
