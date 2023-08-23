package com.dansplugins.detectionsystem.notifications;

import com.dansplugins.detectionsystem.AlternateAccountFinder;
import com.rpkit.core.service.Services;
import com.rpkit.notifications.bukkit.notification.RPKNotificationService;
import com.rpkit.players.bukkit.profile.RPKProfile;
import com.rpkit.players.bukkit.profile.RPKThinProfile;
import com.rpkit.players.bukkit.profile.minecraft.RPKMinecraftProfile;
import com.rpkit.players.bukkit.profile.minecraft.RPKMinecraftProfileService;

import java.util.UUID;

public class RpkNotificationService implements NotificationService {
    private final AlternateAccountFinder plugin;

    public RpkNotificationService(AlternateAccountFinder plugin) {
        this.plugin = plugin;
    }

    @Override
    public void sendNotification(UUID recipient, String title, String body) {
        RPKMinecraftProfileService minecraftProfileService = Services.INSTANCE.get(RPKMinecraftProfileService.class);
        RPKNotificationService notificationService = Services.INSTANCE.get(RPKNotificationService.class);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            RPKMinecraftProfile minecraftProfile = minecraftProfileService.getMinecraftProfile(recipient).join();
            RPKThinProfile thinProfile = minecraftProfile.getProfile();
            if (!(thinProfile instanceof RPKProfile profile)) return;
            notificationService.createNotification(profile, title, body).join();
        });
    }
}
