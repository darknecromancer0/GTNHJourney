package vazkii.botania.api.subtile;

import net.minecraft.nbt.NBTTagCompound;

/** Test-only functional-flower subtile shape matching the GTNH Botania reflection contract. */
public class SubTileFunctional {

    private final int mana;
    private final int maxMana;

    public SubTileFunctional(int mana, int maxMana) {
        this.mana = mana;
        this.maxMana = maxMana;
    }

    public int getMaxMana() {
        return maxMana;
    }

    public final void writeToPacketNBTInternal(NBTTagCompound tag) {
        tag.setInteger("mana", mana);
    }
}
