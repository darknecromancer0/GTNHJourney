package dev.gtnhjourney.safety;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.world.Explosion;
import net.minecraftforge.event.world.ExplosionEvent;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import dev.gtnhjourney.command.JourneyAdminPermissionPolicy;
import dev.gtnhjourney.config.JourneyConfig;

/** Optional global Forge explosion blocker. Cancellation is unconditional while the feature is disabled. */
public final class ExplosionGuard {

    private final ExplosionNotificationThrottle throttle;
    private final ExplosionSourceResolver resolver;

    public ExplosionGuard() {
        this(new ExplosionNotificationThrottle(5000L), new ExplosionSourceResolver());
    }

    ExplosionGuard(ExplosionNotificationThrottle throttle, ExplosionSourceResolver resolver) {
        this.throttle = throttle;
        this.resolver = resolver;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onExplosionStart(final ExplosionEvent.Start event) {
        try {
            cancelBeforeDiagnostics(event, JourneyConfig.explosionsEnabled(), new Runnable() {

                @Override
                public void run() {
                    notifyOperators(event);
                }
            });
        } catch (RuntimeException failure) {
            FMLLog.warning("GTNH Journey cancelled an explosion but diagnostics failed: %s", safeMessage(failure));
        } catch (LinkageError failure) {
            FMLLog.warning("GTNH Journey cancelled an explosion but diagnostics failed: %s", safeMessage(failure));
        }
    }

    static void cancelBeforeDiagnostics(ExplosionEvent.Start event, boolean explosionsEnabled, Runnable diagnostics) {
        if (event == null || explosionsEnabled) return;
        event.setCanceled(true);
        if (diagnostics != null) diagnostics.run();
    }

    public void reset() {
        throttle.reset();
    }

    @SuppressWarnings("rawtypes")
    private void notifyOperators(ExplosionEvent.Start event) {
        ExplosionNotificationThrottle.Decision decision = throttle.record(System.currentTimeMillis());
        if (!decision.shouldNotify()) return;

        Explosion explosion = event.explosion;
        String source = resolver.describe(event.world, explosion);
        int x = explosion == null ? 0 : MathHelper.floor_double(explosion.explosionX);
        int y = explosion == null ? 0 : MathHelper.floor_double(explosion.explosionY);
        int z = explosion == null ? 0 : MathHelper.floor_double(explosion.explosionZ);

        StringBuilder message = new StringBuilder("GTNH Journey: Explosion [")
            .append(source)
            .append("] cancelled at ")
            .append(x)
            .append(' ')
            .append(y)
            .append(' ')
            .append(z)
            .append('.');
        if (decision.suppressedBeforeThis() > 0) {
            message.append(" (+")
                .append(decision.suppressedBeforeThis())
                .append(" more cancelled)");
        }

        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) return;
        List players = server.getConfigurationManager().playerEntityList;
        for (Object value : players) {
            if (!(value instanceof EntityPlayerMP)) continue;
            EntityPlayerMP player = (EntityPlayerMP) value;
            if (JourneyAdminPermissionPolicy.mayMutate(player)) {
                player.addChatMessage(new ChatComponentText(message.toString()));
            }
        }
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.length() == 0 ? failure.getClass().getSimpleName() : message;
    }
}
