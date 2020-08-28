package detectionsystem.EventHandlers;

import detectionsystem.AlternateAccountFinder;
import detectionsystem.Objects.InternetAddressRecord;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinEventHandler {

    AlternateAccountFinder main = null;

    public PlayerJoinEventHandler(AlternateAccountFinder plugin) {
        main = plugin;
    }

    public void handle(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        InternetAddressRecord record = main.utilities.getInternetAddressRecord(player.getAddress());
        if (record == null) {
            InternetAddressRecord newRecord = new InternetAddressRecord(player);
            main.internetAddressRecords.add(newRecord);
        }
        else {
            record.incrementLogins(event.getPlayer());
        }
    }
}
