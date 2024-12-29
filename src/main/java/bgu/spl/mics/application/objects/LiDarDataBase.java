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

    private List<StampedCloudPoints> cloudPoints;

    private LiDarDataBase(String filePath) {
        this.cloudPoints = loadDataFromFile(filePath);
    }
    // Singleton Holder - Lazy Initialization
    private static class SingletonHolderLiDarDataBase {
        private static LiDarDataBase INSTANCE = null;

        private static LiDarDataBase LiDarDataBasecreatInstance (String filePath){
            if (INSTANCE == null){
                INSTANCE = new LiDarDataBase (filePath);
            }
            return INSTANCE;
        }
    }

    public static LiDarDataBase getInstance (String filePath){
        return SingletonHolderLiDarDataBase.LiDarDataBasecreatInstance(filePath);
    }


    /**
     * Returns the singleton instance of LiDarDataBase.
     * Initializes it with the provided file path if not already initialized.
     *
     * @param filePath The file path to initialize the LiDarDataBase.
     * @return The LiDarDataBase instance.
     */


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