package com.dansplugins.detectionsystem.notifications;

import com.dansplugins.detectionsystem.AlternateAccountFinder;
import dansplugins.mailboxes.Mailboxes;
import dansplugins.mailboxes.externalapi.MailboxesAPI;

import java.util.UUID;

public class MailboxesNotificationService implements NotificationService {

    private final AlternateAccountFinder plugin;

    public MailboxesNotificationService(AlternateAccountFinder plugin) {
        this.plugin = plugin;
    }

    @Override
    public void sendNotification(UUID recipient, String title, String body) {
        Mailboxes mailboxesPlugin = (Mailboxes) plugin.getServer().getPluginManager().getPlugin("Mailboxes");
        if (mailboxesPlugin == null) return;
        MailboxesAPI api = mailboxesPlugin.getAPI();
        api.sendPluginMessageToPlayer(plugin.getName(), recipient, title + " - " + body);
    }
}
