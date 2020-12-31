package detectionsystem;

import detectionsystem.AlternateAccountFinder;
import detectionsystem.data.PersistentData;
import detectionsystem.objects.InternetAddressRecord;

import java.net.InetAddress;

public class Utilities {

    AlternateAccountFinder main = null;

    public Utilities(AlternateAccountFinder plugin) {
        main = plugin;
    }

    public InternetAddressRecord getInternetAddressRecord(InetAddress IP) {
        for (InternetAddressRecord record : PersistentData.getInstance().getInternetAddressRecords()) {
            if (record.getIP().equals(IP)) {
                return record;
            }
        }
        return null;
    }

}
