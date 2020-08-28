package detectionsystem.Commands;

import detectionsystem.AlternateAccountFinder;
import detectionsystem.Objects.InternetAddressRecord;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ListCommand {

    AlternateAccountFinder main = null;

    public ListCommand(AlternateAccountFinder plugin) {
        main = plugin;
    }

    public void showInfo(CommandSender sender, String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("aaf.list")) {
                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("suspected")) {
                        for (InternetAddressRecord record : main.internetAddressRecords) {
                            Player primary = record.getPlayerWithMostLogins();
                            if (record.getFlag().equalsIgnoreCase("suspected")) {
                                player.sendMessage(ChatColor.AQUA + "" + primary.getName() + "[" + record.getLogins(primary.getUniqueId()) + "] may have the following alternate accounts: "
                                + record.getSecondaryAccountsFormatted());
                            }
                        }
                    }

                    if (args[0].equalsIgnoreCase("probable")) {
                        for (InternetAddressRecord record : main.internetAddressRecords) {
                            Player primary = record.getPlayerWithMostLogins();
                            if (record.getFlag().equalsIgnoreCase("probable")) {
                                player.sendMessage(ChatColor.AQUA + "" + primary.getName() + "[" + record.getLogins(primary.getUniqueId()) + "] likely has the following alternate accounts: "
                                        + record.getSecondaryAccountsFormatted());
                            }
                        }
                    }

                    if (args[0].equalsIgnoreCase("addresses")) {
                        for (InternetAddressRecord record : main.internetAddressRecords) {
                            player.sendMessage(ChatColor.AQUA + "" + record.getIP().toString());
                        }
                    }

                }
            }
            else {
                player.sendMessage(ChatColor.RED + "Sorry! In order to use this command, you need the following permission: 'aaf.list'");
            }
        }

    }

}
