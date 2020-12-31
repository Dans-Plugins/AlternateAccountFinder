package dansplugins.detectionsystem;

import dansplugins.detectionsystem.bstats.Metrics;
import dansplugins.detectionsystem.eventhandlers.PlayerJoinEventHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class AlternateAccountFinder extends JavaPlugin implements Listener {

    private static AlternateAccountFinder instance;

    @Override
    public void onEnable() {
        instance = this;
        StorageManager.getInstance().load();
        this.getServer().getPluginManager().registerEvents(this, this);

        int pluginId = 9834;
        Metrics metrics = new Metrics(this, pluginId);
    }

    @Override
    public void onDisable() {
        StorageManager.getInstance().save();
    }

    public static AlternateAccountFinder getInstance() {
        return instance;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return CommandInterpreter.getInstance().interpretCommand(sender, label, args);
    }

    @EventHandler()
    public void onPlayerJoin(PlayerJoinEvent event) {
        PlayerJoinEventHandler handler = new PlayerJoinEventHandler(this);
        handler.handle(event);
    }
}
