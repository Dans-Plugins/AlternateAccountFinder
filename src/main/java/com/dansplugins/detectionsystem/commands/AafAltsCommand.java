package com.dansplugins.detectionsystem.commands;

import static net.md_5.bungee.api.ChatColor.*;
import static net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND;
import static net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT;

import com.dansplugins.detectionsystem.AlternateAccountFinder;
import com.dansplugins.detectionsystem.logins.LoginService;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
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
            sender.sendMessage("You do not have permission to use this command.");
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

            if (potentialAlts.isEmpty()) {
                sender.sendMessage(RED + "No potential alts found for " + player.getName());
                return;
            }

            sender.sendMessage(WHITE + "Potential alts for " + player.getName() + ":");
            potentialAlts.forEach(uuid -> {
                OfflinePlayer alt = plugin.getServer().getOfflinePlayer(uuid);
                sender.spigot().sendMessage(
                        Stream.of(
                                new ComponentBuilder("• ").color(GRAY).create(),
                                new ComponentBuilder(alt.getName())
                                        .color(alt.isBanned() ? RED : YELLOW)
                                        .event(new HoverEvent(SHOW_TEXT, new Text("Click here to view other IPs for this account")))
                                        .event(new ClickEvent(RUN_COMMAND, "/aaf ips " + alt.getName()))
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
        if (args.length == 0) {
            return Arrays.stream(plugin.getServer().getOfflinePlayers()).map(OfflinePlayer::getName).toList();
        } else if (args.length == 1) {
            return Arrays.stream(plugin.getServer().getOfflinePlayers()).map(OfflinePlayer::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        } else {
            return List.of();
        }
    }
}
