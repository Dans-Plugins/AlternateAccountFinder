package detectionsystem.objects;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import detectionsystem.UUIDChecker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.Type;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InternetAddressRecord {

    private InetAddress IP = null;
    private ArrayList<UUID> uuids = new ArrayList<>();
    private HashMap<UUID, Integer> logins = new HashMap<>();
    private String flag = "none";

    public InternetAddressRecord(Player player) {
        setIP(player.getAddress().getAddress());
        if (!uuids.contains(player.getUniqueId())) {
            uuids.add(player.getUniqueId());
            logins.put(player.getUniqueId(), 1);
        }
    }

    public InternetAddressRecord(Map<String, String> lockedBlockData) {
        this.load(lockedBlockData);
    }

    public InetAddress getIP() {
        return IP;
    }

    public int getLogins(UUID uuid) {
        return logins.getOrDefault(uuid, 0);
    }

    public String getFlag() {
        return flag;
    }

    public void setIP(InetAddress newIP) {
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

    public Map<String, String> save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();;
        Map<String, String> saveMap = new HashMap<>();
        saveMap.put("IP", gson.toJson(IP));
        saveMap.put("uuids", gson.toJson(uuids));
        saveMap.put("logins", gson.toJson(logins));
        saveMap.put("flag", gson.toJson(flag));

        return saveMap;
    }

    private void load(Map<String, String> data) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();;

        Type addressType = new TypeToken<InetAddress>(){}.getType();
        Type arrayListTypeUUID = new TypeToken<ArrayList<UUID>>(){}.getType();
        Type mapType = new TypeToken<HashMap<UUID, Integer>>(){}.getType();
        Type stringType = new TypeToken<String>(){}.getType();


        IP = gson.fromJson(data.get("IP"), addressType);
        uuids = gson.fromJson(data.get("uuids"), arrayListTypeUUID);
        logins = gson.fromJson(data.get("logins"), mapType);
        flag = gson.fromJson(data.get("flag"), stringType);
    }

    public boolean hasPlayerLoggedIn(String playerName) {
        UUID uuid = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(playerName);

        if (uuid == null) {
            return false;
        }

        return uuids.contains(uuid);
    }

    public ArrayList<String> getPlayerNames() {
        ArrayList<String> toReturn = new ArrayList<>();
        for (UUID uuid : uuids) {
            toReturn.add(UUIDChecker.getInstance().findPlayerNameBasedOnUUID(uuid));
        }
        return toReturn;
    }
}
