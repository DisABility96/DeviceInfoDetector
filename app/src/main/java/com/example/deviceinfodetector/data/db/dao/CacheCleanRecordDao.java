package com.example.deviceinfodetector.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.deviceinfodetector.data.db.entity.CacheCleanRecord;

import java.util.List;

@Dao
public interface CacheCleanRecordDao {
    @Insert
    void insert(CacheCleanRecord record);

    @Query("SELECT * FROM cache_clean_records ORDER BY cleanTime DESC LIMIT 5")
    List<CacheCleanRecord> getLatestCleanRecords();
}