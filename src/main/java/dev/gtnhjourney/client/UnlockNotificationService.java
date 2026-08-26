package dev.gtnhjourney.client;

import dev.gtnhjourney.diagnostics.JourneyRuntimeCounters;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

/** Client-thread display of one human notification for a newly researched logical item. */
public final class UnlockNotificationService {

    private UnlockNotificationService() {}

    public static void show(String displayName) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.thePlayer == null) return;
        minecraft.thePlayer.addChatMessage(new ChatComponentText(UnlockNotificationText.format(displayName)));
        JourneyRuntimeCounters.unlockNotification();
    }
}
