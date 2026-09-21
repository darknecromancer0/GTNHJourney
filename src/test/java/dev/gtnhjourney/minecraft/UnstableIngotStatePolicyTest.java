package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class UnstableIngotStatePolicyTest {

    @Test
    public void legacyExtraUtilitiesRuntimeFieldsDoNotParticipateInIdentity() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("time", 123456L);
        tag.setInteger("dimension", -1);
        tag.setBoolean("creative", false);
        tag.setString("marker", "keep");

        UnstableIngotStatePolicy.normalizeIdentity("ExtraUtilities:unstableingot", 0, tag);

        assertFalse(tag.hasKey("time"));
        assertFalse(tag.hasKey("dimension"));
        assertTrue(tag.hasKey("creative"));
        assertTrue(tag.getBoolean("crafting"));
        assertEquals("keep", tag.getString("marker"));
    }

    @Test
    public void legacyBaseAndCraftingFormsCanonicalizeToSameDangerousState() {
        NBTTagCompound base = new NBTTagCompound();
        NBTTagCompound crafted = new NBTTagCompound();
        crafted.setBoolean("crafting", true);

        UnstableIngotStatePolicy.normalizeIdentity("ExtraUtilities:unstableingot", 0, base);
        UnstableIngotStatePolicy.normalizeIdentity("ExtraUtilities:unstableingot", 0, crafted);

        assertTrue(base.getBoolean("crafting"));
        assertTrue(crafted.getBoolean("crafting"));
        assertEquals(NbtCanonicalizer.canonicalize(base), NbtCanonicalizer.canonicalize(crafted));
    }

    @Test
    public void currentUtilitiesInExcessCraftedAtDoesNotParticipateInIdentity() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("Crafted", true);
        tag.setLong("CraftedAt", 123456L);
        tag.setString("marker", "keep");

        UnstableIngotStatePolicy.normalizeIdentity("utilitiesinexcess:inverted_ingot", 0, tag);

        assertTrue(tag.getBoolean("Crafted"));
        assertFalse(tag.hasKey("CraftedAt"));
        assertEquals("keep", tag.getString("marker"));
    }

    @Test
    public void expiredPersistedTimestampCannotBecomeSafeUntaggedIngot() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("CraftedAt", 123456L);

        UnstableIngotStatePolicy.normalizeIdentity("utilitiesinexcess:inverted_ingot", 0, tag);

        assertTrue(tag.getBoolean("Crafted"));
        assertFalse(tag.hasKey("CraftedAt"));
    }

    @Test
    public void stableUtilitiesInExcessSiblingMetasRemainExact() {
        NBTTagCompound stable = currentTimerTag();
        NBTTagCompound quasiNormalized = currentTimerTag();

        UnstableIngotStatePolicy.normalizeIdentity("utilitiesinexcess:inverted_ingot", 1, stable);
        UnstableIngotStatePolicy.normalizeIdentity("utilitiesinexcess:inverted_ingot", 2, quasiNormalized);

        assertTrue(stable.hasKey("CraftedAt"));
        assertTrue(quasiNormalized.hasKey("CraftedAt"));
    }

    @Test
    public void currentRetrievalUsesNativePreTimerRecipeForm() {
        ItemStack stack = new ItemStack(new Item(), 1, 0);
        NBTTagCompound tag = currentTimerTag();
        tag.setString("marker", "keep");
        stack.setTagCompound(tag);

        ItemStack refreshed = UnstableIngotStatePolicy
            .refreshForRetrieval(stack, "utilitiesinexcess:inverted_ingot", 0, 987654L, 7);

        assertSame(stack, refreshed);
        assertTrue(stack.getTagCompound().getBoolean("Crafted"));
        assertFalse(stack.getTagCompound().hasKey("CraftedAt"));
        assertEquals("keep", stack.getTagCompound().getString("marker"));
    }

    @Test
    public void legacyRetrievalStillGetsFreshTimer() {
        ItemStack stack = new ItemStack(new Item(), 1, 0);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("time", 123456L);
        tag.setInteger("dimension", -1);
        stack.setTagCompound(tag);

        UnstableIngotStatePolicy
            .refreshForRetrieval(stack, "ExtraUtilities:unstableingot", 0, 987654L, 7);

        assertEquals(987654L, stack.getTagCompound().getLong("time"));
        assertEquals(7, stack.getTagCompound().getInteger("dimension"));
    }

    @Test
    public void normalizationRunsForIdentityTemplateAndPersistedMigration() throws Exception {
        String identity = read("src/main/java/dev/gtnhjourney/minecraft/ResearchNbtIdentity.java");
        String template = read("src/main/java/dev/gtnhjourney/minecraft/ResearchTemplateNormalizer.java");
        String persisted = read("src/main/java/dev/gtnhjourney/minecraft/PersistedResearchEntryResolver.java");

        assertTrue(identity.contains("UnstableIngotStatePolicy.normalizeIdentity"));
        assertTrue(template.contains("UnstableIngotStatePolicy.normalizeIdentity"));
        assertTrue(persisted.contains("UnstableIngotStatePolicy.normalizeIdentity"));
    }

    private static NBTTagCompound currentTimerTag() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("Crafted", true);
        tag.setLong("CraftedAt", 123456L);
        return tag;
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
