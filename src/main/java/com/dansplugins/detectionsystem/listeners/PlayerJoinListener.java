package com.dansplugins.detectionsystem.listeners;

import static org.bukkit.event.EventPriority.MONITOR;

import com.dansplugins.detectionsystem.AlternateAccountFinder;
import com.dansplugins.detectionsystem.commands.PlayerNames;
import com.dansplugins.detectionsystem.logins.LoginService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;

public final class PlayerJoinListener implements Listener {

    private final AlternateAccountFinder plugin;

    public PlayerJoinListener(AlternateAccountFinder plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Resolve the address on the main thread, where it is reliably non-null: by the
        // time the async task below runs, the player may have already disconnected and
        // Player.getAddress() would then return null (see issue #65).
        if (event.getPlayer().getAddress() == null) {
            plugin.getLogger().warning("No address available for " + event.getPlayer().getName() + " on join; skipping login record.");
            return;
        }
        InetAddress address = event.getPlayer().getAddress().getAddress();
        UUID minecraftUuid = event.getPlayer().getUniqueId();
        String playerName = event.getPlayer().getName();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            LoginService loginService = plugin.getLoginService();
            loginService.saveLogin(minecraftUuid, address);
            if (loginService.getLoginCount(minecraftUuid, address) == 1) {
                List<UUID> potentialAlts = loginService.getPotentialAlts(minecraftUuid);
                if (potentialAlts.size() > 0) {
                    // An account the server has no cached name for is reported as null by Bukkit,
                    // which previously printed the literal string "null" here; fall back to the
                    // account's UUID instead, as the /aaf commands do (see issue #74).
                    String altNames = String.join(", ", potentialAlts.stream()
                            .map(uuid -> PlayerNames.displayName(plugin.getServer().getOfflinePlayer(uuid).getName(), uuid))
                            .toList());
                    plugin.getLogger().info("Found potential alts for " + playerName + ": " + altNames);
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        List<UUID> recipients = parseValidUuids(plugin.getConfig().getStringList("notify-users"), plugin.getLogger());
                        recipients.forEach(recipient -> {
                            plugin.getNotificationService().sendNotification(
                                    recipient,
                                    playerName + " - potential alts",
                                    playerName + " is potentially an alt of: " + altNames
                            );
                        });
                    });
                }
            }
        });
    }

    /**
     * Parses each entry of {@code notify-users} as a UUID, skipping and warning about any
     * entry that isn't a valid UUID string instead of letting one bad entry abort the rest
     * (see issue #66).
     */
    static List<UUID> parseValidUuids(List<String> uuidStrings, Logger logger) {
        return uuidStrings.stream()
                .flatMap(uuidString -> {
                    try {
                        return Stream.of(UUID.fromString(uuidString));
                    } catch (IllegalArgumentException e) {
                        logger.warning("Skipping invalid notify-users entry \"" + uuidString + "\": not a valid UUID.");
                        return Stream.<UUID>empty();
                    }
                })
                .toList();
    }
}
