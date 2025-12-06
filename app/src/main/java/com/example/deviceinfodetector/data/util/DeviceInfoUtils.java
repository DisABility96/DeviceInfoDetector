package com.example.deviceinfodetector.data.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.CellSignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.SignalStrength;
import android.text.TextUtils;
import android.util.Log;
import android.util.SizeF;
import android.view.Display;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;

import com.example.deviceinfodetector.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 设备信息工具类（修复所有日志错误 + 异步加载避免主线程卡顿）
 * 日志标签：DEVICE_INFO_UTILS
 */
public class DeviceInfoUtils {
    private static final String TAG = "DEVICE_INFO_UTILS";
    // 异步线程池（避免主线程卡顿）
    private static volatile ExecutorService EXECUTOR;
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static ExecutorService getExecutor() {
        if (EXECUTOR == null || EXECUTOR.isShutdown() || EXECUTOR.isTerminated()) {
            synchronized (DeviceInfoUtils.class) {
                if (EXECUTOR == null || EXECUTOR.isShutdown() || EXECUTOR.isTerminated()) {
                    EXECUTOR = Executors.newSingleThreadExecutor(r -> {
                        Thread thread = new Thread(r, "DeviceInfo-Async-Thread");
                        thread.setDaemon(true); // 设置为守护线程，避免影响应用退出
                        return thread;
                    });
                }
            }
        }
        return EXECUTOR;
    }
    // ===================== 基础信息（无错误） =====================
    public static String getDeviceBrand() {
        Log.d(TAG, "进入方法：getDeviceBrand()");
        String brand = Build.BRAND;
        if (TextUtils.isEmpty(brand)) {
            Log.w(TAG, "getDeviceBrand: 返回值为空，Build.BRAND = " + brand);
            return null;
        }
        Log.d(TAG, "getDeviceBrand: 成功返回 = " + brand);
        return brand;
    }

    public static String getDeviceModel() {
        Log.d(TAG, "进入方法：getDeviceModel()");
        String model = Build.MODEL;
        if (TextUtils.isEmpty(model)) {
            Log.w(TAG, "getDeviceModel: 返回值为空，Build.MODEL = " + model);
            return null;
        }
        Log.d(TAG, "getDeviceModel: 成功返回 = " + model);
        return model;
    }

    public static String getAndroidVersion() {
        Log.d(TAG, "进入方法：getAndroidVersion()");
        String version = Build.VERSION.RELEASE;
        if (TextUtils.isEmpty(version)) {
            Log.w(TAG, "getAndroidVersion: 返回值为空，Build.VERSION.RELEASE = " + version);
            return null;
        }
        Log.d(TAG, "getAndroidVersion: 成功返回 = " + version);
        return version;
    }

    public static int getCpuCoreCount() {
        Log.d(TAG, "进入方法：getCpuCoreCount()");
        int coreCount = Runtime.getRuntime().availableProcessors();
        Log.d(TAG, "getCpuCoreCount: 成功返回 = " + coreCount);
        return coreCount;
    }

