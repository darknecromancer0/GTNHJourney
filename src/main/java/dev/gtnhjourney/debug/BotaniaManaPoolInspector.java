package dev.gtnhjourney.debug;

import java.lang.reflect.Method;

import net.minecraft.nbt.NBTTagCompound;

/** Optional Botania 1.7.10 mana-holder bridge. Production code never links Botania classes directly. */
public final class BotaniaManaPoolInspector {

    private static final String MANA_POOL_INTERFACE = "vazkii.botania.api.mana.IManaPool";
    private static final String MANA_COLLECTOR_INTERFACE = "vazkii.botania.api.mana.IManaCollector";
    private static final String SPECIAL_FLOWER_CLASS = "vazkii.botania.common.block.tile.TileSpecialFlower";
    private static final String GENERATING_SUBTILE_CLASS = "vazkii.botania.api.subtile.SubTileGenerating";
    private static final String FUNCTIONAL_SUBTILE_CLASS = "vazkii.botania.api.subtile.SubTileFunctional";

    private BotaniaManaPoolInspector() {}

    public static Result inspect(Object target) {
        if (target == null) return null;
        try {
            if (implementsNamedInterface(target.getClass(), MANA_POOL_INTERFACE)) return inspectPool(target);
            if (implementsNamedInterface(target.getClass(), MANA_COLLECTOR_INTERFACE)) return inspectCollector(target);
            if (extendsNamedClass(target.getClass(), SPECIAL_FLOWER_CLASS)) return inspectFlower(target);
            return null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        } catch (SecurityException ignored) {
            return null;
        } catch (RuntimeException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }

    private static Result inspectPool(Object target) throws ReflectiveOperationException {
        Method currentMethod = publicMethod(target, "getCurrentMana");
        Method freeMethod = publicMethod(target, "getAvailableSpaceForMana");
        int current = nonNegative(number(currentMethod.invoke(target)));
        int free = nonNegative(number(freeMethod.invoke(target)));
        long capacityLong = (long) current + (long) free;
        int capacity = capacityLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) capacityLong;
        return new Result(current, capacity, free);
    }

    private static Result inspectCollector(Object target) throws ReflectiveOperationException {
        Method currentMethod = publicMethod(target, "getCurrentMana");
        Method maxMethod = publicMethod(target, "getMaxMana");
        return fromCurrentAndCapacity(number(currentMethod.invoke(target)), number(maxMethod.invoke(target)));
    }

    private static Result inspectFlower(Object target) throws ReflectiveOperationException {
        Method getSubTile = publicMethod(target, "getSubTile");
        Object subTile = getSubTile.invoke(target);
        if (subTile == null) return null;
        Class<?> subTileType = subTile.getClass();
        if (!extendsNamedClass(subTileType, GENERATING_SUBTILE_CLASS)
            && !extendsNamedClass(subTileType, FUNCTIONAL_SUBTILE_CLASS)) return null;

        Method maxMethod = publicMethod(subTile, "getMaxMana");
        Method packetWriter = subTileType.getMethod("writeToPacketNBTInternal", NBTTagCompound.class);
        if (!packetWriter.isAccessible()) packetWriter.setAccessible(true);
        NBTTagCompound packetTag = new NBTTagCompound();
        packetWriter.invoke(subTile, packetTag);
        if (!packetTag.hasKey("mana", 99)) return null;
        return fromCurrentAndCapacity(packetTag.getInteger("mana"), number(maxMethod.invoke(subTile)));
    }

    private static Result fromCurrentAndCapacity(int currentValue, int capacityValue) {
        int current = nonNegative(currentValue);
        int capacity = nonNegative(capacityValue);
        int free = Math.max(0, capacity - current);
        return new Result(current, capacity, free);
    }

    private static Method publicMethod(Object owner, String name) throws NoSuchMethodException {
        Method method = owner.getClass().getMethod(name);
        if (!method.isAccessible()) method.setAccessible(true);
        return method;
    }

    private static int number(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static int nonNegative(int value) {
        return Math.max(0, value);
    }

    private static boolean implementsNamedInterface(Class<?> type, String interfaceName) {
        if (type == null || interfaceName == null) return false;
        for (Class<?> iface : type.getInterfaces()) {
            if (interfaceName.equals(iface.getName()) || implementsNamedInterface(iface, interfaceName)) return true;
        }
        return implementsNamedInterface(type.getSuperclass(), interfaceName);
    }

    private static boolean extendsNamedClass(Class<?> type, String className) {
        if (type == null || className == null) return false;
        if (className.equals(type.getName())) return true;
        return extendsNamedClass(type.getSuperclass(), className);
    }

    public static final class Result {

        private final int currentMana;
        private final int capacity;
        private final int freeMana;

        Result(int currentMana, int capacity, int freeMana) {
            this.currentMana = currentMana;
            this.capacity = capacity;
            this.freeMana = freeMana;
        }

        public int currentMana() {
            return currentMana;
        }

        public int capacity() {
            return capacity;
        }

        public int freeMana() {
            return freeMana;
        }

        public double percent() {
            if (capacity <= 0) return 0.0D;
            return (double) currentMana * 100.0D / (double) capacity;
        }
    }
}
