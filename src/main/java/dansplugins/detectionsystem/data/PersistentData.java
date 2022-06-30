package dansplugins.detectionsystem.data;

import dansplugins.detectionsystem.objects.InternetAddressRecord;

import java.net.InetAddress;
import java.util.ArrayList;

public class PersistentData {
    private final ArrayList<InternetAddressRecord> internetAddressRecords = new ArrayList<>();

    public ArrayList<InternetAddressRecord> getInternetAddressRecords() {
        return internetAddressRecords;
    }

    public InternetAddressRecord getInternetAddressRecord(InetAddress IP) {
        for (InternetAddressRecord record : getInternetAddressRecords()) {
            if (record.getIP().equals(IP)) {
                return record;
            }
        }
        return null;
    }

    public ArrayList<InternetAddressRecord> getInternetAddressRecordsAssociatedWithPlayer(String playerName) {

        ArrayList<InternetAddressRecord> toReturn = new ArrayList<>();

        for (InternetAddressRecord record : getInternetAddressRecords()) {
            if (record.hasPlayerLoggedIn(playerName)) {
                toReturn.add(record);
            }
        }

        return toReturn;
    }
}
