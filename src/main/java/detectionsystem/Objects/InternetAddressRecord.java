package detectionsystem.Objects;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class InternetAddressRecord {

    private InetSocketAddress IP = null;
    private ArrayList<UUID> uuids = new ArrayList<>();
    private HashMap<UUID, Integer> logins = new HashMap<>();
    private String flag = "none";

    public InternetAddressRecord(Player player) {
        setIP(player.getAddress());
        if (!uuids.contains(player.getUniqueId())) {
            uuids.add(player.getUniqueId());
            logins.put(player.getUniqueId(), 1);
        }
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

        // check if two players have logged in at least 3 times, if so flag as probable
        if (getNumUUIDS() > 1) {
            ArrayList<UUID> playersWhoHaveLoggedInAtleastThreeTimes = new ArrayList<>();
            for (UUID uuid : uuids) {
                if (logins.get(uuid) >= 3) {
                    playersWhoHaveLoggedInAtleastThreeTimes.add(uuid);
                }
            }
            if (playersWhoHaveLoggedInAtleastThreeTimes.size() > 1) {
                setFlag("probable");
            }
        }
    }

    // this only ever gets called when a secondary account logs in using this IP address (primary account added in constructor)
    public void addSecondaryUUID(UUID uuid) {
        if (!uuids.contains(uuid)) {
            uuids.add(uuid);
            logins.put(uuid, 1);
            setFlag("suspected");
            System.out.println(IP.getAddress().toString() +  " has been flagged as as suspected. There may be multiple accounts using this IP address.");
        }
    }

    public UUID getPlayerUUIDWithMostLogins() {
        UUID toReturn = null;
        int max = 0;
        for (UUID uuid : uuids) {
            if (logins.get(uuid) > max) {
                max = logins.get(uuid);
                toReturn = uuid;
            }
        }
        return toReturn;
    }

    public String getSecondaryAccountsFormatted() {

        String toReturn = "";

        int counter = 0;
        for (UUID uuid : uuids) {
            if (!uuid.equals(getPlayerUUIDWithMostLogins()))
            toReturn = toReturn + Bukkit.getOfflinePlayer(uuid).getName() + " [" + getLogins(uuid) + "]";
            counter++;
            if (counter < uuids.size() && counter != 1) {
                toReturn = toReturn + ", ";
            }
        }
        return toReturn;
    }

    public int getNumUUIDS() {
        return uuids.size();
    }
}
