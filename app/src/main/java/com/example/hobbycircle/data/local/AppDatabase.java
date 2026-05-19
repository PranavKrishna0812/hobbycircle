package com.example.hobbycircle.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {EventEntity.class, UserEntity.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS users ("
                    + "id TEXT NOT NULL PRIMARY KEY, "
                    + "name TEXT, "
                    + "email TEXT, "
                    + "role TEXT, "
                    + "location TEXT, "
                    + "selectedHobbiesCsv TEXT, "
                    + "photoUrl TEXT, "
                    + "updatedAtMillis INTEGER NOT NULL DEFAULT 0)");
        }
    };

    public abstract EventDao eventDao();

    public abstract UserDao userDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "hobby_circle_db"
                            )
                            .addMigrations(MIGRATION_2_3)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
