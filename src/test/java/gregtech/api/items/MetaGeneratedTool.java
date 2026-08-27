package gregtech.api.items;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.item.Item;

/** Test-scope stand-in for GT5U's generated tool base; production only reflects on its public mToolStats field. */
public class MetaGeneratedTool extends Item {

    public final Map<Short, Object> mToolStats = new HashMap<Short, Object>();
}
