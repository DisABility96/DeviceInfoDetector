package com.example.deviceinfodetector.data.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "battery_records")
public class BatteryRecord {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public long timestamp;      // 时间戳
    public int level;           // 电量(%)
    public int temperature;     // 温度(℃)

    public BatteryRecord(long timestamp, int level, int temperature) {
        this.timestamp = timestamp;
        this.level = level;
        this.temperature = temperature;
    }
}