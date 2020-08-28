package detectionsystem.Subsystems;

import detectionsystem.AlternateAccountFinder;
import org.bukkit.command.CommandSender;

public class CommandSubsystem {

    AlternateAccountFinder main = null;

    public CommandSubsystem(AlternateAccountFinder plugin) {
        main = plugin;
    }

    public boolean interpretCommand(CommandSender sender, String label, String[] args) {


        return false;
    }
}
