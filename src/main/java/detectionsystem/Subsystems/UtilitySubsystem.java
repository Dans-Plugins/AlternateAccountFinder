package detectionsystem.Subsystems;

import detectionsystem.AlternateAccountFinder;
import detectionsystem.Objects.InternetAddressRecord;

import java.net.InetSocketAddress;

public class UtilitySubsystem {

    AlternateAccountFinder main = null;

    public UtilitySubsystem(AlternateAccountFinder plugin) {
        main = plugin;
    }

    public InternetAddressRecord getInternetAddressRecord(InetSocketAddress IP) {
        for (InternetAddressRecord record : main.internetAddressRecords) {
            if (record.getIP().getAddress().equals(record.getIP().getAddress())) {
                return record;
            }
        }
        return null;
    }

}
