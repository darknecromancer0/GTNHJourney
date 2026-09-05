package dev.gtnhjourney;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import dev.gtnhjourney.client.CommandHintKeyHandler;
import dev.gtnhjourney.nei.JourneyGroupMode;
import dev.gtnhjourney.nei.JourneyOrderMode;
import dev.gtnhjourney.nei.JourneySortState;
import dev.gtnhjourney.nei.JourneyViewState;

public class Release1126ContractTest {

    private static final Pattern RUNTIME_VERSION = Pattern.compile(
        "public static final String VERSION = \\\"([^\\\"]+)\\\";");

    @AfterEach
    public void resetPresentationState() {
        JourneySortState.reset();
        JourneyViewState.setMode(JourneyViewState.Mode.ALL);
    }

    @Test
    public void releaseMetadataAgreesEverywhere() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/GTNHJourney.java");
        String build = read("build.gradle.kts");
        String mcmod = read("src/main/resources/mcmod.info");

        Matcher matcher = RUNTIME_VERSION.matcher(source);
        assertTrue(matcher.find(), "runtime version constant missing");
        String version = matcher.group(1);
        assertTrue(build.contains("version = \"" + version + "\""));
        assertTrue(mcmod.contains("\"version\": \"" + version + "\""));
    }

    @Test
    public void newestIsNotAContentModeAndSortDimensionsRemainIndependent() {
        assertFalse(Arrays.stream(JourneyViewState.Mode.values()).anyMatch(mode -> "NEWEST".equals(mode.name())));

        JourneySortState.setGroup(JourneyViewState.Mode.RESEARCHED, JourneyGroupMode.NATIVE);
        JourneySortState.setOrder(JourneyViewState.Mode.RESEARCHED, JourneyOrderMode.UNLOCK);
        JourneySortState.setLatest(JourneyViewState.Mode.RESEARCHED, true);
        JourneySortState.setOrder(JourneyViewState.Mode.FAVOURITE, JourneyOrderMode.FAVOURITE_ADDED);

        assertEquals(JourneyGroupMode.NATIVE, JourneySortState.group(JourneyViewState.Mode.RESEARCHED));
        assertEquals(JourneyOrderMode.UNLOCK, JourneySortState.order(JourneyViewState.Mode.RESEARCHED));
        assertTrue(JourneySortState.latest(JourneyViewState.Mode.RESEARCHED));
        assertEquals(JourneyOrderMode.FAVOURITE_ADDED, JourneySortState.order(JourneyViewState.Mode.FAVOURITE));
    }

    @Test
    public void plainArrowsRemainVanillaHistoryWhileShiftArrowsNavigateSuggestions() {
        assertFalse(CommandHintKeyHandler.shouldMoveSelection(200, false));
        assertFalse(CommandHintKeyHandler.shouldMoveSelection(208, false));
        assertTrue(CommandHintKeyHandler.shouldMoveSelection(200, true));
        assertTrue(CommandHintKeyHandler.shouldMoveSelection(208, true));
    }

    @Test
    public void explicitNeiViewKeepsNativeItemClickHandling() throws IOException {
        String input = compactWhitespace(read("src/main/java/dev/gtnhjourney/nei/JourneyNEIInputHandler.java"));
        assertTrue(input.contains("if(mode==JourneyViewState.Mode.ALL)returnfalse;"));
    }

    @Test
    public void creativeViewUsesJourneyServerAuthorityInsteadOfNativeNeiCheatToggle() throws IOException {
        String input = compactWhitespace(read("src/main/java/dev/gtnhjourney/nei/JourneyNEIInputHandler.java"));
        String network = read("src/main/java/dev/gtnhjourney/network/JourneyNetwork.java");
        String queue = compactWhitespace(read("src/main/java/dev/gtnhjourney/network/ServerRequestQueue.java"));

        assertTrue(input.contains("JourneyNetwork.requestCreativeIssue("));
        assertFalse(input.contains("NEIClientConfig.canCheatItem"));
        assertFalse(input.contains("ClientPresentationActivityMirror.touch("));
        assertTrue(network.contains("CreativeIssueRequestMessage"));
        assertTrue(queue.contains("JourneyAdminPermissionPolicy.mayMutate(player)"));
        assertTrue(queue.contains("caseCREATIVE_ISSUE:"));
        assertTrue(queue.contains("observeCreativeIssue(player,template);"));
        assertFalse(queue.contains("CreativeIssueResearchSuppressor.mark(player,template)"));
    }

    @Test
    public void nativeNeiMembershipFiltersAreAppliedBeforeJourneySorting() throws IOException {
        String panel = compactWhitespace(read("src/main/java/dev/gtnhjourney/nei/JourneyPanelController.java"));
        String pipeline = read("src/main/java/dev/gtnhjourney/nei/JourneyNeiFilterPipeline.java");
        int filter = panel.indexOf("JourneyNeiFilterPipeline.matchesAll");
        int sort = panel.indexOf("returnstacks(sort(survivors");
        assertTrue(filter >= 0);
        assertTrue(sort > filter);
        assertTrue(pipeline.contains("SUBSET_WIDGET"));
        assertTrue(pipeline.contains("nativeRepresentative"));
        assertTrue(pipeline.contains("binding.searchField"));
    }

    @Test
    public void pageRetentionAndRightServiceClusterAreExplicitPolicies() throws IOException {
        String page = read("src/main/java/dev/gtnhjourney/nei/JourneyPageRetentionPolicy.java");
        String header = compactWhitespace(read("src/main/java/dev/gtnhjourney/nei/JourneyHeaderLayout.java"));
        assertTrue(page.contains("return Math.min(previous, last);"));
        assertTrue(header.contains("intnativeGX=pageNextX-pageNextW-GAP;"));
        assertTrue(header.contains("RightClustercluster=rightCluster(nativeG,pagePrevY,2);"));
        assertTrue(header.contains("if(!fits(leftEnd,cluster.latest))cluster=rightCluster(nativeG,pagePrevY,1);"));
        assertTrue(header.contains("if(!fits(leftEnd,cluster.latest))cluster=rightCluster(nativeG,pagePrevY,0);"));
        assertTrue(header.contains("Slotorder=slot(cursor-GAP-SMALL"));
        assertTrue(header.contains("Slotlatest=slot(group.x-GAP-SMALL"));
    }

    @Test
    public void liveChecklistCoversReleaseBlockingSortingAndInputRegressions() throws IOException {
        Path checklist = Paths.get("docs/v1.1.26-live-test.md");
        assertTrue(Files.isRegularFile(checklist));
        String text = read(checklist.toString()).toLowerCase();
        assertTrue(text.contains("j + n + l"));
        assertTrue(text.contains("c + n + l"));
        assertTrue(text.contains("page 2+"));
        assertTrue(text.contains("item subsets"));
        assertTrue(text.contains("@"));
        assertTrue(text.contains("%"));
        assertTrue(text.contains("#"));
        assertTrue(text.contains("ctrl+backspace"));
        assertTrue(text.contains("shift+up"));
        assertTrue(text.contains("s/t disappear"));
        assertTrue(text.contains("native g"));
        assertTrue(text.contains("dropdowns render above nei item cells"));
        assertTrue(text.contains("immediately researches"));
    }

    @Test
    public void liveAddendumSupersedesOldGroupedLatestAndCreativeResearchRules() throws IOException {
        Path addendum = Paths.get("docs/superpowers/specs/2026-09-05-v1.1.26-live-sorting-ui-addendum.md");
        assertTrue(Files.isRegularFile(addendum));
        String text = read(addendum.toString()).toLowerCase();
        assertTrue(text.contains("highest activity sequence is promoted to the first position"));
        assertTrue(text.contains("immediately through the normal authoritative research path"));
        assertTrue(text.contains("postrenderobjects"));
        assertTrue(text.contains("remember group/order/l independently"));
    }

    private static String compactWhitespace(String value) {
        return value.replaceAll("\\s+", "");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
