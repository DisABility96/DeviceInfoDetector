package com.example.deviceinfodetector.data.db.entity;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "app_bg_records")
public class AppBgRecord {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String appName;       // 应用名称
    public String packageName;   // 包名
    public long startTime;       // 启动时间戳
    public int launchCount;      // 今日启动次数
    public String wakeReason;    // 唤醒原因

    public AppBgRecord(String appName, String packageName, long startTime, int launchCount, String wakeReason) {
        this.appName = appName;
        this.packageName = packageName;
        this.startTime = startTime;
        this.launchCount = launchCount;
        this.wakeReason = wakeReason;
    }
}