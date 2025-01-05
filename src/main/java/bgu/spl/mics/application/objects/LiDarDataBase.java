package bgu.spl.mics.application.objects;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LiDarDataBase is a singleton class responsible for managing LiDAR data.
 * It provides access to cloud point data and other relevant information for
 * tracked objects.
 */
// LiDarDataBase class
public class LiDarDataBase {

    // fields
    private List<StampedCloudPoints> cloudPoints;
    private AtomicInteger counter = new AtomicInteger(0);

    // Constructor to initialize the LiDarDataBase object.
    private LiDarDataBase(String filePath) {
        this.cloudPoints = loadDataFromFile(filePath);
        this.counter.set(cloudPoints.size());
    }

    // Singleton Holder implementation as shown in class
    private static class SingletonHolderLiDarDataBase {
        private static LiDarDataBase INSTANCE = null;

        private static LiDarDataBase LiDarDataBasecreatInstance(String filePath) {
            if (INSTANCE == null) {
                INSTANCE = new LiDarDataBase(filePath);
            }
            return INSTANCE;
        }
    }

    public static LiDarDataBase getInstance(String filePath) {
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
            return gson.fromJson(reader, new TypeToken<List<StampedCloudPoints>>() {
            }.getType());
        } catch (IOException e) {
            return new ArrayList<>(); // Return an empty list in case of failure
        }
    }

    public int decrementCounter() {
        return counter.updateAndGet(value -> value > 0 ? value - 1 : 0);
    }

    public int getCounter() {
        return counter.get();
    }

    public List<StampedCloudPoints> getCloudPoints() {
        return cloudPoints;
    }
}