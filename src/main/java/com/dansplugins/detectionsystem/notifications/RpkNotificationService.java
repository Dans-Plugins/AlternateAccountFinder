package com.dansplugins.detectionsystem.notifications;

import com.dansplugins.detectionsystem.AlternateAccountFinder;
import com.rpkit.core.service.Services;
import com.rpkit.notifications.bukkit.notification.RPKNotificationService;
import com.rpkit.players.bukkit.profile.RPKProfile;
import com.rpkit.players.bukkit.profile.RPKThinProfile;
import com.rpkit.players.bukkit.profile.minecraft.RPKMinecraftProfile;
import com.rpkit.players.bukkit.profile.minecraft.RPKMinecraftProfileService;

import java.util.UUID;
import java.util.logging.Logger;

public class RpkNotificationService implements NotificationService {
    private final AlternateAccountFinder plugin;

    public RpkNotificationService(AlternateAccountFinder plugin) {
        this.plugin = plugin;
    }

    @Override
    public void sendNotification(UUID recipient, String title, String body) {
        RPKMinecraftProfileService minecraftProfileService = Services.INSTANCE.get(RPKMinecraftProfileService.class);
        RPKNotificationService notificationService = Services.INSTANCE.get(RPKNotificationService.class);
        Logger logger = plugin.getLogger();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                deliverNotification(minecraftProfileService, notificationService, recipient, title, body, logger));
    }

    /**
     * Delivers one notification through RPKit, skipping the recipient with a warning rather than
     * throwing when RPKit cannot supply what the delivery needs (see issue #106).
     *
     * <p>Both services come from {@code Services.INSTANCE.get(...)}, which answers with nothing
     * while RPKit is still starting up or when the notification library is installed without the
     * player library, and {@code getMinecraftProfile} completes with nothing for a UUID RPKit has
     * never seen — a {@code notify-users} entry that has not joined, or one that predates the
     * RPKit installation. This runs on an asynchronous task, so an unguarded dereference surfaces
     * as a bare stack trace naming no configuration entry.
     *
     * <p>Package-private and taking its collaborators as parameters so the skip branches can be
     * covered without a running server; the warnings name the recipient account only, never an
     * address.
     */
    static void deliverNotification(RPKMinecraftProfileService minecraftProfileService,
                                    RPKNotificationService notificationService,
                                    UUID recipient,
                                    String title,
                                    String body,
                                    Logger logger) {
        if (minecraftProfileService == null) {
            logger.warning("RPKit's Minecraft profile service is unavailable; skipping alt notification for " + recipient + ".");
            return;
        }
        if (notificationService == null) {
            logger.warning("RPKit's notification service is unavailable; skipping alt notification for " + recipient + ".");
            return;
        }
        RPKMinecraftProfile minecraftProfile = minecraftProfileService.getMinecraftProfile(recipient).join();
        if (minecraftProfile == null) {
            logger.warning("RPKit has no Minecraft profile for " + recipient + "; skipping alt notification. Check that this notify-users entry is an account RPKit knows about.");
            return;
        }
        RPKThinProfile thinProfile = minecraftProfile.getProfile();
        if (!(thinProfile instanceof RPKProfile profile)) return;
        notificationService.createNotification(profile, title, body).join();
    }
}
