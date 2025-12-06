package com.example.deviceinfodetector.data.db;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.example.deviceinfodetector.data.db.dao.AppBgRecordDao;
import com.example.deviceinfodetector.data.db.dao.CacheCleanRecordDao;
import com.example.deviceinfodetector.data.db.dao.BatteryRecordDao;
import com.example.deviceinfodetector.data.db.entity.AppBgRecord;
import com.example.deviceinfodetector.data.db.entity.CacheCleanRecord;
import com.example.deviceinfodetector.data.db.entity.BatteryRecord;

@Database(entities = {AppBgRecord.class, CacheCleanRecord.class, BatteryRecord.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract AppBgRecordDao appBgRecordDao();
    public abstract CacheCleanRecordDao cacheCleanRecordDao();
    public abstract BatteryRecordDao batteryRecordDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "device_info_db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}