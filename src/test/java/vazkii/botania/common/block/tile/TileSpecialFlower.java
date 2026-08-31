package vazkii.botania.common.block.tile;

/** Test-only special-flower container shape matching the GTNH Botania reflection contract. */
public class TileSpecialFlower {

    private final Object subTile;

    public TileSpecialFlower(Object subTile) {
        this.subTile = subTile;
    }

    public Object getSubTile() {
        return subTile;
    }
}
