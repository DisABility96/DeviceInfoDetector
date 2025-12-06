package com.example.deviceinfodetector.data.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log; // 导入日志类

import com.example.deviceinfodetector.R;
import com.example.deviceinfodetector.data.db.AppDatabase;
import com.example.deviceinfodetector.data.db.entity.AppBgRecord;

import java.util.Calendar;
import java.util.List;

/**
 * 应用后台监测工具类（增强版：添加详细调试日志）
 */
public class AppBgMonitorUtils {

    // 统一日志标签，方便Logcat过滤
    private static final String TAG = "AppBgMonitorUtils";

    // 记录应用后台启动
    public static void recordAppBgLaunch(Context context, String packageName) {
        Log.d(TAG, "=== 开始记录后台启动事件 ===");
        Log.d(TAG, "触发包名：" + packageName);
        Log.d(TAG, "当前线程：" + Thread.currentThread().getName());

        new Thread(() -> {
            Log.d(TAG, "后台线程开始执行：" + Thread.currentThread().getName());
            try {
                // 1. 获取PackageManager
                PackageManager pm = context.getPackageManager();
                Log.d(TAG, "成功获取PackageManager");

                // 2. 获取应用信息
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                String appName = pm.getApplicationLabel(appInfo).toString();
                Log.d(TAG, "成功获取应用信息：" + appName + " (" + packageName + ")");

                // 3. 操作数据库
                AppDatabase db = AppDatabase.getInstance(context);
                Log.d(TAG, "成功获取数据库实例");

                // 4. 更新启动次数或插入新记录
                int updateCount = db.appBgRecordDao().updateLaunchCount(packageName);
                Log.d(TAG, "更新启动次数结果：" + updateCount + " 条记录被更新");

                if (updateCount == 0) {
                    // 无记录，插入新记录
                    long now = System.currentTimeMillis();
                    String wakeReason = context.getString(R.string.reason_push);
                    AppBgRecord newRecord = new AppBgRecord(appName, packageName, now, 1, wakeReason);
                    Log.d(TAG, "插入新记录：" + newRecord.toString());
                    db.appBgRecordDao().insert(newRecord);
                    Log.d(TAG, "新记录插入成功");
                }

                // 5. 删除7天前的过期记录
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, -7);
                long expireTime = cal.getTimeInMillis();
                int deleteCount = db.appBgRecordDao().deleteExpiredRecords(expireTime);
                Log.d(TAG, "删除过期记录：" + deleteCount + " 条记录被删除（过期时间：" + expireTime + "）");

                Log.d(TAG, "=== 后台启动事件记录完成 ===");

            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "应用包名不存在：" + packageName, e);
            } catch (Exception e) {
                Log.e(TAG, "记录后台启动事件失败", e);
            }
        }).start();
    }

    // 获取今日后台启动记录
    public static List<AppBgRecord> getTodayBgRecords(Context context) {
        Log.d(TAG, "=== 获取今日后台启动记录 ===");
        Log.d(TAG, "当前线程：" + Thread.currentThread().getName());

        try {
            AppDatabase db = AppDatabase.getInstance(context);
            List<AppBgRecord> records = db.appBgRecordDao().getTodayBgRecords();
            Log.d(TAG, "获取记录成功，数量：" + (records != null ? records.size() : 0));

            // 打印前5条记录详情（避免日志过长）
            if (records != null && !records.isEmpty()) {
                Log.d(TAG, "前5条记录详情：");
                int limit = Math.min(5, records.size());
                for (int i = 0; i < limit; i++) {
                    AppBgRecord record = records.get(i);
                    Log.d(TAG, "  [" + i + "] " + record.toString());
                }
            }

            Log.d(TAG, "=== 获取今日记录完成 ===");
            return records;
        } catch (Exception e) {
            Log.e(TAG, "获取今日后台记录失败", e);
            return null;
        }
    }

    // 跳转到应用信息页
    public static Intent getAppInfoIntent(String packageName) {
        Log.d(TAG, "生成应用信息页Intent，包名：" + packageName);
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(android.net.Uri.parse("package:" + packageName));
        Log.d(TAG, "Intent生成成功：" + intent.toString());
        return intent;
    }
}