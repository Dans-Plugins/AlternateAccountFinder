package detectionsystem.Objects;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class InternetAddressRecord {

    private InetSocketAddress IP = null;
    private ArrayList<UUID> uuids = null;
    private HashMap<UUID, Integer> logins = null;
    private String flag = "none";

    public InternetAddressRecord(Player player) {
        setIP(player.getAddress());
        addUUID(player.getUniqueId());
    }

    public InetSocketAddress getIP() {
        return IP;
    }

    public int getLogins(UUID uuid) {
        return logins.getOrDefault(uuid, 0);
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
        logins.replace(player.getUniqueId(), logins.get(player.getUniqueId()) + 1);
    }

    public void addUUID(UUID uuid) {
        if (!uuids.contains(uuid)) {
            logins.put(uuid, 1);
        }
        else {
            if (logins.get(uuid) >= 3) {
                setFlag("probable");
            }
            else {
                setFlag("suspected");
            }
        }
    }

    public Player getPlayerWithMostLogins() {
        int max = 0;
        Player toReturn = null;
        for (UUID uuid : uuids) {
            if (logins.get(uuid) > max) {
                toReturn = Bukkit.getPlayer(uuid);
            }
        }
        return toReturn;
    }

    public String getSecondaryAccountsFormatted() {

        String toReturn = "";

        Player primary = getPlayerWithMostLogins();

        int counter = 0;
        for (UUID uuid : uuids) {
            if (!uuid.equals(primary.getUniqueId()))
            toReturn = toReturn + Bukkit.getPlayer(uuid).getName() + " [" + getLogins(uuid) + "]";
            counter++;
            if (counter < uuids.size()) {
                toReturn = toReturn + ", ";
            }
        }
        return toReturn;
    }
}
