package dansplugins.detectionsystem.services;

import dansplugins.detectionsystem.commands.ListCommand;
import dansplugins.detectionsystem.commands.SearchCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class LocalCommandService {

    private static LocalCommandService instance;

    private LocalCommandService() {

    }

    public static LocalCommandService getInstance() {
        if (instance == null) {
            instance = new LocalCommandService();
        }
        return instance;
    }

    public boolean interpretCommand(CommandSender sender, String label, String[] args) {
        switch(label) {
            case "aaflist":
                ListCommand listCommand = new ListCommand();
                listCommand.showInfo(sender, args);
                break;
            case "aafsearch":
                SearchCommand searchCommand = new SearchCommand();
                searchCommand.searchForPlayer(sender, args);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "AlternateAccountFinder doesn't know that command!");
        }

        return false;
    }
}
