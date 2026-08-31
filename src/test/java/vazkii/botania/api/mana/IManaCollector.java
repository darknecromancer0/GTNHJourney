package vazkii.botania.api.mana;

/** Test-only shape of the GTNH Botania 1.7.10 mana collector API used through reflection in production. */
public interface IManaCollector {

    int getCurrentMana();

    int getMaxMana();
}
