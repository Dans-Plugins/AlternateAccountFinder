package dansplugins.detectionsystem.services;

import dansplugins.detectionsystem.commands.ListCommand;
import dansplugins.detectionsystem.commands.SearchCommand;
import dansplugins.detectionsystem.data.PersistentData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandService {
    private final PersistentData persistentData;

    public CommandService(PersistentData persistentData) {
        this.persistentData = persistentData;
    }

    public boolean interpretCommand(CommandSender sender, String label, String[] args) {
        switch(label) {
            case "aaflist":
                ListCommand listCommand = new ListCommand(persistentData);
                listCommand.showInfo(sender, args);
                break;
            case "aafsearch":
                SearchCommand searchCommand = new SearchCommand(persistentData);
                searchCommand.searchForPlayer(sender, args);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "AlternateAccountFinder doesn't know that command!");
        }

        return false;
    }
}
