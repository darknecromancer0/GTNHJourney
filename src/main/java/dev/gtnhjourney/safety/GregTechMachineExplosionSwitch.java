package dev.gtnhjourney.safety;

import java.lang.reflect.Field;

/** Narrow reflective access to GregTechAPI.sMachineExplosions. */
public final class GregTechMachineExplosionSwitch {

    private static final String API_CLASS = "gregtech.api.GregTechAPI";
    private static final String FIELD_NAME = "sMachineExplosions";
    private Field field;
    private boolean unavailable;

    public synchronized boolean isSupported() { return resolve() != null; }

    public synchronized boolean isEnabled() {
        Field resolved = resolve();
        if (resolved == null) return true;
        try {
            return resolved.getBoolean(null);
        } catch (IllegalAccessException ignored) {
            unavailable = true;
            return true;
        }
    }

    public synchronized boolean setEnabled(boolean enabled) {
        Field resolved = resolve();
        if (resolved == null) return false;
        try {
            resolved.setBoolean(null, enabled);
            return resolved.getBoolean(null) == enabled;
        } catch (IllegalAccessException ignored) {
            unavailable = true;
            return false;
        }
    }

    private Field resolve() {
        if (unavailable) return null;
        if (field != null) return field;
        try {
            Class<?> api = Class.forName(API_CLASS, false, GregTechMachineExplosionSwitch.class.getClassLoader());
            field = api.getField(FIELD_NAME);
            return field;
        } catch (Throwable ignored) {
            unavailable = true;
            return null;
        }
    }
}
