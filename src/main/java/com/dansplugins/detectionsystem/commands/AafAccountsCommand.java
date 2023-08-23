package com.dansplugins.detectionsystem.commands;

import static net.md_5.bungee.api.ChatColor.*;
import static net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND;
import static net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT;

import com.dansplugins.detectionsystem.AlternateAccountFinder;
import com.dansplugins.detectionsystem.logins.AddressInfo;
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public final class AafAccountsCommand implements CommandExecutor, TabCompleter {

    private final AlternateAccountFinder plugin;

    public AafAccountsCommand(AlternateAccountFinder plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aaf.accounts")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(RED + "Usage: /aaf accounts [ip]");
            return true;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            InetAddress ip;
            try {
                ip = InetAddress.getByName(args[0]);
            } catch (UnknownHostException e) {
                sender.sendMessage(RED + "Invalid IP address.");
                return;
            }

            LoginService loginService = plugin.getLoginService();
            AddressInfo addressInfo = loginService.getAddressInfo(ip);

            sender.sendMessage(WHITE + "Accounts for " + ip.getHostAddress() + ":");
            addressInfo.getAccounts().forEach(uuid -> {
                OfflinePlayer player = plugin.getServer().getOfflinePlayer(uuid);
                sender.spigot().sendMessage(
                        Stream.of(
                                new ComponentBuilder("• ").color(GRAY).create(),
                                new ComponentBuilder(player.getName())
                                        .color(YELLOW)
                                        .event(new HoverEvent(SHOW_TEXT, new Text("Click here to view other IPs for this account")))
                                        .event(new ClickEvent(RUN_COMMAND, "/aaf ips " + player.getName()))
                                        .create(),
                                new ComponentBuilder(" (" + addressInfo.getLogins(uuid) + " logins)").color(GRAY).create()
                        ).flatMap(Arrays::stream).toArray(BaseComponent[]::new)
                );
            });
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return plugin.getServer().getOnlinePlayers().stream().map(player -> player.getAddress().getAddress().getHostAddress()).toList();
        } else if (args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(player -> player.getAddress().getAddress().getHostAddress())
                    .filter(ip -> ip.startsWith(args[0]))
                    .toList();
        } else {
            return List.of();
        }
    }
}
