package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

/** Contract for the v1.1.20 rule: any verified positive charge unlocks a real 100% endpoint. */
public class PositiveChargeFullEndpointContractTest {

    @Test
    public void gregTechExpansionSynthesizesFullInsteadOfKeepingObservedPartialCharge() throws Exception {
        String source = read("src/main/java/dev/gtnhjourney/minecraft/ResearchStateExpander.java");
        assertTrue(source.contains("GtChargeStatePolicy.toFull"),
            "GT positive charge must synthesize the canonical 100% endpoint");
    }

    @Test
    public void cofhExpansionCanReceiveEnergyToBuildFullEndpoint() throws Exception {
        String source = read("src/main/java/dev/gtnhjourney/minecraft/CofhChargeStatePolicy.java");
        assertTrue(source.contains("receiveEnergy"), "CoFH full endpoint must use IEnergyContainerItem.receiveEnergy");
        assertTrue(source.contains("toFull"), "CoFH positive charge expansion must synthesize 100% charge");
    }

    @Test
    public void ic2ExpansionCanChargeCopyToBuildFullEndpoint() throws Exception {
        String source = read("src/main/java/dev/gtnhjourney/minecraft/Ic2ChargeStatePolicy.java");
        assertTrue(source.contains("getMethod(\"charge\""), "IC2 adapter must bind IElectricItemManager.charge");
        assertTrue(source.contains("toFull"), "IC2 positive charge expansion must synthesize 100% charge");
        assertTrue(source.contains("collapseFullEndpoint"), "IC2 Electric Jetpack dedupe exception must remain intact");
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
