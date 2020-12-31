package detectionsystem.data;

import detectionsystem.objects.InternetAddressRecord;

import java.util.ArrayList;

public class PersistentData {

    private static PersistentData instance;

    private ArrayList<InternetAddressRecord> internetAddressRecords = new ArrayList<>();

    private PersistentData() {

    }

    public static PersistentData getInstance() {
        if (instance == null) {
            instance = new PersistentData();
        }
        return instance;
    }

    public ArrayList<InternetAddressRecord> getInternetAddressRecords() {
        return internetAddressRecords;
    }

}
