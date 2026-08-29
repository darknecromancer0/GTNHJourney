package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

public class BotaniaTransientStatePolicyTest {

    @Test
    public void daybloomDecayCounterDoesNotCreateResearchVariants() {
        NBTTagCompound fresh = flower("daybloom", 143);
        NBTTagCompound old = flower("daybloom", 7212);

        BotaniaTransientStatePolicy.normalize("Botania:specialFlower", fresh);
        BotaniaTransientStatePolicy.normalize("Botania:specialFlower", old);

        assertFalse(fresh.hasKey("passiveDecayTicks"));
        assertFalse(old.hasKey("passiveDecayTicks"));
        assertEquals("daybloom", fresh.getString("type"));
        assertEquals(ResearchNbtIdentity.canonicalize(fresh), ResearchNbtIdentity.canonicalize(old));
    }

    @Test
    public void differentSpecialFlowerTypesRemainDifferent() {
        NBTTagCompound daybloom = flower("daybloom", 257);
        NBTTagCompound pureDaisy = flower("puredaisy", 257);

        BotaniaTransientStatePolicy.normalize("Botania:specialFlower", daybloom);
        BotaniaTransientStatePolicy.normalize("Botania:specialFlower", pureDaisy);

        assertNotEquals(ResearchNbtIdentity.canonicalize(daybloom), ResearchNbtIdentity.canonicalize(pureDaisy));
    }

    @Test
    public void wandBindingCoordinatesDoNotCreateResearchVariants() {
        NBTTagCompound first = wand(0, 0, 455, 80, -30);
        NBTTagCompound second = wand(0, 0, 458, 79, -32);

        BotaniaTransientStatePolicy.normalize("Botania:twigWand", first);
        BotaniaTransientStatePolicy.normalize("Botania:twigWand", second);

        assertFalse(first.hasKey("boundTileX"));
        assertFalse(first.hasKey("boundTileY"));
        assertFalse(first.hasKey("boundTileZ"));
        assertEquals(0, first.getInteger("color1"));
        assertEquals(0, first.getInteger("color2"));
        assertEquals(ResearchNbtIdentity.canonicalize(first), ResearchNbtIdentity.canonicalize(second));
    }

    @Test
    public void wandColorsRemainSemantic() {
        NBTTagCompound plain = wand(0, 0, 455, 80, -30);
        NBTTagCompound recolored = wand(3, 14, 455, 80, -30);

        BotaniaTransientStatePolicy.normalize("Botania:twigWand", plain);
        BotaniaTransientStatePolicy.normalize("Botania:twigWand", recolored);

        assertNotEquals(ResearchNbtIdentity.canonicalize(plain), ResearchNbtIdentity.canonicalize(recolored));
    }

    @Test
    public void unrelatedBotaniaNbtIsUntouched() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("passiveDecayTicks", 77);
        tag.setInteger("boundTileX", 42);

        BotaniaTransientStatePolicy.normalize("Botania:unknownItem", tag);

        assertEquals(77, tag.getInteger("passiveDecayTicks"));
        assertEquals(42, tag.getInteger("boundTileX"));
    }

    private static NBTTagCompound flower(String type, int decay) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("type", type);
        tag.setInteger("passiveDecayTicks", decay);
        return tag;
    }

    private static NBTTagCompound wand(int color1, int color2, int x, int y, int z) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("color1", color1);
        tag.setInteger("color2", color2);
        tag.setInteger("boundTileX", x);
        tag.setInteger("boundTileY", y);
        tag.setInteger("boundTileZ", z);
        return tag;
    }
}
