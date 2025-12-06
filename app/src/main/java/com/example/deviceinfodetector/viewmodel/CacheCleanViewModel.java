package com.example.deviceinfodetector.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.deviceinfodetector.data.util.CacheCleanUtils;

import java.util.List;

/**
 * 缓存清理ViewModel（修复：使用正确的异步缓存扫描方法）
 */
public class CacheCleanViewModel extends AndroidViewModel {

    private static final String TAG = "CacheCleanViewModel";

    // 缓存应用列表LiveData
    private final MutableLiveData<List<CacheCleanUtils.AppCacheInfo>> appCacheList = new MutableLiveData<>();
    // 清理进度LiveData
    private final MutableLiveData<Integer> cleanProgress = new MutableLiveData<>(0);
    // 释放空间LiveData
    private final MutableLiveData<Long> totalReleaseSpace = new MutableLiveData<>(0L);

    public CacheCleanViewModel(@NonNull Application application) {
        super(application);
        // 初始化时加载缓存列表
        loadAppCacheList();
    }

    /**
     * 获取缓存应用列表LiveData
     */
    public LiveData<List<CacheCleanUtils.AppCacheInfo>> getAppCacheList() {
        return appCacheList;
    }

    /**
     * 获取清理进度LiveData
     */
    public LiveData<Integer> getCleanProgress() {
        return cleanProgress;
    }

    /**
     * 获取释放空间LiveData
     */
    public LiveData<Long> getTotalReleaseSpace() {
        return totalReleaseSpace;
    }

    /**
     * 加载缓存列表（修复：使用正确的异步方法）
     */
    public void loadAppCacheList() {
        Log.d(TAG, "开始加载缓存列表");

        // 修复：调用修复后的异步方法 getAppCacheListAsync
        // 替代可能的同步调用或错误的异步方法
        CacheCleanUtils.getAppCacheListAsync(getApplication(), cacheList -> {
            Log.d(TAG, "缓存列表加载完成，数量：" + (cacheList != null ? cacheList.size() : 0));

            // 在主线程更新LiveData（getAppCacheListAsync内部已处理主线程回调）
            appCacheList.setValue(cacheList);
        });
    }

    /**
     * 清理选中的应用缓存
     */
    public void cleanSelectedApps(List<String> selectedPackages) {
        if (selectedPackages == null || selectedPackages.isEmpty()) {
            return;
        }

        // 重置进度和释放空间
        cleanProgress.setValue(0);
        totalReleaseSpace.setValue(0L);

        long totalRelease = 0;
        int totalApps = selectedPackages.size();

        // 模拟清理进度（实际项目中应根据实际清理情况更新）
        for (int i = 0; i < totalApps; i++) {
            String packageName = selectedPackages.get(i);

            // 调用清理方法
            boolean success = CacheCleanUtils.cleanAppCache(getApplication(), packageName);
            if (success) {
                // 假设每个应用释放1MB空间（实际应根据实际情况计算）
                totalRelease += 1024 * 1024;
            }

            // 更新进度
            int progress = (i + 1) * 100 / totalApps;
            cleanProgress.setValue(progress);
        }

        // 更新释放空间
        totalReleaseSpace.setValue(totalRelease);

        // 清理完成后重新加载缓存列表
        loadAppCacheList();
    }
}