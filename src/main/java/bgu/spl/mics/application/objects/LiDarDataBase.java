package bgu.spl.mics.application.objects;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * LiDarDataBase is a singleton class responsible for managing LiDAR data.
 * It provides access to cloud point data and other relevant information for tracked objects.
 */
public class LiDarDataBase {

    private static LiDarDataBase instance;// Singleton instance
    private List<StampedCloudPoints> cloudPoints;

    private LiDarDataBase(String filePath) {
        this.cloudPoints = loadDataFromFile(filePath);
    }

    public static LiDarDataBase getInstance(String filePath) {
        if (instance == null) {
            synchronized (LiDarDataBase.class) {
                if (instance == null) {
                    instance = new LiDarDataBase(filePath);
                }
            }
        }
        return instance;
    }

    private List<StampedCloudPoints> loadDataFromFile(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            return gson.fromJson(reader, new TypeToken<List<StampedCloudPoints>>() {}.getType());
        } catch (IOException e) {
            return new ArrayList<>(); // Return an empty list in case of failure
        }
    }

    public List<StampedCloudPoints> getCloudPoints() {
        return cloudPoints;
    }
    //-----------------------פונקציה שמחזירה משהו שהמיקרו מבקש
}