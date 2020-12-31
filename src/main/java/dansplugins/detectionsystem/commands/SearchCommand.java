package dansplugins.detectionsystem.commands;

import dansplugins.detectionsystem.data.PersistentData;
import dansplugins.detectionsystem.objects.InternetAddressRecord;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

public class SearchCommand {

    public void searchForPlayer(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aaf.search")) {
            sender.sendMessage(ChatColor.RED + "In order to use this command, you need the following permission: 'aaf.search'");
            return;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /aafsearch (playername)");
            return;
        }

        String playerName = args[0];

        // search
        ArrayList<InternetAddressRecord> list = PersistentData.getInstance().getInternetAddressRecordsAssociatedWithPlayer(playerName);

        sendPlayerInfo(sender, list, playerName);
    }

    private void sendPlayerInfo(CommandSender sender, ArrayList<InternetAddressRecord> list, String playerName) {

        if (list.size() == 0) {
            sender.sendMessage(ChatColor.RED + "That player has no suspicious activity.");
            return;
        }

        sender.sendMessage(ChatColor.AQUA + "\n == Search Results for '" + playerName + "' == ");
        sender.sendMessage(ChatColor.AQUA + "IP addresses used: " + list.size());
        sender.sendMessage(ChatColor.AQUA + "Potential Alts: " + getPotentialAltsFormatted(playerName, list));

    }

    private String getPotentialAltsFormatted(String playerName, ArrayList<InternetAddressRecord> list) {

        String potentialAltsFormatted = "";

        for (InternetAddressRecord record : list) {

            ArrayList<String> playerNames = record.getPlayerNames();

            for (String name : playerNames) {
                if (!name.equalsIgnoreCase(playerName) && !potentialAltsFormatted.contains(name)) {
                    if (potentialAltsFormatted.equalsIgnoreCase("")) {
                        potentialAltsFormatted = potentialAltsFormatted + name;
                    }
                    else {
                        potentialAltsFormatted = potentialAltsFormatted + ", " + name;
                    }
                }
            }

        }
        if (potentialAltsFormatted.equalsIgnoreCase("")) {
            return "none";
        }
        return potentialAltsFormatted;
    }

}
