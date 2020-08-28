package detectionsystem.Objects;

import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.UUID;

public class InternetAddressRecord {

    private InetSocketAddress IP = null;
    private HashMap<UUID, Integer> uuids = null;
    private String flag = "none";

    public InternetAddressRecord(Player player) {
        setIP(player.getAddress());
        addUUID(player.getUniqueId());
    }

    public InetSocketAddress getIP() {
        return IP;
    }

    public int getLogins(UUID uuid) {
        return uuids.getOrDefault(uuid, 0);
    }

    public String getFlag() {
        return flag;
    }

    public void setIP(InetSocketAddress newIP) {
        IP = newIP;
    }

    public void setFlag(String s) {
        flag = s;
    }

    public void incrementLogins(Player player) {
        uuids.replace(player.getUniqueId(), uuids.get(player.getUniqueId()) + 1);
    }

    // this should only ever run if a second account logs in using this IP address
    public void addUUID(UUID uuid) {
        if (!uuids.containsKey(uuid)) {
            uuids.put(uuid, 1);
            setFlag("suspected");
        }
        else {
            if (uuids.get(uuid) >= 3) {
                setFlag("probable");
            }
        }
    }
}
