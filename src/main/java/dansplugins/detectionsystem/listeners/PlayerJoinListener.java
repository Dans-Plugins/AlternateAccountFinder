package dansplugins.detectionsystem.listeners;

import dansplugins.detectionsystem.data.PersistentData;
import dansplugins.detectionsystem.objects.InternetAddressRecord;
import dansplugins.detectionsystem.utils.UUIDChecker;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener {
    private final PersistentData persistentData;
    private final UUIDChecker uuidChecker;

    public PlayerJoinListener(PersistentData persistentData, UUIDChecker uuidChecker) {
        this.persistentData = persistentData;
        this.uuidChecker = uuidChecker;
    }

    public void handle(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        InternetAddressRecord record = persistentData.getInternetAddressRecord(player.getAddress().getAddress());
        if (record == null) { // no record of this IP address exists, create one
            InternetAddressRecord newRecord = new InternetAddressRecord(player, uuidChecker);
            persistentData.getInternetAddressRecords().add(newRecord);
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
