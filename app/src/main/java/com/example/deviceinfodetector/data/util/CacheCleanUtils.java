package com.example.deviceinfodetector.data.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 缓存清理工具类（修复：确保回调在主线程执行）
 */
public class CacheCleanUtils {
    // 应用缓存信息实体（保持原有结构不变）
    public static class AppCacheInfo {
        public String appName;
        public String packageName;
        public long cacheSize;

        public AppCacheInfo(String appName, String packageName, long cacheSize) {
            this.appName = appName;
            this.packageName = packageName;
            this.cacheSize = cacheSize;
        }
    }

    // 异步线程池（单例，避免频繁创建）
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "CacheClean-Async");
        thread.setDaemon(true); // 守护线程，不影响应用退出
        return thread;
    });

    // 主线程Handler（用于将回调切换到主线程）
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /**
     * 获取所有应用缓存信息（修复：简化逻辑，优先保证获取当前应用缓存）
     */
    public static List<AppCacheInfo> getAppCacheList(Context context) {
        List<AppCacheInfo> cacheList = new ArrayList<>();
        if (context == null) {
            return cacheList;
        }

        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return cacheList;
        }

        // 首先获取当前应用的缓存信息，确保至少能返回当前应用的缓存
        String currentPackageName = context.getPackageName();
        ApplicationInfo currentAppInfo;
        try {
            currentAppInfo = pm.getApplicationInfo(currentPackageName, 0);
            String currentAppName = pm.getApplicationLabel(currentAppInfo).toString();
            long currentCacheSize = calculateCurrentAppCacheSize(context);

            // 添加日志，查看当前应用缓存大小
            android.util.Log.d("CacheCleanUtils", "当前应用：" + currentAppName + "，缓存大小：" + currentCacheSize);

            if (currentCacheSize > 0) {
                cacheList.add(new AppCacheInfo(currentAppName, currentPackageName, currentCacheSize));
                android.util.Log.d("CacheCleanUtils", "添加当前应用到缓存列表");
            }
        } catch (Exception e) {
            android.util.Log.e("CacheCleanUtils", "获取当前应用缓存失败", e);
        }

        // 如果已经有缓存（当前应用），则直接返回，不再扫描其他应用
        if (!cacheList.isEmpty()) {
            android.util.Log.d("CacheCleanUtils", "返回包含当前应用的缓存列表，数量：" + cacheList.size());
            return cacheList;
        }

        // 如果当前应用没有缓存，再尝试扫描其他应用（简化逻辑）
        List<ApplicationInfo> appList;
        try {
            appList = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        } catch (Exception e) {
            e.printStackTrace();
            return cacheList;
        }

        for (ApplicationInfo appInfo : appList) {
            try {
                String packageName = appInfo.packageName;
                String appName = pm.getApplicationLabel(appInfo).toString();
                long cacheSize = 0;

                // 简化：只计算应用的APK大小（安全，无需特殊权限）
                File apkFile = new File(appInfo.sourceDir);
                if (apkFile.exists()) {
                    cacheSize = apkFile.length();
                }

                // 只添加缓存大小大于0的应用
                if (cacheSize > 0) {
                    cacheList.add(new AppCacheInfo(appName, packageName, cacheSize));
                }
            } catch (Exception e) {
                // 单个应用失败不影响整体，跳过
            }
        }

        android.util.Log.d("CacheCleanUtils", "最终缓存列表数量：" + cacheList.size());
        return cacheList;
    }

    /**
     * 计算当前应用的缓存大小（修复：确保能正确获取当前应用缓存）
     */
    private static long calculateCurrentAppCacheSize(Context context) {
        long totalCache = 0;

        try {
            // 1. 当前应用内部缓存目录
            File internalCacheDir = context.getCacheDir();
            if (internalCacheDir != null && internalCacheDir.exists()) {
                totalCache += calculateFolderSize(internalCacheDir);
                android.util.Log.d("CacheCleanUtils", "内部缓存大小：" + totalCache);
            }

            // 2. 当前应用外部缓存目录
            File externalCacheDir = context.getExternalCacheDir();
            if (externalCacheDir != null && externalCacheDir.exists()) {
                totalCache += calculateFolderSize(externalCacheDir);
                android.util.Log.d("CacheCleanUtils", "外部缓存大小：" + (totalCache - calculateFolderSize(internalCacheDir)));
            }

            // 3. 当前应用外部存储缓存
            File externalStorageCache = new File(
                    Environment.getExternalStorageDirectory() + "/Android/data/" +
                            context.getPackageName() + "/cache");
            if (externalStorageCache.exists()) {
                totalCache += calculateFolderSize(externalStorageCache);
                android.util.Log.d("CacheCleanUtils", "外部存储缓存大小：" + (totalCache - calculateFolderSize(internalCacheDir) - calculateFolderSize(externalCacheDir)));
            }
        } catch (Exception e) {
            android.util.Log.e("CacheCleanUtils", "计算当前应用缓存大小失败", e);
        }

        return totalCache;
    }

    /**
     * 递归计算文件夹大小（增强安全检查）
     */
    private static long calculateFolderSize(File file) {
        if (file == null || !file.exists() || !file.canRead()) {
            return 0;
        }
        long size = 0;
        try {
            if (file.isFile()) {
                return file.length();
            }
            File[] files = file.listFiles();
            if (files == null) {
                return 0;
            }
            for (File f : files) {
                size += calculateFolderSize(f);
            }
        } catch (SecurityException e) {
            // 无权限访问子文件，返回已计算大小
        }
        return size;
    }

    /**
     * 清理单个应用缓存（仅支持当前应用，避免权限问题）
     */
    public static boolean cleanAppCache(Context context, String packageName) {
        // 仅允许清理当前应用缓存（其他应用需要特殊权限）
        if (context == null || !packageName.equals(context.getPackageName())) {
            return false;
        }

        try {
            // 1. 清理内部缓存
            File internalCacheDir = context.getCacheDir();
            deleteFolder(internalCacheDir);

            // 2. 清理外部缓存
            File externalCacheDir = context.getExternalCacheDir();
            deleteFolder(externalCacheDir);

            // 3. 清理外部存储缓存（如果有）
            File externalStorageCache = new File(
                    Environment.getExternalStorageDirectory() + "/Android/data/" + packageName + "/cache");
            deleteFolder(externalStorageCache);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 递归删除文件夹（增强安全检查）
     */
    private static void deleteFolder(File file) {
        if (file == null || !file.exists() || !file.canWrite()) {
            return;
        }
        try {
            if (file.isFile()) {
                file.delete();
                return;
            }
            File[] files = file.listFiles();
            if (files == null) {
                file.delete();
                return;
            }
            for (File f : files) {
                deleteFolder(f);
            }
            file.delete();
        } catch (SecurityException e) {
            // 无权限删除子文件，忽略
        }
    }

    /**
     * 字节转易读格式（保持原有逻辑不变）
     */
    public static String formatFileSize(long size) {
        if (size <= 0) return "0B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.getDefault(), "%.2f %s",
                size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    /**
     * 异步获取缓存列表（修复：确保回调在主线程执行）
     */
    public static void getAppCacheListAsync(Context context, OnCacheListCallback callback) {
        if (context == null || callback == null) {
            return;
        }

        EXECUTOR.execute(() -> {
            List<AppCacheInfo> cacheList = getAppCacheList(context);
            // 修复：使用主线程Handler将回调切换到主线程
            MAIN_HANDLER.post(() -> callback.onResult(cacheList));
        });
    }

    /**
     * 缓存列表回调接口（新增：支持异步调用）
     */
    public interface OnCacheListCallback {
        void onResult(List<AppCacheInfo> cacheList);
    }
}