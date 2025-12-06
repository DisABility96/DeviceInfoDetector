package com.example.deviceinfodetector.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.deviceinfodetector.data.util.DeviceInfoUtils;

public class DeviceInfoViewModel extends AndroidViewModel {
    private final MutableLiveData<String> deviceBrand = new MutableLiveData<>();
    private final MutableLiveData<String> deviceModel = new MutableLiveData<>();
    private final MutableLiveData<String> androidVersion = new MutableLiveData<>();
    private final MutableLiveData<String> totalMemory = new MutableLiveData<>(); // 改为String类型
    private final MutableLiveData<String> cpuCoreCount = new MutableLiveData<>();
    private final MutableLiveData<String> screenRefreshRate = new MutableLiveData<>();
    private final MutableLiveData<String> screenResolution = new MutableLiveData<>();

    public DeviceInfoViewModel(Application application) {
        super(application);
        loadDeviceInfo();
    }

    private void loadDeviceInfo() {
        deviceBrand.setValue(DeviceInfoUtils.getDeviceBrand());
        deviceModel.setValue(DeviceInfoUtils.getDeviceModel());
        androidVersion.setValue(DeviceInfoUtils.getAndroidVersion());

        // 修复1：long转String（格式化内存大小）
        long memory = DeviceInfoUtils.getTotalMemory(getApplication());
        totalMemory.setValue(DeviceInfoUtils.formatFileSize(memory));

        cpuCoreCount.setValue(String.valueOf(DeviceInfoUtils.getCpuCoreCount()));

        // 修复2：方法名错误（getScreenRefreshRate → getRefreshRate）
        screenRefreshRate.setValue(DeviceInfoUtils.getRefreshRate(getApplication()));

        screenResolution.setValue(DeviceInfoUtils.getScreenResolution(getApplication()));
    }

    // Getter方法
    public MutableLiveData<String> getDeviceBrand() { return deviceBrand; }
    public MutableLiveData<String> getDeviceModel() { return deviceModel; }
    public MutableLiveData<String> getAndroidVersion() { return androidVersion; }
    public MutableLiveData<String> getTotalMemory() { return totalMemory; }
    public MutableLiveData<String> getCpuCoreCount() { return cpuCoreCount; }
    public MutableLiveData<String> getScreenRefreshRate() { return screenRefreshRate; }
    public MutableLiveData<String> getScreenResolution() { return screenResolution; }
}