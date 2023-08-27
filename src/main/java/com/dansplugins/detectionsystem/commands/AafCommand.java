package com.dansplugins.detectionsystem.commands;

import com.dansplugins.detectionsystem.AlternateAccountFinder;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AafCommand implements CommandExecutor, TabCompleter {

    private final AafAccountsCommand accountsCommand;
    private final AafIpsCommand ipsCommand;
    private final AafAltsCommand altsCommand;

    private final List<String> accountsAliases = List.of("accounts");
    private final List<String> ipsAliases = List.of("ips");
    private final List<String> altsAliases = List.of("alts");

    private final List<String> subcommands = new ArrayList<>() {{
        addAll(accountsAliases);
        addAll(ipsAliases);
        addAll(altsAliases);
    }};

    public AafCommand(AlternateAccountFinder plugin) {
        this.accountsCommand = new AafAccountsCommand(plugin);
        this.ipsCommand = new AafIpsCommand(plugin);
        this.altsCommand = new AafAltsCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /aaf [" + String.join(", ", subcommands) + "]");
            return true;
        }

        if (accountsAliases.contains(args[0].toLowerCase())) {
            return accountsCommand.onCommand(sender, command, label, Arrays.stream(args).skip(1).toArray(String[]::new));
        } else if (ipsAliases.contains(args[0].toLowerCase())) {
            return ipsCommand.onCommand(sender, command, label, Arrays.stream(args).skip(1).toArray(String[]::new));
        } else if (altsAliases.contains(args[0].toLowerCase())) {
            return altsCommand.onCommand(sender, command, label, Arrays.stream(args).skip(1).toArray(String[]::new));
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /aaf [" + String.join(", ", subcommands) + "]");
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return subcommands;
        } else if (args.length == 1) {
            return subcommands.stream()
                    .filter(subcommand -> subcommand.startsWith(args[0].toLowerCase()))
                    .toList();
        } else {
            if (accountsAliases.contains(args[0].toLowerCase())) {
                return accountsCommand.onTabComplete(sender, command, label, Arrays.stream(args).skip(1).toArray(String[]::new));
            } else if (ipsAliases.contains(args[0].toLowerCase())) {
                return ipsCommand.onTabComplete(sender, command, label, Arrays.stream(args).skip(1).toArray(String[]::new));
            } else if (altsAliases.contains(args[0].toLowerCase())) {
                return altsCommand.onTabComplete(sender, command, label, Arrays.stream(args).skip(1).toArray(String[]::new));
            } else {
                return List.of();
            }
        }
    }
}
