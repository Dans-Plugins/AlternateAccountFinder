package com.dansplugins.detectionsystem.listeners;

import static org.bukkit.event.EventPriority.MONITOR;

import com.dansplugins.detectionsystem.AlternateAccountFinder;
import com.dansplugins.detectionsystem.logins.LoginService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.UUID;

public final class PlayerJoinListener implements Listener {

    private final AlternateAccountFinder plugin;

    public PlayerJoinListener(AlternateAccountFinder plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            LoginService loginService = plugin.getLoginService();
            loginService.saveLogin(event.getPlayer());
            if (loginService.getLoginCount(event.getPlayer().getUniqueId(), event.getPlayer().getAddress().getAddress()) == 1) {
                List<UUID> potentialAlts = loginService.getPotentialAlts(event.getPlayer().getUniqueId());
                if (potentialAlts.size() > 0) {
                    plugin.getLogger().info("Found potential alts for " + event.getPlayer().getName() + ": " + String.join(", ", potentialAlts.stream().map(uuid -> plugin.getServer().getOfflinePlayer(uuid).getName()).toList()));
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        plugin.getConfig().getStringList("notify-users").forEach(uuidString -> {
                            plugin.getNotificationService().sendNotification(
                                    UUID.fromString(uuidString),
                                    event.getPlayer().getName() + " - potential alts",
                                    event.getPlayer().getName() + " is potentially an alt of: " +
                                            String.join(", ", potentialAlts.stream().map(uuid -> plugin.getServer().getOfflinePlayer(uuid).getName()).toList())
                            );
                        });
                    });
                }
            }
        });
    }
}
