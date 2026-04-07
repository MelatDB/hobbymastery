package com.example.hobbymasterytracker;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Hobby.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract HobbyDao hobbyDao();
    private static AppDatabase instance;
    private static String currentUser = "default";

    public static synchronized AppDatabase getInstance(Context context) {
        return getInstance(context, currentUser);
    }

    public static synchronized AppDatabase getInstance(Context context, String username) {
        String dbName = "hobby_db_" + username.toLowerCase().replaceAll("[^a-z0-9]", "_");
        if (instance == null || !currentUser.equals(username)) {
            if (instance != null) instance.close();
            currentUser = username;
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, dbName)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public static void resetInstance() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }
}