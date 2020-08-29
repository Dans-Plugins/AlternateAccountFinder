package detectionsystem.Subsystems;

import detectionsystem.AlternateAccountFinder;
import detectionsystem.Objects.InternetAddressRecord;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class UtilitySubsystem {

    AlternateAccountFinder main = null;

    public UtilitySubsystem(AlternateAccountFinder plugin) {
        main = plugin;
    }

    public InternetAddressRecord getInternetAddressRecord(InetAddress IP) {
        for (InternetAddressRecord record : main.internetAddressRecords) {
            if (record.getIP().equals(IP)) {
                return record;
            }
        }
        return null;
    }

}
