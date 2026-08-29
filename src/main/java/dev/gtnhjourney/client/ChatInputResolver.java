package dev.gtnhjourney.client;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;

/** Resolves the live chat text field without assuming a fixed transformed GuiChat field layout. */
public final class ChatInputResolver {

    private static final Map<Class<?>, Field> SUCCESSFUL_FIELDS = new HashMap<Class<?>, Field>();

    private ChatInputResolver() {}

    public static GuiTextField resolve(GuiChat chat) {
        if (chat == null) {
            CommandHintDiagnostics.recordResolverFailure();
            return null;
        }

        Class<?> runtimeClass = chat.getClass();
        Field cached;
        synchronized (SUCCESSFUL_FIELDS) {
            cached = SUCCESSFUL_FIELDS.get(runtimeClass);
        }
        Object cachedValue = readAssignable(chat, cached, GuiTextField.class);
        if (cachedValue != null) {
            recordSuccess(cached);
            return (GuiTextField) cachedValue;
        }
        if (cached != null) {
            synchronized (SUCCESSFUL_FIELDS) {
                SUCCESSFUL_FIELDS.remove(runtimeClass);
            }
        }

        Field resolved = findLiveAssignableField(chat, GuiTextField.class);
        if (resolved == null) {
            CommandHintDiagnostics.recordResolverFailure();
            return null;
        }
        Object value = readAssignable(chat, resolved, GuiTextField.class);
        if (value == null) {
            CommandHintDiagnostics.recordResolverFailure();
            return null;
        }
        synchronized (SUCCESSFUL_FIELDS) {
            SUCCESSFUL_FIELDS.put(runtimeClass, resolved);
        }
        recordSuccess(resolved);
        return (GuiTextField) value;
    }

    static Field findLiveAssignableField(Object target, Class<?> fieldType) {
        if (target == null || fieldType == null) return null;
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!fieldType.isAssignableFrom(field.getType())) continue;
                if (readAssignable(target, field, fieldType) != null) return field;
            }
        }
        return null;
    }

    static Object readAssignable(Object target, Field field, Class<?> fieldType) {
        if (target == null || field == null || fieldType == null) return null;
        try {
            if (!field.isAccessible()) field.setAccessible(true);
            Object value = field.get(target);
            return fieldType.isInstance(value) ? value : null;
        } catch (IllegalAccessException ignored) {
            return null;
        } catch (RuntimeException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }

    private static void recordSuccess(Field field) {
        if (field == null) return;
        CommandHintDiagnostics.recordResolverSuccess(
            field.getDeclaringClass().getName() + "#" + field.getName());
    }
}
