package dev.gtnhjourney.debug;

import java.lang.reflect.Method;

/** Optional Botania 1.7.10 mana-pool bridge. Production code never links Botania classes directly. */
public final class BotaniaManaPoolInspector {

    private static final String MANA_POOL_INTERFACE = "vazkii.botania.api.mana.IManaPool";

    private BotaniaManaPoolInspector() {}

    public static Result inspect(Object target) {
        if (target == null || !implementsNamedInterface(target.getClass(), MANA_POOL_INTERFACE)) return null;
        try {
            Method currentMethod = target.getClass().getMethod("getCurrentMana");
            Method freeMethod = target.getClass().getMethod("getAvailableSpaceForMana");
            if (!currentMethod.isAccessible()) currentMethod.setAccessible(true);
            if (!freeMethod.isAccessible()) freeMethod.setAccessible(true);
            int current = nonNegative(number(currentMethod.invoke(target)));
            int free = nonNegative(number(freeMethod.invoke(target)));
            long capacityLong = (long) current + (long) free;
            int capacity = capacityLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) capacityLong;
            return new Result(current, capacity, free);
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
