package com.example.deviceinfodetector.ui.fragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.deviceinfodetector.R;
import com.example.deviceinfodetector.databinding.FragmentDeviceInfoBinding;
import com.example.deviceinfodetector.data.util.DeviceInfoUtils;

/**
 * 设备信息Fragment（修复所有错误 + 异步加载）
 */
public class DeviceInfoFragment extends Fragment {
    private static final String TAG = "DeviceInfoFragment";
    private FragmentDeviceInfoBinding binding;
    private static final int REQUEST_PERMISSIONS_CODE = 1001;

    private static final String[] REQUIRED_PERMISSIONS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
            new String[]{
                    Manifest.permission.READ_PHONE_NUMBERS,
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.BLUETOOTH_CONNECT
            } :
            new String[]{
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.BLUETOOTH
            };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDeviceInfoBinding.inflate(inflater, container, false);
        Log.d(TAG, "布局绑定状态：" + (binding == null ? "失败" : "成功"));
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!isAdded() || binding == null) {
            Log.e(TAG, "Fragment未附加或Binding为空，终止数据加载");
            return;
        }

        // 1. 加载无需权限的基础信息
        loadBasicInfo();
        // 2. 检查权限并加载扩展信息
        checkPermissionsAndLoadExtendedInfo();
    }

    /**
     * 加载无需权限的基础信息
     */
    private void loadBasicInfo() {
        Context context = requireContext();

        // 基础设备信息
        binding.tvBrand.setText(getString(R.string.label_brand) + safeValue(DeviceInfoUtils.getDeviceBrand()));
        binding.tvDeviceModelFull.setText(getString(R.string.label_device_model_full) + safeValue(DeviceInfoUtils.getDeviceModel()));
        binding.tvManufacturer.setText(getString(R.string.label_manufacturer) + safeValue(Build.MANUFACTURER));
        binding.tvAndroidVersion.setText(getString(R.string.label_android_version) + safeValue(DeviceInfoUtils.getAndroidVersion()));
        binding.tvSdkVersion.setText(getString(R.string.label_sdk_version) + Build.VERSION.SDK_INT);

        // 硬件基础信息
        binding.tvCpuCore.setText(getString(R.string.label_cpu_core) + DeviceInfoUtils.getCpuCoreCount());
        binding.tvCpuModel.setText(getString(R.string.label_cpu_model) + safeValue(DeviceInfoUtils.getCpuModel()));
        binding.tvScreenResolution.setText(getString(R.string.label_screen_resolution) + safeValue(DeviceInfoUtils.getScreenResolution(context)));

        Log.d(TAG, "基础信息加载完成（无需权限）");
    }

    /**
     * 检查权限并加载扩展信息
     */
    private void checkPermissionsAndLoadExtendedInfo() {
        boolean allGranted = true;
        for (String perm : REQUIRED_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(requireContext(), perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            // 权限全授予，加载所有扩展信息
            loadAllExtendedInfo();
        } else {
            // 权限未授予，先置为未知，再申请权限
            setExtendedInfoToUnknown();
            ActivityCompat.requestPermissions(requireActivity(), REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS_CODE);
        }
    }

    /**
     * 加载所有需权限的扩展信息（适配异步方法）
     */
    private void loadAllExtendedInfo() {
        Context context = requireContext();
        Log.d(TAG, "开始加载所有扩展信息");

        // 硬件扩展信息（同步）
        binding.tvTotalMemory.setText(getString(R.string.label_total_memory) + DeviceInfoUtils.formatFileSize(DeviceInfoUtils.getTotalMemory(context)));
        binding.tvAvailableRam.setText(getString(R.string.label_available_ram) + DeviceInfoUtils.formatFileSize(DeviceInfoUtils.getAvailableMemory(context)));
        binding.tvRefreshRate.setText(getString(R.string.label_refresh_rate) + safeValue(DeviceInfoUtils.getRefreshRate(context)));

        // 网络信息（同步）
        binding.tvNetworkType.setText(getString(R.string.label_network_type) + safeValue(DeviceInfoUtils.getNetworkType(context)));
        binding.tvIpv4Address.setText(getString(R.string.label_ipv4_address) + safeValue(DeviceInfoUtils.getLocalIpv4(context)));
        binding.tvConnectionSpeed.setText(getString(R.string.label_connection_speed) + safeValue(DeviceInfoUtils.getConnectionSpeed(context)));
        binding.tvSignalStrength.setText(getString(R.string.label_signal_strength) + safeValue(DeviceInfoUtils.getSignalStrength(context)));
        binding.tvWifiStandard.setText(getString(R.string.label_wifi_standard) + safeValue(DeviceInfoUtils.getWifiStandard(context)));
        binding.tvNetworkOperator.setText(getString(R.string.label_network_operator) + safeValue(DeviceInfoUtils.getNetworkOperator(context)));
        binding.tvOperatorCountry.setText(getString(R.string.label_operator_country) + safeValue(DeviceInfoUtils.getOperatorCountry(context)));
        binding.tvSupport5g4g.setText(getString(R.string.label_support_5g_4g) + safeValue(DeviceInfoUtils.getSupport5G4G(context)));

        // 域名信息（异步）
        binding.tvDomainName.setText(getString(R.string.label_domain_name) + getString(R.string.status_loading));
        DeviceInfoUtils.getDomainNameAsync(context, domain -> {
            binding.tvDomainName.setText(getString(R.string.label_domain_name) + safeValue(domain));
        });

        // 蓝牙信息（同步）
        binding.tvBluetoothVer.setText(getString(R.string.label_bluetooth_ver) + safeValue(DeviceInfoUtils.getBluetoothVersion(context)));

        // 应用信息（异步）
        binding.tvSystemAppCount.setText(getString(R.string.label_system_app_count) + getString(R.string.status_loading));
        DeviceInfoUtils.getSystemAppCountAsync(context, count -> {
            binding.tvSystemAppCount.setText(getString(R.string.label_system_app_count) + count);
        });

        binding.tvUserAppCount.setText(getString(R.string.label_user_app_count) + getString(R.string.status_loading));
        DeviceInfoUtils.getUserAppCountAsync(context, count -> {
            binding.tvUserAppCount.setText(getString(R.string.label_user_app_count) + count);
        });

        binding.tvAppTotalSize.setText(getString(R.string.label_app_total_size) + getString(R.string.status_loading));
        DeviceInfoUtils.getUserAppTotalSizeAsync(context, size -> {
            binding.tvAppTotalSize.setText(getString(R.string.label_app_total_size) + safeValue(size));
        });

        // 相机信息（同步）
        binding.tvCameraSensorSize.setText(getString(R.string.label_camera_sensor_size) + safeValue(DeviceInfoUtils.getCameraSensorSize(context)));
        binding.tvCameraEffectivePixels.setText(getString(R.string.label_camera_effective_pixels) + safeValue(DeviceInfoUtils.getCameraEffectivePixels(context)));
        binding.tvCameraResolution.setText(getString(R.string.label_camera_resolution) + safeValue(DeviceInfoUtils.getCameraMaxResolution(context)));
        binding.tvCameraEquivalentFocal.setText(getString(R.string.label_camera_equivalent_focal) + safeValue(DeviceInfoUtils.getCameraEquivalentFocal(context)));
        binding.tvCameraShutterSpeed.setText(getString(R.string.label_camera_shutter_speed) + safeValue(DeviceInfoUtils.getCameraShutterSpeed(context)));
        binding.tvCameraOis.setText(getString(R.string.label_camera_ois) + safeValue(DeviceInfoUtils.getCameraOIS(context)));

        Log.d(TAG, "扩展信息加载触发完成（异步任务已提交）");
    }

    /**
     * 权限未授予时，扩展信息置为未知
     */
    private void setExtendedInfoToUnknown() {
        String unknown = getString(R.string.status_unknown);
        String loading = getString(R.string.status_loading);

        // 硬件扩展
        binding.tvTotalMemory.setText(getString(R.string.label_total_memory) + unknown);
        binding.tvAvailableRam.setText(getString(R.string.label_available_ram) + unknown);
        binding.tvRefreshRate.setText(getString(R.string.label_refresh_rate) + unknown);

        // 网络信息
        binding.tvNetworkType.setText(getString(R.string.label_network_type) + unknown);
        binding.tvIpv4Address.setText(getString(R.string.label_ipv4_address) + unknown);
        binding.tvConnectionSpeed.setText(getString(R.string.label_connection_speed) + unknown);
        binding.tvSignalStrength.setText(getString(R.string.label_signal_strength) + unknown);
        binding.tvWifiStandard.setText(getString(R.string.label_wifi_standard) + unknown);
        binding.tvDomainName.setText(getString(R.string.label_domain_name) + unknown);
        binding.tvNetworkOperator.setText(getString(R.string.label_network_operator) + unknown);
        binding.tvOperatorCountry.setText(getString(R.string.label_operator_country) + unknown);
        binding.tvSupport5g4g.setText(getString(R.string.label_support_5g_4g) + unknown);

        // 蓝牙信息
        binding.tvBluetoothVer.setText(getString(R.string.label_bluetooth_ver) + unknown);

        // 应用信息
        binding.tvSystemAppCount.setText(getString(R.string.label_system_app_count) + unknown);
        binding.tvUserAppCount.setText(getString(R.string.label_user_app_count) + unknown);
        binding.tvAppTotalSize.setText(getString(R.string.label_app_total_size) + unknown);

        // 相机信息
        binding.tvCameraSensorSize.setText(getString(R.string.label_camera_sensor_size) + unknown);
        binding.tvCameraEffectivePixels.setText(getString(R.string.label_camera_effective_pixels) + unknown);
        binding.tvCameraResolution.setText(getString(R.string.label_camera_resolution) + unknown);
        binding.tvCameraEquivalentFocal.setText(getString(R.string.label_camera_equivalent_focal) + unknown);
        binding.tvCameraShutterSpeed.setText(getString(R.string.label_camera_shutter_speed) + unknown);
        binding.tvCameraOis.setText(getString(R.string.label_camera_ois) + unknown);
    }

    /**
     * 安全值处理（null转unknown）
     */
    private String safeValue(String value) {
        return value == null || value.isEmpty() ? getString(R.string.status_unknown) : value;
    }

    /**
     * 权限申请回调
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            boolean allGranted = grantResults.length > 0;
            for (int res : grantResults) {
                if (res != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                loadAllExtendedInfo();
                Log.d(TAG, "权限申请成功，加载完整扩展信息");
            } else {
                Log.w(TAG, "部分权限被拒绝，扩展信息仍显示未知");
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 释放线程池
        DeviceInfoUtils.release();
        binding = null;
        Log.d(TAG, "Fragment视图销毁，资源已释放");
    }
}