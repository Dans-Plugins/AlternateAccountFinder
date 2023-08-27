package com.dansplugins.detectionsystem.notifications;

import static net.md_5.bungee.api.ChatColor.GRAY;
import static net.md_5.bungee.api.ChatColor.WHITE;

import com.dansplugins.detectionsystem.AlternateAccountFinder;
import org.bukkit.entity.Player;

import java.util.UUID;

public class MessageNotificationService implements NotificationService {
    private final AlternateAccountFinder plugin;

    public MessageNotificationService(AlternateAccountFinder plugin) {
        this.plugin = plugin;
    }

    @Override
    public void sendNotification(UUID recipient, String title, String body) {
        Player player = plugin.getServer().getPlayer(recipient);
        if (player != null) {
            player.sendMessage(WHITE + title);
            player.sendMessage(GRAY + body);
        }
    }
}
