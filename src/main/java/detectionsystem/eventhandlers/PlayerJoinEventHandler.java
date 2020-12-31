package detectionsystem.eventhandlers;

import detectionsystem.AlternateAccountFinder;
import detectionsystem.objects.InternetAddressRecord;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinEventHandler {

    AlternateAccountFinder main = null;

    public PlayerJoinEventHandler(AlternateAccountFinder plugin) {
        main = plugin;
    }

    public void handle(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        InternetAddressRecord record = main.utilities.getInternetAddressRecord(player.getAddress().getAddress());
        if (record == null) { // no record of this IP address exists, create one
            System.out.println(player.getName() + " has joined with a new IP address. Creating record.");
            InternetAddressRecord newRecord = new InternetAddressRecord(player);
            main.internetAddressRecords.add(newRecord);
        }
        else { // record exists
            if (record.getLogins(player.getUniqueId()) == 0) { // IP address has been used before, but not with this account, add the uuid.
                System.out.println(player.getName() + " has joined with an IP address that has been used before. Altering record.");
                record.addSecondaryUUID(player.getUniqueId());
            }
            else {
                record.incrementLogins(player); // IP address has been used before with this account, increment logins for this player.

                if (record.getNumUUIDS() > 2) {
                    System.out.println("[ALERT] More than two accounts has joined using the IP address that " + player.getName() + " just logged in with.");
                }

                if (record.getNumUUIDS() > 1) {
                    System.out.println("[ALERT] More than one account has joined using the IP address that " + player.getName() + " just logged in with.");
                }
            }
        }
    }
}
