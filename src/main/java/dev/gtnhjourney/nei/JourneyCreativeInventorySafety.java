package dev.gtnhjourney.nei;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Removes only renderer-hostile GT volumetric-flask permutations from vanilla Creative's backing item list before
 * rendering. GregTech 5.09.54.20 creates a filled flask for every registered fluid, including fluids with no icon.
 */
public final class JourneyCreativeInventorySafety {

    private static Field itemListField;
    private static Field currentScrollField;
    private static Method scrollToMethod;
    private static boolean reflectionFailed;
    private static boolean loggedFailure;

    private GuiContainerCreative activeScreen;
    private final Map<ItemStack, Boolean> checked = new IdentityHashMap<ItemStack, Boolean>();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || reflectionFailed) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        GuiScreen screen = minecraft == null ? null : minecraft.currentScreen;
        if (!(screen instanceof GuiContainerCreative)) {
            activeScreen = null;
            checked.clear();
            return;
        }

        GuiContainerCreative creative = (GuiContainerCreative) screen;
        if (activeScreen != creative) {
            activeScreen = creative;
            checked.clear();
        }
        sanitize(creative);
    }

    @SuppressWarnings("unchecked")
    private void sanitize(final GuiContainerCreative creative) {
        if (creative == null || creative.inventorySlots == null) return;
        try {
            final Container container = creative.inventorySlots;
            final List<ItemStack> items = (List<ItemStack>) itemListField(container).get(container);
            int removed = JourneyListSanitizer.removeMatching(items, new JourneyListSanitizer.Matcher<ItemStack>() {

                @Override
                public boolean matches(ItemStack stack) {
                    if (stack == null || stack.getItem() == null) return false;
                    Boolean unsafe = checked.get(stack);
                    if (unsafe == null) {
                        unsafe = Boolean.valueOf(JourneyGlobalSafetyPolicy.shouldHide(stack));
                        checked.put(stack, unsafe);
                    }
                    if (!unsafe.booleanValue()) return false;
                    checked.remove(stack);
                    return true;
                }
            });
            if (removed <= 0) return;

            float scroll = currentScrollField().getFloat(creative);
            scrollToMethod(container).invoke(container, Float.valueOf(scroll));
        } catch (Throwable failure) {
            reflectionFailed = true;
            checked.clear();
            if (!loggedFailure) {
                loggedFailure = true;
                FMLLog.warning(
                    "[GTNH Journey] Creative flask safety unavailable; leaving vanilla Creative unchanged: %s",
                    failure.toString());
            }
        }
    }

    private static Field itemListField(Container container) throws NoSuchFieldException {
        if (itemListField == null) itemListField = findField(container.getClass(), "itemList", "field_148330_a");
        return itemListField;
    }

    private static Field currentScrollField() throws NoSuchFieldException {
        if (currentScrollField == null) {
            currentScrollField = findField(GuiContainerCreative.class, "currentScroll", "field_147067_x");
        }
        return currentScrollField;
    }

    private static Method scrollToMethod(Container container) throws NoSuchMethodException {
        if (scrollToMethod == null) {
            scrollToMethod = findMethod(container.getClass(), new String[] { "scrollTo", "func_148329_a" }, float.class);
        }
        return scrollToMethod;
    }

    private static Field findField(Class<?> type, String... names) throws NoSuchFieldException {
        for (String name : names) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(type.getName());
    }

    private static Method findMethod(Class<?> type, String[] names, Class<?> parameter) throws NoSuchMethodException {
        for (String name : names) {
            try {
                Method method = type.getDeclaredMethod(name, parameter);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {}
        }
        throw new NoSuchMethodException(type.getName());
    }
}
