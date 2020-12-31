package dansplugins.detectionsystem;

import dansplugins.detectionsystem.commands.ListCommand;
import dansplugins.detectionsystem.commands.SearchCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandInterpreter {

    private static CommandInterpreter instance;

    private CommandInterpreter() {

    }

    public static CommandInterpreter getInstance() {
        if (instance == null) {
            instance = new CommandInterpreter();
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
            default:
                sender.sendMessage(ChatColor.RED + "AlternateAccountFinder doesn't know that command!");
        }

        return false;
    }
}
