package com.dansplugins.detectionsystem.commands;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static net.md_5.bungee.api.ChatColor.*;
import static net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND;
import static net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT;

import com.dansplugins.detectionsystem.AlternateAccountFinder;
import com.dansplugins.detectionsystem.logins.AccountAddressInfo;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public final class AafIpsCommand implements CommandExecutor, TabCompleter {

    private final AlternateAccountFinder plugin;

    public AafIpsCommand(AlternateAccountFinder plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aaf.ips")) {
            sender.sendMessage(RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(RED + "Usage: /aaf ips [player]");
            return true;
        }

        OfflinePlayer player = plugin.getServer().getOfflinePlayer(args[0]);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            LoginService loginService = plugin.getLoginService();
            AccountAddressInfo accountAddressInfo = loginService.getAccountInfo(player.getUniqueId());

            List<InetAddress> addresses = accountAddressInfo.getAddresses();
            if (addresses.isEmpty()) {
                sender.sendMessage(RED + "No IP addresses found for " + player.getName());
                return;
            }

            sender.sendMessage(WHITE + "Addresses for " + player.getName() + ":");
            addresses.forEach(ip -> {
                AddressInfo addressInfo = accountAddressInfo.getAddressInfo(ip);
                sender.spigot().sendMessage(
                        Stream.of(
                            new ComponentBuilder("• ").color(GRAY).create(),
                            new ComponentBuilder(ip.getHostAddress())
                                    .color(plugin.getServer().getIPBans().contains(ip.getHostAddress()) ? RED : YELLOW)
                                    .event(new HoverEvent(SHOW_TEXT, new Text("Click here to view other accounts for this IP address")))
                                    .event(new ClickEvent(RUN_COMMAND, "/aaf accounts " + ip.getHostAddress()))
                                    .create(),
                                new ComponentBuilder(" (" + addressInfo.getLogins() + " logins, first login "
                                        + ISO_LOCAL_DATE_TIME.format(addressInfo.getFirstLogin())
                                        + ", last login " + ISO_LOCAL_DATE_TIME.format(addressInfo.getLastLogin())
                                        + ")").color(GRAY).create()
                        ).flatMap(Arrays::stream).toArray(BaseComponent[]::new)
                );
            });
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aaf.ips")) {
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

