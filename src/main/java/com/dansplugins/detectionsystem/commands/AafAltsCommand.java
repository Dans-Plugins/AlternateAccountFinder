package com.dansplugins.detectionsystem.commands;

import static net.md_5.bungee.api.ChatColor.*;

import com.dansplugins.detectionsystem.AlternateAccountFinder;
import com.dansplugins.detectionsystem.logins.LoginService;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public final class AafAltsCommand implements CommandExecutor, TabCompleter {

    private final AlternateAccountFinder plugin;

    public AafAltsCommand(AlternateAccountFinder plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aaf.alts")) {
            sender.sendMessage(RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(RED + "Usage: /aaf alts [player]");
            return true;
        }

        OfflinePlayer player = plugin.getServer().getOfflinePlayer(args[0]);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            LoginService loginService = plugin.getLoginService();
            List<UUID> potentialAlts = loginService.getPotentialAlts(player.getUniqueId());
            String playerName = PlayerNames.displayName(player.getName(), player.getUniqueId());

            if (potentialAlts.isEmpty()) {
                sender.sendMessage(RED + "No potential alts found for " + playerName);
                return;
            }

            sender.sendMessage(WHITE + "Potential alts for " + playerName + ":");
            potentialAlts.forEach(uuid -> {
                OfflinePlayer alt = plugin.getServer().getOfflinePlayer(uuid);
                sender.spigot().sendMessage(
                        Stream.of(
                                new ComponentBuilder("• ").color(GRAY).create(),
                                new ComponentBuilder(PlayerNames.displayName(alt.getName(), uuid))
                                        .color(alt.isBanned() ? RED : YELLOW)
                                        .create()
                        ).flatMap(Arrays::stream).toArray(BaseComponent[]::new)
                );
            });
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aaf.alts")) {
            return List.of();
        }
        if (args.length > 1) {
            return List.of();
        }
        // Names are collected before filtering because an account the server has no cached name
        // for yields null here; PlayerNames skips those instead of throwing (see issue #74).
        List<String> names = Arrays.stream(plugin.getServer().getOfflinePlayers())
                .map(OfflinePlayer::getName)
                .toList();
        return PlayerNames.matchingNames(names, args.length == 0 ? "" : args[0]);
    }
}
