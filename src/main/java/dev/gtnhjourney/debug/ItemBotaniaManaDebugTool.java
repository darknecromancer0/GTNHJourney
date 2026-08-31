package dev.gtnhjourney.debug;

import java.util.Locale;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

/** Read-only diagnostic wand that reports exact mana stored in supported Botania mana holders. */
public final class ItemBotaniaManaDebugTool extends Item {

    public ItemBotaniaManaDebugTool() {
        setMaxStackSize(1);
        setUnlocalizedName("botaniaManaDebugTool");
        setTextureName("minecraft:blaze_rod");
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return "Botania Mana Debug Tool";
    }

    @Override
    public boolean hasEffect(ItemStack stack, int pass) {
        return true;
    }

    @Override
    public boolean onItemUseFirst(
        ItemStack stack,
        EntityPlayer player,
        World world,
        int x,
        int y,
        int z,
        int side,
        float hitX,
        float hitY,
        float hitZ) {
        if (world == null) return false;
        if (world.isRemote) return false;
        if (!(player instanceof EntityPlayerMP)) return false;

        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        TileEntity tile = world.getTileEntity(x, y, z);
        BotaniaManaPoolInspector.Result result = BotaniaManaPoolInspector.inspect(tile);
        if (result == null) {
            tell(serverPlayer, "Botania mana debug: target is not a supported Mana Pool, Spreader, or mana-storing flower.");
            return true;
        }

        tell(serverPlayer, format(x, y, z, result));
        return true;
    }

    static String format(int x, int y, int z, BotaniaManaPoolInspector.Result result) {
        if (result == null) return "Botania Mana: unavailable.";
        return "Botania Mana @ " + x + "," + y + "," + z + ": " + result.currentMana() + " / "
            + result.capacity() + " mana (" + String.format(Locale.ROOT, "%.2f", result.percent()) + "%), free "
            + result.freeMana() + ".";
    }

    private static void tell(EntityPlayerMP player, String text) {
        player.addChatMessage(new ChatComponentText("[Journey] " + text));
    }
}
