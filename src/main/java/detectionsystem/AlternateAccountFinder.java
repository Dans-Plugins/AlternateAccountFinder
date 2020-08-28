package detectionsystem;

import detectionsystem.EventHandlers.PlayerJoinEventHandler;
import detectionsystem.Objects.InternetAddressRecord;
import detectionsystem.Subsystems.CommandSubsystem;
import detectionsystem.Subsystems.UtilitySubsystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public final class AlternateAccountFinder extends JavaPlugin implements Listener {

    // subsystems
    private CommandSubsystem commandInterpreter = new CommandSubsystem(this);
    public UtilitySubsystem utilities = new UtilitySubsystem(this);

    // saved
    public ArrayList<InternetAddressRecord> internetAddressRecords = new ArrayList<>();

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {

    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return commandInterpreter.interpretCommand(sender, label, args);
    }

    @EventHandler()
    public void onPlayerJoin(PlayerJoinEvent event) {
        PlayerJoinEventHandler handler = new PlayerJoinEventHandler(this);
        handler.handle(event);
    }
}
