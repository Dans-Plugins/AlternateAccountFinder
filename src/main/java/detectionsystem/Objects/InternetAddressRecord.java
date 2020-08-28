package detectionsystem.Objects;

import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.UUID;

public class InternetAddressRecord {

    private InetSocketAddress IP = null;
    private UUID uuid = null;
    private int logins = 0;

    public InternetAddressRecord(Player player) {
        IP = player.getAddress();
        uuid = player.getUniqueId();
        logins = 1;
    }

    // getters

    public InetSocketAddress getIP() {
        return IP;
    }

    public UUID getUUID() {
        return uuid;
    }

    public int getLogins() {
        return logins;
    }

    // setters

    public void setIP(InetSocketAddress newIP) {
        IP = newIP;
    }

    public void setUUID(UUID newUUID) {
        uuid = newUUID;
    }

    public void incrementLogins() {
        logins++;
    }
}
