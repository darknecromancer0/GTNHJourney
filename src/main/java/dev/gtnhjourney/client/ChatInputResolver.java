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
        GuiTextField cachedValue = read(chat, cached);
        if (cachedValue != null) {
            recordSuccess(cached);
            return cachedValue;
        }
        if (cached != null) {
            synchronized (SUCCESSFUL_FIELDS) {
                SUCCESSFUL_FIELDS.remove(runtimeClass);
            }
        }

        for (Class<?> type = runtimeClass; type != null && GuiChat.class.isAssignableFrom(type); type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!GuiTextField.class.isAssignableFrom(field.getType())) continue;
                GuiTextField value = read(chat, field);
                if (value == null) continue;
                synchronized (SUCCESSFUL_FIELDS) {
                    SUCCESSFUL_FIELDS.put(runtimeClass, field);
                }
                recordSuccess(field);
                return value;
            }
        }

        CommandHintDiagnostics.recordResolverFailure();
        return null;
    }

    private static GuiTextField read(GuiChat chat, Field field) {
        if (field == null) return null;
        try {
            if (!field.isAccessible()) field.setAccessible(true);
            Object value = field.get(chat);
            return value instanceof GuiTextField ? (GuiTextField) value : null;
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
