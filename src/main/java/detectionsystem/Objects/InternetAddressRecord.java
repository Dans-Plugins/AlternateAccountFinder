package detectionsystem.Objects;

import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.UUID;

public class InternetAddressRecord {

    private InetSocketAddress IP = null;
    private HashMap<UUID, Integer> uuids = null;

    public InternetAddressRecord(Player player) {
        setIP(player.getAddress());
        addUUID(player.getUniqueId());
    }

    // getters

    public InetSocketAddress getIP() {
        return IP;
    }

    public int getLogins(UUID uuid) {
        return uuids.getOrDefault(uuid, 0);
    }

    // setters

    public void setIP(InetSocketAddress newIP) {
        IP = newIP;
    }

    public void incrementLogins(Player player) {
        uuids.replace(player.getUniqueId(), uuids.get(player.getUniqueId()) + 1);
    }

    public void addUUID(UUID uuid) {
        if (!uuids.containsKey(uuid)) {
            uuids.put(uuid, 1);
        }
    }
}
