package dev.gtnhjourney.acquisition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

public class InventoryStackSignatureTest {

    @Test
    public void subtypeMetadataRemainsPartOfFastIdentity() {
        Item item = new Item().setHasSubtypes(true);
        ItemStack first = new ItemStack(item, 1, 1001);
        ItemStack second = new ItemStack(item, 1, 1002);

        assertEquals(1001, InventoryStackSignature.fastMeta(first));
        assertEquals(1002, InventoryStackSignature.fastMeta(second));
    }

    @Test
    public void classicDurabilityWearDoesNotChurnFastIdentity() {
        Item item = new Item().setMaxDamage(100);
        ItemStack fresh = new ItemStack(item, 1, 0);
        ItemStack worn = new ItemStack(item, 1, 37);

        assertEquals(0, InventoryStackSignature.fastMeta(fresh));
        assertEquals(0, InventoryStackSignature.fastMeta(worn));
    }

    @Test
    public void electricEndpointSignatureStillSeparatesMetaItemsSharingOneItem() {
        int sameItemHash = 12345;
        int baseEndpoint = 101;

        int first = InventoryStackSignature.combineEndpointSignature(sameItemHash, 1001, baseEndpoint);
        int second = InventoryStackSignature.combineEndpointSignature(sameItemHash, 1002, baseEndpoint);

        assertNotEquals(first, second);
        assertEquals(first, InventoryStackSignature.combineEndpointSignature(sameItemHash, 1001, baseEndpoint));
    }

    @Test
    public void ic2VisualChargeDamageCollapsesBeforeEndpointSignature() {
        Item item = new Item().setMaxDamage(100);
        ItemStack emptyVisual = new ItemStack(item, 1, 100);
        ItemStack partialVisual = new ItemStack(item, 1, 37);

        assertEquals(100, InventoryStackSignature.stableEndpointMeta(emptyVisual, 201));
        assertEquals(100, InventoryStackSignature.stableEndpointMeta(partialVisual, 201));
    }

    @Test
    public void gtEndpointKeepsSubtypeMetaIdentity() {
        Item item = new Item().setHasSubtypes(true);
        ItemStack first = new ItemStack(item, 1, 1001);
        ItemStack second = new ItemStack(item, 1, 1002);

        assertEquals(1001, InventoryStackSignature.stableEndpointMeta(first, 101));
        assertEquals(1002, InventoryStackSignature.stableEndpointMeta(second, 101));
    }
}
