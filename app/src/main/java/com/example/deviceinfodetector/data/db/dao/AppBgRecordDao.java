package com.example.deviceinfodetector.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.deviceinfodetector.data.db.entity.AppBgRecord;

import java.util.List;

@Dao
public interface AppBgRecordDao {
    @Insert
    void insert(AppBgRecord record);

    @Query("SELECT * FROM app_bg_records WHERE date(startTime/1000, 'unixepoch') = date('now') ORDER BY launchCount DESC")
    List<AppBgRecord> getTodayBgRecords();

    @Query("UPDATE app_bg_records SET launchCount = launchCount + 1 WHERE packageName = :packageName AND date(startTime/1000, 'unixepoch') = date('now')")
    int updateLaunchCount(String packageName);

    @Query("DELETE FROM app_bg_records WHERE startTime < :expireTime")
    int deleteExpiredRecords(long expireTime);
}