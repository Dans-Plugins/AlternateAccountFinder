package detectionsystem;

import detectionsystem.Subsystems.CommandSubsystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class AlternateAccountFinder extends JavaPlugin {

    CommandSubsystem commandInterpreter = new CommandSubsystem(this);

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return commandInterpreter.interpretCommand(sender, label, args);
    }

}
