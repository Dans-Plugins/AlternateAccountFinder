package dansplugins.detectionsystem;

import dansplugins.detectionsystem.bstats.Metrics;
import dansplugins.detectionsystem.data.PersistentData;
import dansplugins.detectionsystem.eventhandlers.PlayerJoinEventHandler;
import dansplugins.detectionsystem.services.CommandService;
import dansplugins.detectionsystem.services.StorageService;
import dansplugins.detectionsystem.utils.UUIDChecker;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class AlternateAccountFinder extends JavaPlugin implements Listener {
    private final PersistentData persistentData = new PersistentData();
    private final UUIDChecker uuidChecker = new UUIDChecker();
    private final StorageService storageService = new StorageService(persistentData, uuidChecker);
    private final CommandService commandService = new CommandService(persistentData);

    @Override
    public void onEnable() {
        storageService.load();
        this.getServer().getPluginManager().registerEvents(this, this);

        int pluginId = 9834;
        Metrics metrics = new Metrics(this, pluginId);
    }

    @Override
    public void onDisable() {
        storageService.save();
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return commandService.interpretCommand(sender, label, args);
    }

    @EventHandler()
    public void onPlayerJoin(PlayerJoinEvent event) {
        PlayerJoinEventHandler handler = new PlayerJoinEventHandler(persistentData, uuidChecker);
        handler.handle(event);
    }
}
