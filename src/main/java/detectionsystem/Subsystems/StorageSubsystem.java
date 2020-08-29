package detectionsystem.Subsystems;

import detectionsystem.AlternateAccountFinder;
import detectionsystem.Objects.InternetAddressRecord;

public class StorageSubsystem {

    AlternateAccountFinder main = null;

    public StorageSubsystem(AlternateAccountFinder plugin) {
        main = plugin;
    }

    public void save() {
        saveInternetAddressRecordFilenames();
        saveInternetAddressRecords();
    }

    public void load() {
        loadInternetAddressRecords();
    }

    private void saveInternetAddressRecordFilenames() {

    }


    private void saveInternetAddressRecords() {
        for (InternetAddressRecord record : main.internetAddressRecords) {
            record.save();
        }
    }

    private void loadInternetAddressRecords() {

    }




}
