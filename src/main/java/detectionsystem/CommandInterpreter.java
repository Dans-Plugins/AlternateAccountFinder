package detectionsystem;

import detectionsystem.AlternateAccountFinder;
import detectionsystem.commands.ListCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandInterpreter {

    AlternateAccountFinder main = null;

    public CommandInterpreter(AlternateAccountFinder plugin) {
        main = plugin;
    }

    public boolean interpretCommand(CommandSender sender, String label, String[] args) {
        switch(label) {
            case "aaflist":
                ListCommand command = new ListCommand(main);
                command.showInfo(sender, args);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "AlternateAccountFinder doesn't know that command");
        }

        return false;
    }
}
