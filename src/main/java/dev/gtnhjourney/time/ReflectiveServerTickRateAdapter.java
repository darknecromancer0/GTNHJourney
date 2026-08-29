package dev.gtnhjourney.time;

import java.lang.reflect.Method;

import net.minecraft.server.MinecraftServer;

/** Bridges Journey to optional methods supplied by the MinecraftServer pacing mixin. */
public final class ReflectiveServerTickRateAdapter implements ServerTickRateAdapter {

    private static final String AVAILABLE = "gtnhjourney$isSpeedHookAvailable";
    private static final String SET = "gtnhjourney$setSpeedMultiplier";
    private static final String RESET = "gtnhjourney$resetSpeedMultiplier";

    private final TargetProvider targetProvider;

    public ReflectiveServerTickRateAdapter() {
        this(new TargetProvider() {
            @Override
            public Object currentServer() {
                return MinecraftServer.getServer();
            }
        });
    }

    ReflectiveServerTickRateAdapter(TargetProvider targetProvider) {
        if (targetProvider == null) throw new IllegalArgumentException("targetProvider must not be null");
        this.targetProvider = targetProvider;
    }

    @Override
    public boolean isSupported() {
        Object target = target();
        if (target == null) return false;
        try {
            Method method = method(target.getClass(), AVAILABLE);
            Object value = method.invoke(target);
            return Boolean.TRUE.equals(value);
        } catch (ReflectiveOperationException ignored) {
            return false;
        } catch (RuntimeException ignored) {
            return false;
        } catch (LinkageError ignored) {
            return false;
        }
    }

    @Override
    public boolean applyMultiplier(int multiplier) {
        if (!JourneySpeedState.isAllowedMultiplier(multiplier) || !isSupported()) return false;
        Object target = target();
        if (target == null) return false;
        try {
            Method method = method(target.getClass(), SET, Integer.TYPE);
            Object value = method.invoke(target, Integer.valueOf(multiplier));
            return Boolean.TRUE.equals(value);
        } catch (ReflectiveOperationException ignored) {
            return false;
        } catch (RuntimeException ignored) {
            return false;
        } catch (LinkageError ignored) {
            return false;
        }
    }

    @Override
    public void reset() {
        Object target = target();
        if (target == null) return;
        try {
            method(target.getClass(), RESET).invoke(target);
        } catch (ReflectiveOperationException ignored) {
        } catch (RuntimeException ignored) {
        } catch (LinkageError ignored) {
        }
    }

    private Object target() {
        try {
            return targetProvider.currentServer();
        } catch (RuntimeException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }

    private static Method method(Class<?> type, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = type.getMethod(name, parameterTypes);
        if (!method.isAccessible()) method.setAccessible(true);
        return method;
    }

    interface TargetProvider {
        Object currentServer();
    }
}
