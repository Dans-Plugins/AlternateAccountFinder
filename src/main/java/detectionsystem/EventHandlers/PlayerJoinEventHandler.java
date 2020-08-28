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
        if (record == null) { // no record of this IP address exists, create one
            InternetAddressRecord newRecord = new InternetAddressRecord(player);
            main.internetAddressRecords.add(newRecord);
        }
        else { // record exists
            if (record.getLogins(player.getUniqueId()) == 0) { // IP address has been used before, but not with this account, add the uuid.
                record.addSecondaryUUID(player.getUniqueId());
            }
            else {
                record.incrementLogins(player); // IP address has been used before with this account, increment logins for this player.
            }
        }
    }
}
