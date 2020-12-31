package dansplugins.detectionsystem.commands;

import dansplugins.detectionsystem.data.PersistentData;
import dansplugins.detectionsystem.objects.InternetAddressRecord;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ListCommand {

    public void showInfo(CommandSender sender, String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("aaf.list")) {
                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("suspected")) {
                        listSuspected(player);
                        return;
                    }

                    if (args[0].equalsIgnoreCase("probable")) {
                        listProbable(player);
                        return;
                    }

                    if (args[0].equalsIgnoreCase("addresses")) {
                        listAddresses(player);
                    }

                }
                else {
                    player.sendMessage(ChatColor.RED + "Usage: /aaflist [suspected | probable | addresses]");
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "Sorry! In order to use this command, you need the following permission: 'aaf.list'");
            }
        }

    }

    public void listSuspected(Player player) {
        int count = 0;
        for (InternetAddressRecord record : PersistentData.getInstance().getInternetAddressRecords()) {
            OfflinePlayer primary = Bukkit.getOfflinePlayer(record.getPlayerUUIDWithMostLogins());
            if (record.getFlag().equalsIgnoreCase("suspected")) {
                player.sendMessage(ChatColor.AQUA + "" + primary.getName() + " [" + record.getLogins(primary.getUniqueId()) + "] may have the following alternate accounts: "
                        + record.getSecondaryAccountsFormatted());
                count++;
            }
        }
        if (count == 0) {
            player.sendMessage(ChatColor.GREEN + "No suspected alternate accounts!");
        }
    }

    public void listProbable(Player player) {
        int count = 0;
        for (InternetAddressRecord record : PersistentData.getInstance().getInternetAddressRecords()) {
            OfflinePlayer primary = Bukkit.getOfflinePlayer(record.getPlayerUUIDWithMostLogins());
            if (record.getFlag().equalsIgnoreCase("probable")) {
                player.sendMessage(ChatColor.AQUA + "" + primary.getName() + " [" + record.getLogins(primary.getUniqueId()) + "] likely has the following alternate accounts: "
                        + record.getSecondaryAccountsFormatted());
                count++;
            }
        }
        if (count == 0) {
            player.sendMessage(ChatColor.GREEN + "No probable alternate accounts!");
        }
    }

    public void listAddresses(Player player) {
        for (InternetAddressRecord record : PersistentData.getInstance().getInternetAddressRecords()) {
            player.sendMessage(ChatColor.AQUA + "" + record.getIP().toString().substring(1));
        }
    }

}
