package vazkii.botania.api.mana;

/** Test-only shape of the GTNH Botania 1.7.10 mana pool API used through reflection in production. */
public interface IManaPool {

    int getCurrentMana();

    int getAvailableSpaceForMana();
}
