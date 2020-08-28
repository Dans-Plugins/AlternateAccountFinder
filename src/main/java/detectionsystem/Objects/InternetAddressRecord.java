package detectionsystem.Objects;

import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.UUID;

public class InternetAddressRecord {

    private InetSocketAddress IP = null;
    private HashMap<UUID, Integer> uuids = null;

    public InternetAddressRecord(Player player) {
        IP = player.getAddress();
        if (!uuids.containsKey(player.getUniqueId())) {
            uuids.put(player.getUniqueId(), uuids.get(player.getUniqueId()) + 1);
        }
    }

    // getters

    public InetSocketAddress getIP() {
        return IP;
    }

    public int getLogins(UUID uuid) {
        return uuids.get(uuid);
    }

    // setters

    public void setIP(InetSocketAddress newIP) {
        IP = newIP;
    }

    public void incrementLogins(Player player) {
        uuids.replace(player.getUniqueId(), uuids.get(player.getUniqueId()) + 1);
    }
}
