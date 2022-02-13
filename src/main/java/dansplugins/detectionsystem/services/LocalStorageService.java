package dansplugins.detectionsystem.services;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import dansplugins.detectionsystem.data.PersistentData;
import dansplugins.detectionsystem.objects.InternetAddressRecord;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalStorageService {

    private static LocalStorageService instance;

    private final static String FILE_PATH = "./plugins/AlternateAccountFinder/";
    private final static String RECORDS_FILE_NAME = "records.json";

    private final static Type LIST_MAP_TYPE = new TypeToken<ArrayList<HashMap<String, String>>>(){}.getType();

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();;

    private LocalStorageService() {

    }

    public static LocalStorageService getInstance() {
        if (instance == null) {
            instance = new LocalStorageService();
        }
        return instance;
    }

    public void save() {
        createSaveFolderIfNonexistant();
        saveInternetAddressRecords();
    }

    public void load() {
        loadInternetAddressRecords();
    }

    private void createSaveFolderIfNonexistant() {
        File saveFolder = new File(FILE_PATH);
        try {
            if (!saveFolder.exists()) {
                saveFolder.mkdir();
            }
        } catch(Exception e) {
            System.out.println("A problem occurred creating the necessarily files for the Alternate Account Finder.");
        }

    }

    private void saveInternetAddressRecords() {
        List<Map<String, String>> records = new ArrayList<>();
        for (InternetAddressRecord record : PersistentData.getInstance().getInternetAddressRecords()){
            records.add(record.save());
        }

        File file = new File(FILE_PATH + RECORDS_FILE_NAME);
        writeOutFiles(file, records);
    }

    private void loadInternetAddressRecords() {
        PersistentData.getInstance().getInternetAddressRecords().clear();

        ArrayList<HashMap<String, String>> data = loadDataFromFilename(FILE_PATH + RECORDS_FILE_NAME);

        for (Map<String, String> chunkData : data){
            InternetAddressRecord record = new InternetAddressRecord(chunkData);
            PersistentData.getInstance().getInternetAddressRecords().add(record);
        }
    }

    private void writeOutFiles(File file, List<Map<String, String>> saveData) {
        try {
            file.createNewFile();
            FileWriter saveWriter = new FileWriter(file);
            saveWriter.write(gson.toJson(saveData));
            saveWriter.close();
        } catch(IOException e) {
            System.out.println("ERROR: " + e.toString());
        }
    }

    private ArrayList<HashMap<String, String>> loadDataFromFilename(String filename) {
        try{
            Gson gson = new GsonBuilder().setPrettyPrinting().create();;
            JsonReader reader = new JsonReader(new FileReader(filename));
            return gson.fromJson(reader, LIST_MAP_TYPE);
        } catch (FileNotFoundException e) {
            // Fail silently because this can actually happen in normal use
        }
        return new ArrayList<>();
    }

}
