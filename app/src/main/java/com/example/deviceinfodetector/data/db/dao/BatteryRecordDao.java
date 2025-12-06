package com.example.deviceinfodetector.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.deviceinfodetector.data.db.entity.BatteryRecord;

import java.util.List;

@Dao
public interface BatteryRecordDao {
    @Insert
    void insert(BatteryRecord record);

    @Query("SELECT * FROM battery_records WHERE timestamp >= :startTime ORDER BY timestamp ASC")
    List<BatteryRecord> getBatteryRecords(long startTime);
}