package com.example.deviceinfodetector.data.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cache_clean_records")
public class CacheCleanRecord {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public long cleanTime;       // 清理时间戳
    public int appCount;         // 清理应用数量
    public long releaseSpace;    // 释放空间(字节)

    public CacheCleanRecord(long cleanTime, int appCount, long releaseSpace) {
        this.cleanTime = cleanTime;
        this.appCount = appCount;
        this.releaseSpace = releaseSpace;
    }
}