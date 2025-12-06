package com.example.deviceinfodetector.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.deviceinfodetector.data.util.AppBgMonitorUtils;

public class AppBgBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "AppBgBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "=== 收到广播 ===");
        Log.d(TAG, "广播Action：" + intent.getAction());
        Log.d(TAG, "广播Data：" + intent.getData());

        // 处理应用安装/替换/重启广播
        if (Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())
                || Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())
                || Intent.ACTION_PACKAGE_RESTARTED.equals(intent.getAction())) {

            // 获取包名
            String packageName = intent.getData() != null
                    ? intent.getData().getSchemeSpecificPart()
                    : null;
            Log.d(TAG, "解析到包名：" + packageName);

            // 调用记录方法
            if (packageName != null) {
                Log.d(TAG, "调用recordAppBgLaunch记录后台启动");
                AppBgMonitorUtils.recordAppBgLaunch(context, packageName);
            }
        }

        Log.d(TAG, "=== 广播处理完成 ===");
    }
}