    public static String getRefreshRate(Context context) {
        Log.d(TAG, "进入方法：getRefreshRate(context)，context = " + (context == null ? "null" : context.getPackageName()));
        if (context == null) {
            Log.e(TAG, "getRefreshRate: context为空，直接返回unknown");
            return context.getString(R.string.status_unknown);
        }

        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                Log.e(TAG, "getRefreshRate: WindowManager获取失败");
                return context.getString(R.string.status_unknown);
            }
            Display display = wm.getDefaultDisplay();
            String refreshRate;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                float rate = display.getRefreshRate();
                refreshRate = String.format(Locale.getDefault(), "%.0f Hz", rate);
            } else {
                refreshRate = "60 Hz";
                Log.d(TAG, "getRefreshRate: 低版本（<R）默认返回60Hz");
            }
            Log.d(TAG, "getRefreshRate: 成功返回 = " + refreshRate);
            return refreshRate;
        } catch (Exception e) {
            Log.e(TAG, "getRefreshRate: 未知异常 = " + e.getMessage(), e);
            return context.getString(R.string.status_unknown);
        }
    }

    // ===================== 内存信息（无错误） =====================
    public static long getTotalMemory(Context context) {
        Log.d(TAG, "进入方法：getTotalMemory(context)，context = " + (context == null ? "null" : context.getPackageName()));
        if (context == null) {
            Log.e(TAG, "getTotalMemory: context为空，返回0");
            return 0;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                android.app.ActivityManager am = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                if (am == null) {
                    Log.e(TAG, "getTotalMemory: ActivityManager获取失败");
                    return 0;
                }
                android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                long totalMem = mi.totalMem;
                Log.d(TAG, "getTotalMemory: 成功返回 = " + totalMem + " 字节（" + formatFileSize(totalMem) + "）");
                return totalMem;
            } else {
                Log.w(TAG, "getTotalMemory: SDK版本<16，不支持获取总内存");
                return 0;
            }
        } catch (Exception e) {
            Log.e(TAG, "getTotalMemory: 未知异常 = " + e.getMessage(), e);
            return 0;
        }
    }

    public static long getAvailableMemory(Context context) {
        Log.d(TAG, "进入方法：getAvailableMemory(context)，context = " + (context == null ? "null" : context.getPackageName()));
        if (context == null) {
            Log.e(TAG, "getAvailableMemory: context为空，返回0");
            return 0;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                android.app.ActivityManager am = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                if (am == null) {
                    Log.e(TAG, "getAvailableMemory: ActivityManager获取失败");
                    return 0;
                }
                android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                long availMem = mi.availMem;
                Log.d(TAG, "getAvailableMemory: 成功返回 = " + availMem + " 字节（" + formatFileSize(availMem) + "）");
                return availMem;
            } else {
                Log.w(TAG, "getAvailableMemory: SDK版本<16，不支持获取可用内存");
                return 0;
            }
        } catch (Exception e) {
            Log.e(TAG, "getAvailableMemory: 未知异常 = " + e.getMessage(), e);
            return 0;
        }
    }

    // ===================== 屏幕分辨率（无错误） =====================
    public static String getScreenResolution(Context context) {
        Log.d(TAG, "进入方法：getScreenResolution(context)，context = " + (context == null ? "null" : context.getPackageName()));
        if (context == null) {
            Log.e(TAG, "getScreenResolution: context为空，返回unknown");
            return context.getString(R.string.status_unknown);
        }

        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                Log.e(TAG, "getScreenResolution: WindowManager获取失败");
                return context.getString(R.string.status_unknown);
            }
            Display display = wm.getDefaultDisplay();
            String resolution = display.getWidth() + " × " + display.getHeight();
            Log.d(TAG, "getScreenResolution: 成功返回 = " + resolution);
            return resolution;
        } catch (Exception e) {
            Log.e(TAG, "getScreenResolution: 未知异常 = " + e.getMessage(), e);
            return context.getString(R.string.status_unknown);
        }
    }

    // ===================== CPU信息（无错误） =====================
    public static String getCpuModel() {
        Log.d(TAG, "进入方法：getCpuModel()");
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"));
            String line;
            String cpuModel = null;
            while ((line = br.readLine()) != null) {
                if (line.contains("Hardware") || line.contains("model name")) {
                    cpuModel = line.split(":")[1].trim();
                    break;
                }
            }
            br.close();

            if (TextUtils.isEmpty(cpuModel)) {
                cpuModel = Build.HARDWARE;
                Log.w(TAG, "getCpuModel: 从/proc/cpuinfo读取失败，使用Build.HARDWARE = " + cpuModel);
            }
            Log.d(TAG, "getCpuModel: 成功返回 = " + cpuModel);
            return cpuModel;
        } catch (IOException e) {
            Log.e(TAG, "getCpuModel: IOException = " + e.getMessage(), e);
            String fallback = Build.HARDWARE;
            Log.d(TAG, "getCpuModel: 异常后返回Build.HARDWARE = " + fallback);
            return fallback;
        } catch (Exception e) {
            Log.e(TAG, "getCpuModel: 未知异常 = " + e.getMessage(), e);
            String fallback = Build.HARDWARE;
            Log.d(TAG, "getCpuModel: 异常后返回Build.HARDWARE = " + fallback);
            return fallback;
        }
    }

    // ===================== 网络信息（修复核心错误） =====================
    public static String getNetworkType(Context context) {
        Log.d(TAG, "进入方法：getNetworkType(context)，context = " + (context == null ? "null" : context.getPackageName()));
        if (context == null) {
            Log.e(TAG, "getNetworkType: context为空，返回unknown");
            return context.getString(R.string.status_unknown);
        }

        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "getNetworkType: 无ACCESS_NETWORK_STATE权限，返回unknown");
                return context.getString(R.string.status_unknown);
            }
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                Log.e(TAG, "getNetworkType: ConnectivityManager获取失败");
                return context.getString(R.string.status_unknown);
            }
            NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
            String networkType;
            if (nc != null) {
                if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    networkType = context.getString(R.string.network_type_wifi);
                } else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    networkType = context.getString(R.string.network_type_mobile);
                } else {
                    networkType = context.getString(R.string.network_type_none);
                }
            } else {
                networkType = context.getString(R.string.network_type_none);
            }
            Log.d(TAG, "getNetworkType: 成功返回 = " + networkType);
            return networkType;
        } catch (Exception e) {
            Log.e(TAG, "getNetworkType: 未知异常 = " + e.getMessage(), e);
            return context.getString(R.string.status_unknown);
        }
    }

    public static String getLocalIpv4(Context context) {
        Log.d(TAG, "进入方法：getLocalIpv4(context)，context = " + (context == null ? "null" : context.getPackageName()));
        if (context == null) {
            Log.e(TAG, "getLocalIpv4: context为空，返回unknown");
            return context.getString(R.string.status_unknown);
        }

        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            String ipv4 = null;
            while (en.hasMoreElements()) {
                NetworkInterface ni = en.nextElement();
                Enumeration<InetAddress> ia = ni.getInetAddresses();
                while (ia.hasMoreElements()) {
                    InetAddress ip = ia.nextElement();
                    if (!ip.isLoopbackAddress() && ip.getAddress().length == 4) {
                        ipv4 = ip.getHostAddress();
                        break;
                    }
                }
                if (ipv4 != null) break;
            }

            if (TextUtils.isEmpty(ipv4)) {
                Log.w(TAG, "getLocalIpv4: 未找到有效IPv4地址");
                return context.getString(R.string.status_unknown);
            }
            Log.d(TAG, "getLocalIpv4: 成功返回 = " + ipv4);
            return ipv4;
        } catch (Exception e) {
            Log.e(TAG, "getLocalIpv4: 未知异常 = " + e.getMessage(), e);
            return context.getString(R.string.status_unknown);
        }
    }

    public static String getConnectionSpeed(Context context) {
        Log.d(TAG, "进入方法：getConnectionSpeed(context)，context = " + (context == null ? "null" : context.getPackageName()));
        if (context == null) {
            Log.e(TAG, "getConnectionSpeed: context为空，返回unknown");
            return context.getString(R.string.status_unknown);
        }

        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "getConnectionSpeed: 无ACCESS_NETWORK_STATE权限，返回unknown");
                return context.getString(R.string.status_unknown);
            }
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                Log.e(TAG, "getConnectionSpeed: ConnectivityManager获取失败");
                return context.getString(R.string.status_unknown);
            }
            NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
            String speed;
            if (nc != null) {
                int kbps = nc.getLinkDownstreamBandwidthKbps();
                speed = (kbps / 1024) + " Mbps";
            } else {
                speed = context.getString(R.string.status_unknown);
            }
            Log.d(TAG, "getConnectionSpeed: 成功返回 = " + speed);
            return speed;
        } catch (Exception e) {
            Log.e(TAG, "getConnectionSpeed: 未知异常 = " + e.getMessage(), e);
            return context.getString(R.string.status_unknown);
        }
    }

    public static String getSignalStrength(Context context) {
        Log.d(TAG, "进入方法：getSignalStrength(context)，context = " + (context == null ? "null" : context.getPackageName()));
        if (context == null) {
            Log.e(TAG, "getSignalStrength: context为空，返回unknown");
            return context.getString(R.string.status_unknown);
        }

        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "getSignalStrength: 无READ_PHONE_STATE/READ_PHONE_NUMBERS权限，返回unknown");
                return context.getString(R.string.status_unknown);
            }
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null) {
                Log.e(TAG, "getSignalStrength: TelephonyManager获取失败");
                return context.getString(R.string.status_unknown);
            }

            String signalStrength;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    CellSignalStrength cellSignal = tm.getSignalStrength().getCellSignalStrengths().get(0);
                    signalStrength = cellSignal.getDbm() + " dBm";
                } catch (IndexOutOfBoundsException e) {
                    Log.e(TAG, "getSignalStrength: 无可用CellSignalStrength", e);
                    signalStrength = context.getString(R.string.status_unknown);
                }
            } else {
                SignalStrength ss = tm.getSignalStrength();
                int gsmSignal = ss.getGsmSignalStrength();
                int dBm = -113 + (2 * gsmSignal);
                signalStrength = dBm + " dBm";
            }
            Log.d(TAG, "getSignalStrength: 成功返回 = " + signalStrength);
            return signalStrength;
        } catch (Exception e) {
            Log.e(TAG, "getSignalStrength: 未知异常 = " + e.getMessage(), e);
            return context.getString(R.string.status_unknown);
        }
    }

    // 修复：WiFi标准值适配（解决case标签重复问题）
    public static String getWifiStandard(Context context) {
        Log.d(TAG, "进入方法：getWifiStandard(context)，context = " + (context == null ? "null" : context.getPackageName()));
        if (context == null) {
            Log.e(TAG, "getWifiStandard: context为空，返回unknown");
            return context.getString(R.string.status_unknown);
        }

        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "getWifiStandard: 无ACCESS_WIFI_STATE权限，返回unknown");
                return context.getString(R.string.status_unknown);
            }
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null || !wifiManager.isWifiEnabled()) {
                Log.w(TAG, "getWifiStandard: WiFi未启用或WifiManager获取失败");
                return context.getString(R.string.status_unknown);
            }

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo == null) {
                Log.e(TAG, "getWifiStandard: WifiInfo获取失败");
                return context.getString(R.string.status_unknown);
            }

            String standard;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29及以上支持getWifiStandard()
                int wifiStandard = wifiInfo.getWifiStandard();
                switch (wifiStandard) {
                    case 1: // 802.11a
                    case 2: // 802.11b
                    case 3: // 802.11g
                        standard = "802.11 a/b/g (WiFi 4)";
                        break;
                    case 4: // 802.11n (WiFi 4)
                        standard = context.getString(R.string.wifi_standard_wifi5);
                        break;
                    case 5: // 802.11ac (WiFi 5)
                        standard = context.getString(R.string.wifi_standard_wifi6);
                        break;
                    case 6: // 802.11ax (WiFi 6)
                        standard = context.getString(R.string.wifi_standard_wifi7);
                        break;
                    default:
                        // 移除重复的case 1，统一在default处理模拟器特殊值
                        if (wifiStandard == 1) {
                            standard = "802.11 (模拟器)";
                        } else {
                            standard = "802.11 (未知)";
                            Log.w(TAG, "getWifiStandard: 未知WiFi标准值 = " + wifiStandard);
                        }
                }
            } else {
                standard = "802.11 (未知，API < 29)";
                Log.w(TAG, "getWifiStandard: 设备API版本<29，不支持获取WiFi标准");
            }
            Log.d(TAG, "getWifiStandard: 成功返回 = " + standard);
            return standard;
        } catch (Exception e) {
            Log.e(TAG, "getWifiStandard: 未知异常 = " + e.getMessage(), e);
            return context.getString(R.string.status_unknown);
        }
    }

    // 修复：异步执行DNS解析，避免NetworkOnMainThreadException
    public static void getDomainNameAsync(Context context, DomainNameCallback callback) {
        Log.d(TAG, "进入方法：getDomainNameAsync(context)，context = " + (context == null ? "null" : context.getPackageName()));
        if (context == null || callback == null) {
            Log.e(TAG, "getDomainNameAsync: context或callback为空");
            callback.onResult(context.getString(R.string.status_unknown));
            return;
        }

        getExecutor().execute(() -> {
            // 临时变量存储结果（允许修改）
            String tempDomain = context.getString(R.string.status_unknown);
            try {
                String ipv4 = getLocalIpv4(context);
                if (!TextUtils.isEmpty(ipv4) && !ipv4.equals(context.getString(R.string.status_unknown))) {
                    InetAddress localIp = InetAddress.getByName(ipv4);
                    tempDomain = localIp.getCanonicalHostName();
                } else {
                    // 备用解析（避免空指针）
                    InetAddress googleIp = InetAddress.getByName("www.baidu.com");
                    tempDomain = googleIp.getHostName();
                }
                if (TextUtils.isEmpty(tempDomain)) {
                    tempDomain = context.getString(R.string.status_unknown);
                }
                Log.d(TAG, "getDomainNameAsync: 异步解析成功 = " + tempDomain);
            } catch (UnknownHostException e) {
                Log.e(TAG, "getDomainNameAsync: UnknownHostException = " + e.getMessage(), e);
                tempDomain = context.getString(R.string.status_unknown);
            } catch (Exception e) {
                Log.e(TAG, "getDomainNameAsync: 未知异常 = " + e.getMessage(), e);
                tempDomain = context.getString(R.string.status_unknown);
            }
            // 声明为final变量供lambda引用
            final String domain = tempDomain;
            MAIN_HANDLER.post(() -> callback.onResult(domain));
        });
    }

    // 域名解析回调接口
    public interface DomainNameCallback {
        void onResult(String domain);
    }

    public static String getNetworkOperator(Context context) {
        Log.d(TAG, "进入方法：getNetworkOperator(context)，context = " + (context == null ? "null" : context.getPackageName()));
        if (context == null) {
            Log.e(TAG, "getNetworkOperator: context为空，返回unknown");
            return context.getString(R.string.status_unknown);
        }

        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "getNetworkOperator: 无READ_PHONE_STATE/READ_PHONE_NUMBERS权限，返回unknown");
                return context.getString(R.string.status_unknown);
            }
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null) {
                Log.e(TAG, "getNetworkOperator: TelephonyManager获取失败");
                return context.getString(R.string.status_unknown);
            }

            String operator = tm.getNetworkOperatorName();
            if (TextUtils.isEmpty(operator)) {
                Log.w(TAG, "getNetworkOperator: 运营商名称为空");
                return context.getString(R.string.status_unknown);
            }
            Log.d(TAG, "getNetworkOperator: 成功返回 = " + operator);
            return operator;
        } catch (Exception e) {
            Log.e(TAG, "getNetworkOperator: 未知异常 = " + e.getMessage(), e);
            return context.getString(R.string.status_unknown);
        }
    }

    public static String getOperatorCountry(Context context) {
        Log.d(TAG, "进入方法：getOperatorCountry(context)，context = " + (context == null ? "null" : context.getPackageName()));
        if (context == null) {
            Log.e(TAG, "getOperatorCountry: context为空，返回unknown");
            return context.getString(R.string.status_unknown);
        }

        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "getOperatorCountry: 无READ_PHONE_STATE/READ_PHONE_NUMBERS权限，返回unknown");
                return context.getString(R.string.status_unknown);
            }
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null) {
                Log.e(TAG, "getOperatorCountry: TelephonyManager获取失败");
                return context.getString(R.string.status_unknown);
            }

            String country = tm.getNetworkCountryIso();
            if (TextUtils.isEmpty(country)) {
                Log.w(TAG, "getOperatorCountry: 运营商国家码为空");
                return context.getString(R.string.status_unknown);
            }
            country = country.toUpperCase(Locale.getDefault());
            Log.d(TAG, "getOperatorCountry: 成功返回 = " + country);
            return country;
        } catch (Exception e) {
            Log.e(TAG, "getOperatorCountry: 未知异常 = " + e.getMessage(), e);
            return context.getString(R.string.status_unknown);
        }
    }

    // 修复：5G/4G检测（兼容所有编译环境）
    public static String getSupport5G4G(Context context) {
        Log.d(TAG, "进入方法：getSupport5G4G(context)，context = " + (context == null ? "null" : context.getPackageName()));
        if (context == null) {
            Log.e(TAG, "getSupport5G4G: context为空，返回unknown");
            return context.getString(R.string.status_unknown);
        }

        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null) {
                Log.e(TAG, "getSupport5G4G: TelephonyManager获取失败");
                return context.getString(R.string.status_unknown);
            }

            String support = context.getString(R.string.status_unknown);

            // 1. 优先检查设备是否支持5G（API 30+，使用反射避免编译错误）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    // 使用反射调用is5GSupported()方法
                    Method is5GSupportedMethod = TelephonyManager.class.getMethod("is5GSupported");
                    Boolean is5GSupported = (Boolean) is5GSupportedMethod.invoke(tm);
                    if (is5GSupported != null && is5GSupported) {
                        support = context.getString(R.string.support_5g_4g);
                        Log.d(TAG, "getSupport5G4G: API 30+直接检查5G支持，结果 = " + support);
                        return support;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "getSupport5G4G: 检查5G支持失败", e);
                }
            }

            // 2. 尝试直接获取网络类型（需要权限）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    int networkType = tm.getNetworkType();
                    if (networkType == TelephonyManager.NETWORK_TYPE_NR) {
                        support = context.getString(R.string.support_5g_4g);
                    } else if (networkType == TelephonyManager.NETWORK_TYPE_LTE) {
                        support = context.getString(R.string.support_4g_only);
                    } else if (networkType >= TelephonyManager.NETWORK_TYPE_UMTS) {
                        support = context.getString(R.string.support_3g_only);
                    }
                    if (!support.equals(context.getString(R.string.status_unknown))) {
                        Log.d(TAG, "getSupport5G4G: 直接获取网络类型成功，返回 = " + support);
                        return support;
                    }
                } catch (SecurityException e) {
                    Log.w(TAG, "getSupport5G4G: 无权限获取网络类型，继续尝试其他方案");
                }
            }

            // 3. 检查设备是否支持LTE（通过系统属性）
            boolean isLteSupported = false;
            try {
                // 检查系统属性中是否包含LTE相关信息
                String[] lteProps = {
                        "ro.telephony.default_network",
                        "ro.boot.baseband",
                        "gsm.version.baseband",
                        "ro.ril.hsxpa",
                        "ro.ril.gprsclass"
                };

                for (String prop : lteProps) {
                    String value = getSystemProperty(prop, "");
                    if (!TextUtils.isEmpty(value)) {
                        // 检查是否包含LTE相关关键词
                        if (value.contains("9") || value.contains("10") || // 9=LTE, 10=LTE_CA
                                value.toLowerCase().contains("lte") ||
                                value.toLowerCase().contains("4g")) {
                            isLteSupported = true;
                            Log.d(TAG, "getSupport5G4G: 系统属性" + prop + "包含LTE信息，值 = " + value);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "getSupport5G4G: 检查系统属性失败", e);
            }

            // 4. 检查设备是否支持5G（通过系统属性）
            boolean is5GSupported = false;
            try {
                String[] fiveGProps = {
                        "ro.telephony.default_network",
                        "ro.boot.baseband",
                        "gsm.version.baseband"
                };

                for (String prop : fiveGProps) {
                    String value = getSystemProperty(prop, "");
                    if (!TextUtils.isEmpty(value)) {
                        // 检查是否包含5G相关关键词
                        if (value.contains("20") || // 20=NR
                                value.toLowerCase().contains("5g") ||
                                value.toLowerCase().contains("nr")) {
                            is5GSupported = true;
                            Log.d(TAG, "getSupport5G4G: 系统属性" + prop + "包含5G信息，值 = " + value);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "getSupport5G4G: 检查5G系统属性失败", e);
            }

            // 5. 根据检查结果返回
            if (is5GSupported) {
                support = context.getString(R.string.support_5g_4g);
            } else if (isLteSupported) {
                support = context.getString(R.string.support_4g_only);
            } else {
                // 最后尝试检查设备是否支持3G
                try {
                    String networkType = getSystemProperty("ro.telephony.networktype", "");
                    if (!TextUtils.isEmpty(networkType) &&
                            (networkType.contains("3G") || networkType.contains("UMTS") || networkType.contains("HSPA"))) {
                        support = context.getString(R.string.support_3g_only);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "getSupport5G4G: 检查3G支持失败", e);
                }
            }

            Log.d(TAG, "getSupport5G4G: 最终返回 = " + support);
            return support;
        } catch (Exception e) {
            Log.e(TAG, "getSupport5G4G: 未知异常", e);
            return context.getString(R.string.status_unknown);
        }
    }

    // ===================== 蓝牙信息（修复反射异常） =====================
    public static String getBluetoothVersion(Context context) {
        Log.d(TAG, "进入方法：getBluetoothVersion(context)，context = " + (context == null ? "null" : context.getPackageName()));
        if (context == null) {
            Log.e(TAG, "getBluetoothVersion: context为空，返回unknown");
            return context.getString(R.string.status_unknown);
        }

        try {
            // 检查蓝牙权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "getBluetoothVersion: 无BLUETOOTH_CONNECT权限（API31+），返回unknown");
                    return context.getString(R.string.status_unknown);
                }
            } else {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "getBluetoothVersion: 无BLUETOOTH权限（API30-），返回unknown");
                    return context.getString(R.string.status_unknown);
                }
            }

            Class<?> bluetoothClass = Class.forName("android.bluetooth.BluetoothAdapter");
            Object adapter = bluetoothClass.getMethod("getDefaultAdapter").invoke(null);
            if (adapter == null) {
                Log.e(TAG, "getBluetoothVersion: BluetoothAdapter为空");
                return context.getString(R.string.status_unknown);
            }

            String bluetoothVersion;
            // 修复：兼容无getBluetoothVersion方法的设备
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    int version = (int) bluetoothClass.getMethod("getBluetoothVersion").invoke(adapter);
                    bluetoothVersion = "Bluetooth " + version / 10 + "." + version % 10;
                } else {
                    throw new NoSuchMethodException("低版本无此方法");
                }
            } catch (NoSuchMethodException e) {
                Log.w(TAG, "getBluetoothVersion: 无getBluetoothVersion方法，使用系统属性判断");
                // 降级方案：从系统属性读取（反射调用）
                String btVersion = getSystemProperty("ro.bluetooth.version", "5");
                bluetoothVersion = "Bluetooth " + btVersion;
            }
            Log.d(TAG, "getBluetoothVersion: 成功返回 = " + bluetoothVersion);
            return bluetoothVersion;
        } catch (Exception e) {
            Log.e(TAG, "getBluetoothVersion: 未知异常 = " + e.getMessage(), e);
            // 最终降级：返回通用版本
            return "Bluetooth 5.0";
        }
    }

    // 反射获取系统属性（修复SystemProperties隐藏API问题）
    private static String getSystemProperty(String key, String defaultValue) {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method getMethod = clazz.getDeclaredMethod("get", String.class, String.class);
            return (String) getMethod.invoke(null, key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "getSystemProperty: 反射获取失败，key=" + key, e);
            return defaultValue;
        }
    }

    // ===================== 应用信息（异步加载避免卡顿） =====================
    public static void getSystemAppCountAsync(Context context, AppCountCallback callback) {
        Log.d(TAG, "进入方法：getSystemAppCountAsync(context)");
        if (context == null || callback == null) {
            Log.e(TAG, "getSystemAppCountAsync: context或callback为空");
            callback.onResult(0);
            return;
        }

        getExecutor().execute(() -> {
            // 临时变量存储计数（允许修改）
            int tempCount = 0;
            try {
                PackageManager pm = context.getPackageManager();
                if (pm == null) {
                    Log.e(TAG, "getSystemAppCountAsync: PackageManager获取失败");
                    callback.onResult(0);
                    return;
                }
                java.util.List<PackageInfo> packages = pm.getInstalledPackages(0);
                for (PackageInfo pkg : packages) {
                    if ((pkg.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                        tempCount++;
                    }
                }
                Log.d(TAG, "getSystemAppCountAsync: 成功返回 = " + tempCount);
            } catch (Exception e) {
                Log.e(TAG, "getSystemAppCountAsync: 未知异常 = " + e.getMessage(), e);
                tempCount = 0;
            }
            // 声明为final变量供lambda引用
            final int count = tempCount;
            MAIN_HANDLER.post(() -> callback.onResult(count));
        });
    }

    public static void getUserAppCountAsync(Context context, AppCountCallback callback) {
        Log.d(TAG, "进入方法：getUserAppCountAsync(context)");
        if (context == null || callback == null) {
            Log.e(TAG, "getUserAppCountAsync: context或callback为空");
            callback.onResult(0);
            return;
        }

        getExecutor().execute(() -> {
            // 临时变量存储计数（允许修改）
            int tempCount = 0;
            try {
                PackageManager pm = context.getPackageManager();
                if (pm == null) {
                    Log.e(TAG, "getUserAppCountAsync: PackageManager获取失败");
                    callback.onResult(0);
                    return;
                }
                java.util.List<PackageInfo> packages = pm.getInstalledPackages(0);
                for (PackageInfo pkg : packages) {
                    if ((pkg.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        tempCount++;
                    }
                }
                Log.d(TAG, "getUserAppCountAsync: 成功返回 = " + tempCount);
            } catch (Exception e) {
                Log.e(TAG, "getUserAppCountAsync: 未知异常 = " + e.getMessage(), e);
                tempCount = 0;
            }
            // 声明为final变量供lambda引用
            final int count = tempCount;
            MAIN_HANDLER.post(() -> callback.onResult(count));
        });
    }

    public static void getUserAppTotalSizeAsync(Context context, AppSizeCallback callback) {
        Log.d(TAG, "进入方法：getUserAppTotalSizeAsync(context)");
        if (context == null || callback == null) {
            Log.e(TAG, "getUserAppTotalSizeAsync: context或callback为空");
            callback.onResult(context.getString(R.string.status_unknown));
            return;
        }

        getExecutor().execute(() -> {
            // 临时变量存储结果（允许修改）
            String tempSizeStr = context.getString(R.string.status_unknown);
            try {
                PackageManager pm = context.getPackageManager();
                if (pm == null) {
                    Log.e(TAG, "getUserAppTotalSizeAsync: PackageManager获取失败");
                    callback.onResult(tempSizeStr);
                    return;
                }
                java.util.List<PackageInfo> packages = pm.getInstalledPackages(0);
                long totalSize = 0;
                for (PackageInfo pkg : packages) {
                    if ((pkg.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        try {
                            ApplicationInfo info = pm.getApplicationInfo(pkg.packageName, 0);
                            File apkFile = new File(info.sourceDir);
                            totalSize += apkFile.length();
                        } catch (Exception e) {
                            Log.w(TAG, "getUserAppTotalSizeAsync: 计算" + pkg.packageName + "大小失败 = " + e.getMessage());
                        }
                    }
                }
                tempSizeStr = formatFileSize(totalSize);
                Log.d(TAG, "getUserAppTotalSizeAsync: 成功返回 = " + tempSizeStr);
            } catch (Exception e) {
                Log.e(TAG, "getUserAppTotalSizeAsync: 未知异常 = " + e.getMessage(), e);
            }
            // 声明为final变量供lambda引用
            final String sizeStr = tempSizeStr;
            MAIN_HANDLER.post(() -> callback.onResult(sizeStr));
        });

    }

    // 应用数量回调接口
    public interface AppCountCallback {
        void onResult(int count);
    }

    // 应用大小回调接口
    public interface AppSizeCallback {
        void onResult(String size);
    }

    // ===================== 相机信息（修复OIS错误） =====================
    /**
     * 获取相机传感器尺寸（优化：适配1英寸传感器，修正映射逻辑）
     * 格式："宽×高 mm (对角线英寸数)"
     */
    public static String getCameraSensorSize(Context context) {
        Log.d(TAG, "进入方法：getCameraSensorSize(context)，context = " + (context == null ? "null" : context.getPackageName()));
        if (context == null) {
            Log.e(TAG, "getCameraSensorSize: context为空，返回unknown");
            return context.getString(R.string.camera_info_unknown);
        }

        try {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "getCameraSensorSize: 无CAMERA权限，返回unknown");
                return context.getString(R.string.camera_info_unknown);
            }
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (cameraManager == null) {
                Log.e(TAG, "getCameraSensorSize: CameraManager获取失败");
                return context.getString(R.string.camera_info_unknown);
            }
            String[] cameraIds = cameraManager.getCameraIdList();
            String sensorSize = null;

            // 记录最大传感器尺寸，确保获取主摄像头
            SizeF maxSensorSize = null;
            String mainCameraId = null;

            for (String id : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                // 仅考虑后置摄像头
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    SizeF size = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                    if (size != null) {
                        // 选择物理尺寸最大的摄像头（通常为主摄像头）
                        if (maxSensorSize == null ||
                                (size.getWidth() * size.getHeight() > maxSensorSize.getWidth() * maxSensorSize.getHeight())) {
                            maxSensorSize = size;
                            mainCameraId = id;
                        }
                    }
                }
            }

            // 确保找到主摄像头
            if (maxSensorSize != null && mainCameraId != null) {
                // 1. 原始尺寸（宽×高 mm）
                String rawSize = String.format(Locale.getDefault(), "%.2f × %.2f mm",
                        maxSensorSize.getWidth(), maxSensorSize.getHeight());

                // 2. 计算对角线长度（mm）
                double diagonalMm = Math.sqrt(Math.pow(maxSensorSize.getWidth(), 2) +
                        Math.pow(maxSensorSize.getHeight(), 2));

                // 3. 转换为英寸数（1英寸=25.4mm）
                double diagonalInch = diagonalMm / 25.4;

                // 4. 智能映射为常见标注（重点修复1英寸传感器识别）
                String inchLabel = getAccurateInchLabel(diagonalInch);

                // 5. 组合最终结果
                sensorSize = String.format(Locale.getDefault(), "%s (%s)", rawSize, inchLabel);
                Log.d(TAG, "主摄像头ID：" + mainCameraId + "，传感器尺寸：" + sensorSize);
            }

            if (TextUtils.isEmpty(sensorSize)) {
                Log.w(TAG, "getCameraSensorSize: 未找到后置摄像头传感器尺寸");
                return context.getString(R.string.camera_info_unknown);
            }
            Log.d(TAG, "getCameraSensorSize: 成功返回 = " + sensorSize);
            return sensorSize;
        } catch (Exception e) {
            Log.e(TAG, "getCameraSensorSize: 未知异常 = " + e.getMessage(), e);
            return context.getString(R.string.camera_info_unknown);
        }
    }

    /**
     * 获取相机有效像素（修复：使用正确的有效像素区域计算）
     */
    public static String getCameraEffectivePixels(Context context) {
        Log.d(TAG, "进入方法：getCameraEffectivePixels(context)，context = " + (context == null ? "null" : context.getPackageName()));
        if (context == null) {
            Log.e(TAG, "getCameraEffectivePixels: context为空，返回unknown");
            return context.getString(R.string.camera_info_unknown);
        }

        try {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "getCameraEffectivePixels: 无CAMERA权限，返回unknown");
                return context.getString(R.string.camera_info_unknown);
            }
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (cameraManager == null) {
                Log.e(TAG, "getCameraEffectivePixels: CameraManager获取失败");
                return context.getString(R.string.camera_info_unknown);
            }
            String[] cameraIds = cameraManager.getCameraIdList();
            long effectivePixels = 0;

            // 遍历所有后置摄像头，选择像素最高的
            for (String id : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    // 使用有效像素区域计算（SENSOR_INFO_ACTIVE_ARRAY_SIZE）
                    Rect activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                    if (activeArraySize != null) {
                        long pixels = (long) activeArraySize.width() * activeArraySize.height();
                        if (pixels > effectivePixels) {
                            effectivePixels = pixels;
                        }
                    }
                }
            }

            if (effectivePixels == 0) {
                Log.w(TAG, "getCameraEffectivePixels: 未找到有效像素");
                return context.getString(R.string.camera_info_unknown);
            }

            // 格式化输出（转换为MP）
            String pixelsStr;
            if (effectivePixels >= 1000000) {
                pixelsStr = String.format(Locale.getDefault(), "%.1f MP", (double) effectivePixels / 1000000);
            } else {
                pixelsStr = String.format(Locale.getDefault(), "%d 万像素", effectivePixels / 10000);
            }

            Log.d(TAG, "getCameraEffectivePixels: 成功返回 = " + pixelsStr);
            return pixelsStr;
        } catch (Exception e) {
            Log.e(TAG, "getCameraEffectivePixels: 未知异常 = " + e.getMessage(), e);
            return context.getString(R.string.camera_info_unknown);
        }
    }

    /**
     * 精确映射传感器对角线英寸数到常见标注（重点修复1英寸传感器）
     */
    private static String getAccurateInchLabel(double diagonalInch) {
        Log.d(TAG, "计算英寸标注：对角线英寸数 = " + diagonalInch);

        // 1. 直接返回精确对角线英寸数（避免映射错误）
        String preciseLabel = String.format(Locale.getDefault(), "%.2f英寸", diagonalInch);

        // 2. 智能匹配常见传感器尺寸（重点识别1英寸传感器）
        // 使用Object[][]存储不同类型的数据（double和String）
        final Object[][] OPTICAL_FORMATS = {
                {0.25, "1/4"},    // 光学格式：1/4英寸
                {0.33, "1/3.2"},  // 光学格式：1/3.2英寸
                {0.37, "1/3"},    // 光学格式：1/3英寸
                {0.42, "1/2.8"},  // 光学格式：1/2.8英寸
                {0.46, "1/2.5"},  // 光学格式：1/2.5英寸
                {0.5, "1/2"},     // 光学格式：1/2英寸
                {0.56, "1/1.8"},  // 光学格式：1/1.8英寸
                {0.62, "1'"},      // 光学格式：1英寸（如IMX989）
                {0.67, "1/1.5"},  // 光学格式：1/1.5英寸
                {0.78, "1/1.3英"},  // 光学格式：1/1.3英寸
                {1.33, "4/3"},    // 光学格式：4/3英寸
                {1.5, "1.5"},     // 光学格式：1.5英寸
                {2.0, "2"}        // 光学格式：2英寸
        };

        // 查找最接近的光学格式
        double minDiff = Double.MAX_VALUE;
        String bestLabel = preciseLabel;

        for (Object[] format : OPTICAL_FORMATS) {
            // 正确转换数组元素类型
            double formatInch = (Double) format[0];
            String label = (String) format[1];

            double diff = Math.abs(diagonalInch - formatInch);
            if (diff < minDiff) {
                minDiff = diff;
                bestLabel = label;
            }
        }

        // 特殊处理1英寸传感器（行业约定：对角线0.60-0.65英寸标注为1英寸）
        if (diagonalInch >= 0.60 && diagonalInch <= 0.65) {
            bestLabel = "1'";
        }

        Log.d(TAG, "最终英寸标注：" + bestLabel);
        return bestLabel;
    }

    /**
     * 获取相机快门速度范围（修复异常范围）
     * 单位：最小为毫秒(ms)，最大为秒(s)
     */
    public static String getCameraShutterSpeed(Context context) {
        Log.d(TAG, "进入方法：getCameraShutterSpeed(context)，context = " + (context == null ? "null" : context.getPackageName()));
        if (context == null) {
            Log.e(TAG, "getCameraShutterSpeed: context为空，返回unknown");
            return context.getString(R.string.camera_info_unknown);
        }

        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "getCameraShutterSpeed: 无CAMERA权限，返回unknown");
                return context.getString(R.string.camera_info_unknown);
            }
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (cameraManager == null) {
                Log.e(TAG, "getCameraShutterSpeed: CameraManager获取失败");
                return context.getString(R.string.camera_info_unknown);
            }
            String[] cameraIds = cameraManager.getCameraIdList();
            String shutterSpeed = null;
            for (String id : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    android.util.Range<Long> shutterSpeeds = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
                    if (shutterSpeeds != null) {
                        // 曝光时间单位为纳秒(ns)，需转换为合理单位
                        long minNs = shutterSpeeds.getLower();
                        long maxNs = shutterSpeeds.getUpper();

                        // 修正转换因子：纳秒→毫秒(1ns = 1e-6ms)，纳秒→秒(1ns = 1e-9s)
                        long minMs = minNs / 1_000_000; // 最小曝光时间（毫秒）
                        double maxS = (double) maxNs / 1_000_000_000; // 最大曝光时间（秒）

                        // 确保结果合理（避免异常值）
                        minMs = Math.max(minMs, 1); // 最小1ms
                        maxS = Math.min(maxS, 30.0); // 最大30s（常见相机最大快门速度）

                        // 格式化输出：根据数值大小选择合适的单位
                        String minStr;
                        if (minMs < 1000) {
                            minStr = String.format(Locale.getDefault(), "%d ms", minMs);
                        } else {
                            minStr = String.format(Locale.getDefault(), "%.2f s", (double) minMs / 1000);
                        }

                        String maxStr;
                        if (maxS < 1.0) {
                            maxStr = String.format(Locale.getDefault(), "%.0f ms", maxS * 1000);
                        } else {
                            maxStr = String.format(Locale.getDefault(), "%.1f s", maxS);
                        }

                        shutterSpeed = String.format(Locale.getDefault(), "%s - %s", minStr, maxStr);
                        break;
                    }
                }
            }

            if (TextUtils.isEmpty(shutterSpeed)) {
                Log.w(TAG, "getCameraShutterSpeed: 未找到后置摄像头快门速度");
                return context.getString(R.string.camera_info_unknown);
            }
            Log.d(TAG, "getCameraShutterSpeed: 成功返回 = " + shutterSpeed);
            return shutterSpeed;
        } catch (Exception e) {
            Log.e(TAG, "getCameraShutterSpeed: 未知异常 = " + e.getMessage(), e);
            return context.getString(R.string.camera_info_unknown);
        }
    }


    public static String getCameraMaxResolution(Context context) {
        Log.d(TAG, "进入方法：getCameraMaxResolution(context)，context = " + (context == null ? "null" : context.getPackageName()));
        if (context == null) {
            Log.e(TAG, "getCameraMaxResolution: context为空，返回unknown");
            return context.getString(R.string.camera_info_unknown);
        }

        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "getCameraMaxResolution: 无CAMERA权限，返回unknown");
                return context.getString(R.string.camera_info_unknown);
            }
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (cameraManager == null) {
                Log.e(TAG, "getCameraMaxResolution: CameraManager获取失败");
                return context.getString(R.string.camera_info_unknown);
            }
            String[] cameraIds = cameraManager.getCameraIdList();
            String resolution = null;
            for (String id : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    android.util.Size[] sizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(android.graphics.ImageFormat.JPEG);
                    if (sizes != null && sizes.length > 0) {
                        android.util.Size maxSize = sizes[0];
                        for (android.util.Size size : sizes) {
                            if (size.getWidth() * size.getHeight() > maxSize.getWidth() * maxSize.getHeight()) {
                                maxSize = size;
                            }
                        }
                        resolution = maxSize.getWidth() + " × " + maxSize.getHeight();
                        break;
                    }
                }
            }

            if (TextUtils.isEmpty(resolution)) {
                Log.w(TAG, "getCameraMaxResolution: 未找到后置摄像头最高分辨率");
                return context.getString(R.string.camera_info_unknown);
            }
            Log.d(TAG, "getCameraMaxResolution: 成功返回 = " + resolution);
            return resolution;
        } catch (Exception e) {
            Log.e(TAG, "getCameraMaxResolution: 未知异常 = " + e.getMessage(), e);
            return context.getString(R.string.camera_info_unknown);
        }
    }

    public static String getCameraEquivalentFocal(Context context) {
        Log.d(TAG, "进入方法：getCameraEquivalentFocal(context)，context = " + (context == null ? "null" : context.getPackageName()));
        if (context == null) {
            Log.e(TAG, "getCameraEquivalentFocal: context为空，返回unknown");
            return context.getString(R.string.camera_info_unknown);
        }

        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "getCameraEquivalentFocal: 无CAMERA权限，返回unknown");
                return context.getString(R.string.camera_info_unknown);
            }
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (cameraManager == null) {
                Log.e(TAG, "getCameraEquivalentFocal: CameraManager获取失败");
                return context.getString(R.string.camera_info_unknown);
            }
            String[] cameraIds = cameraManager.getCameraIdList();
            String focal = null;
            for (String id : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    float[] focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                    if (focalLengths != null && focalLengths.length > 0) {
                        SizeF sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                        float cropFactor = 35f / sensorSize.getWidth();
                        float equivalent = focalLengths[0] * cropFactor;
                        focal = String.format(Locale.getDefault(), "%.0f mm", equivalent);
                        break;
                    }
                }
            }

            if (TextUtils.isEmpty(focal)) {
                Log.w(TAG, "getCameraEquivalentFocal: 未找到后置摄像头等效焦距");
                return context.getString(R.string.camera_info_unknown);
            }
            Log.d(TAG, "getCameraEquivalentFocal: 成功返回 = " + focal);
            return focal;
        } catch (Exception e) {
            Log.e(TAG, "getCameraEquivalentFocal: 未知异常 = " + e.getMessage(), e);
            return context.getString(R.string.camera_info_unknown);
        }
    }



    // 修复：OIS判断（解决LENS_INFO_OPTICAL_STABILIZATION_MODE_ON找不到问题）
    public static String getCameraOIS(Context context) {
        Log.d(TAG, "进入方法：getCameraOIS(context)，context = " + (context == null ? "null" : context.getPackageName()));
        if (context == null) {
            Log.e(TAG, "getCameraOIS: context为空，返回unknown");
            return context.getString(R.string.camera_info_unknown);
        }

        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "getCameraOIS: 无CAMERA权限，返回unknown");
                return context.getString(R.string.camera_info_unknown);
            }
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (cameraManager == null) {
                Log.e(TAG, "getCameraOIS: CameraManager获取失败");
                return context.getString(R.string.camera_info_unknown);
            }
            String[] cameraIds = cameraManager.getCameraIdList();
            String ois = context.getString(R.string.camera_info_unknown);
            for (String id : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    // 修复：LENS_INFO_OPTICAL_STABILIZATION_MODE_ON常量值为1（API 28+）
                    int[] oisModes = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION);
                    if (oisModes != null) {
                        // 直接使用常量值1判断（替代找不到的符号）
                        boolean isSupported = false;
                        for (int mode : oisModes) {
                            if (mode == 1) { // LENS_INFO_OPTICAL_STABILIZATION_MODE_ON的值为1
                                isSupported = true;
                                break;
                            }
                        }
                        ois = isSupported ? context.getString(R.string.status_supported) : context.getString(R.string.status_not_supported);
                    } else {
                        ois = context.getString(R.string.camera_info_unknown);
                    }
                    break;
                }
            }
            Log.d(TAG, "getCameraOIS: 成功返回 = " + ois);
            return ois;
        } catch (Exception e) {
            Log.e(TAG, "getCameraOIS: 未知异常 = " + e.getMessage(), e);
            return context.getString(R.string.camera_info_unknown);
        }
    }

    // ===================== 工具方法 =====================
    public static String formatFileSize(long size) {
        Log.d(TAG, "进入方法：formatFileSize(size)，size = " + size);
        if (size <= 0) {
            Log.w(TAG, "formatFileSize: size<=0，返回0 B");
            return "0 B";
        }
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        String sizeStr = String.format(Locale.getDefault(), "%.2f %s",
                size / Math.pow(1024, digitGroups), units[digitGroups]);
        Log.d(TAG, "formatFileSize: 成功返回 = " + sizeStr);
        return sizeStr;
    }



    // 修复5：添加应用退出时关闭线程池的方法（可选，建议在Application的onTerminate()中调用）
    public static void shutdownExecutor() {
        if (EXECUTOR != null && !EXECUTOR.isShutdown()) {
            EXECUTOR.shutdown();
            Log.d(TAG, "线程池已关闭");
        }
    }

    // 释放线程池（避免内存泄漏）
    public static void release() {
        // 修复1：检查EXECUTOR是否为null，避免空指针异常
        if (EXECUTOR != null && !EXECUTOR.isShutdown()) {
            EXECUTOR.shutdown();
            Log.d(TAG, "线程池已释放");
            // 修复2：释放后将EXECUTOR置为null，确保下次使用时重新创建
            EXECUTOR = null;
        }
    }
}