package utils;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.util.Log;

import com.mingbikes.acquisition.Acquisition;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * device info utils
 */
public class DeviceInfo {

    private static int startTime = TimeUtils.currentTimestamp();
    private static long totalMemory = 0;

    /**
     * return the display name of the current operating system.
     */
    public static String getOS() {
        return "Android";
    }

    /**
     * return the current operating system version
     */
    public static String getOSVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    /**
     * return the current device model.
     */
    public static String getDevice() {
        return android.os.Build.MODEL;
    }

    /**
     * return the current device density
     */
    public static String getDensity(final Context context) {

        String densityStr = "";

        final int density = context.getResources().getDisplayMetrics().densityDpi;
        switch (density) {
            case DisplayMetrics.DENSITY_LOW:
                densityStr = "LDPI";
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                densityStr = "MDPI";
                break;
            case DisplayMetrics.DENSITY_TV:
                densityStr = "TVDPI";
                break;
            case DisplayMetrics.DENSITY_HIGH:
                densityStr = "HDPI";
                break;
            //todo uncomment in android sdk 25
            //case DisplayMetrics.DENSITY_260:
            //    densityStr = "XHDPI";
            //    break;
            case DisplayMetrics.DENSITY_280:
                densityStr = "XHDPI";
                break;
            //todo uncomment in android sdk 25
            //case DisplayMetrics.DENSITY_300:
            //    densityStr = "XHDPI";
            //    break;
            case DisplayMetrics.DENSITY_XHIGH:
                densityStr = "XHDPI";
                break;
            //todo uncomment in android sdk 25
            //case DisplayMetrics.DENSITY_340:
            //    densityStr = "XXHDPI";
            //    break;
            case DisplayMetrics.DENSITY_360:
                densityStr = "XXHDPI";
                break;
            case DisplayMetrics.DENSITY_400:
                densityStr = "XXHDPI";
                break;
            case DisplayMetrics.DENSITY_420:
                densityStr = "XXHDPI";
                break;
            case DisplayMetrics.DENSITY_XXHIGH:
                densityStr = "XXHDPI";
                break;
            case DisplayMetrics.DENSITY_560:
                densityStr = "XXXHDPI";
                break;
            case DisplayMetrics.DENSITY_XXXHIGH:
                densityStr = "XXXHDPI";
                break;
            default:
                densityStr = "other";
                break;
        }
        return densityStr;
    }

    /**
     * return the current locale (ex. "zh").
     */
    public static String getLocale() {
        final Locale locale = Locale.getDefault();
        return locale.getLanguage() + "_" + locale.getCountry();
    }

    public static String getAppVersion(final Context context) {
        String result = Acquisition.DEFAULT_APP_VERSION;
        try {
            result = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        }
        catch (PackageManager.NameNotFoundException e) {
            if (Acquisition.sharedInstance().isLoggingEnabled()) {
                Log.i(Acquisition.TAG, "No app version found");
            }
        }
        return result;
    }

    /**
     * return the package name of the app that installed this app
     */
    public static String getPackageName(final Context context) {
        String result = "";
        if(android.os.Build.VERSION.SDK_INT >= 3 ) {
            try {
                result = context.getPackageManager().getInstallerPackageName(context.getPackageName());
            } catch (Exception e) {
                if (Acquisition.sharedInstance().isLoggingEnabled()) {
                    Log.i(Acquisition.TAG, "Can't get Installer package");
                }
            }
            if (result == null || result.length() == 0) {
                result = "";
                if (Acquisition.sharedInstance().isLoggingEnabled()) {
                    Log.i(Acquisition.TAG, "No store found");
                }
            }
        }
        return result;
    }

    /**
     * Returns the current device manufacturer.
     */
    static String getManufacturer() {
        return android.os.Build.MANUFACTURER;
    }

    /**
     * return the current device cpu.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static String getCpu() {
        if(android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP )
            return android.os.Build.CPU_ABI;
        else
            return Build.SUPPORTED_ABIS[0];
    }

    /**
     * return the current device openGL version.
     */
    public static String getOpenGL(Context context) {
        PackageManager packageManager = context.getPackageManager();
        FeatureInfo[] featureInfos = packageManager.getSystemAvailableFeatures();
        if (featureInfos != null && featureInfos.length > 0) {
            for (FeatureInfo featureInfo : featureInfos) {
                // Null feature name means this feature is the open gl es version feature.
                if (featureInfo.name == null) {
                    if (featureInfo.reqGlEsVersion != FeatureInfo.GL_ES_VERSION_UNDEFINED) {
                        return Integer.toString((featureInfo.reqGlEsVersion & 0xffff0000) >> 16);
                    } else {
                        return "1"; // Lack of property means OpenGL ES version 1
                    }
                }
            }
        }
        return "1";
    }

    /**
     * return the current device RAM amount.
     */
    public static String getRamCurrent(Context context) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        return Long.toString(getTotalRAM() - (mi.availMem / 1048576L));
    }

    /**
     * return the total device RAM amount.
     */
    public static String getRamTotal(Context context) {
        return Long.toString(getTotalRAM());
    }

    private static long getTotalRAM() {
        if(totalMemory == 0) {
            RandomAccessFile reader = null;
            String load = null;
            try {
                reader = new RandomAccessFile("/proc/meminfo", "r");
                load = reader.readLine();

                // Get the Number value from the string
                Pattern p = Pattern.compile("(\\d+)");
                Matcher m = p.matcher(load);
                String value = "";
                while (m.find()) {
                    value = m.group(1);
                }
                try {
                    totalMemory = Long.parseLong(value) / 1024;
                }catch(NumberFormatException ex){
                    totalMemory = 0;
                }
            } catch (IOException ex) {
                try {
                    if(reader != null) {
                        reader.close();
                    }
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
                ex.printStackTrace();
            }
            finally {
                try {
                    if(reader != null) {
                        reader.close();
                    }
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        }
        return totalMemory;
    }

    /**
     * return the current device disk space.
     */
    @TargetApi(18)
    public static String getDiskCurrent() {
        if(android.os.Build.VERSION.SDK_INT < 18 ) {
            StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
            long   total  = ((long)statFs.getBlockCount() * (long)statFs.getBlockSize());
            long   free   = ((long)statFs.getAvailableBlocks() * (long)statFs.getBlockSize());
            return Long.toString((total - free)/ 1048576L);
        }
        else{
            StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
            long   total  = (statFs.getBlockCountLong() * statFs.getBlockSizeLong());
            long   free   = (statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong());
            return Long.toString((total - free) / 1048576L);
        }
    }

    /**
     * return the current device disk space.
     */
    @TargetApi(18)
    public static String getDiskTotal() {
        if(android.os.Build.VERSION.SDK_INT < 18 ) {
            StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
            long   total  = ((long)statFs.getBlockCount() * (long)statFs.getBlockSize());
            return Long.toString(total/ 1048576L);
        }
        else{
            StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
            long   total  = (statFs.getBlockCountLong() * statFs.getBlockSizeLong());
            return Long.toString(total/ 1048576L);
        }
    }

    /**
     * return the current device battery level.
     */
    public static String getBatteryLevel(Context context) {
        try {
            Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if(batteryIntent != null) {
                int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                // Error checking that probably isn't needed but I added just in case.
                if (level > -1 && scale > 0) {
                    return Float.toString(((float) level / (float) scale) * 100.0f);
                }
            }
        }
        catch(Exception e){
            if (Acquisition.sharedInstance().isLoggingEnabled()) {
                Log.i(Acquisition.TAG, "Can't get batter level");
            }
        }

        return null;
    }

    /**
     * get app's running time before crashing.
     */
    public static String getRunningTime() {
        return Integer.toString(TimeUtils.currentTimestamp() - startTime);
    }

    /**
     * return the current device orientation.
     */
    public static String getOrientation(Context context) {
        int orientation = context.getResources().getConfiguration().orientation;
        switch(orientation)
        {
            case  Configuration.ORIENTATION_LANDSCAPE:
                return "Landscape";
            case Configuration.ORIENTATION_PORTRAIT:
                return "Portrait";
            case Configuration.ORIENTATION_SQUARE:
                return "Square";
            case Configuration.ORIENTATION_UNDEFINED:
                return "Unknown";
            default:
                return null;
        }
    }

    /**
     * check if device is rooted.
     */
    public static String isRooted() {
        String[] paths = { "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
                "/system/bin/failsafe/su", "/data/local/su" };
        for (String path : paths) {
            if (new File(path).exists()) return "true";
        }
        return "false";
    }

    /**
     * check if device is online.
     */
    public static String isOnline(Context context) {
        try {
            ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (conMgr != null && conMgr.getActiveNetworkInfo() != null
                    && conMgr.getActiveNetworkInfo().isAvailable()
                    && conMgr.getActiveNetworkInfo().isConnected()) {

                return "true";
            }
            return "false";
        }
        catch(Exception e){
            if (Acquisition.sharedInstance().isLoggingEnabled()) {
                Log.w(Acquisition.TAG, "Got exception determining connectivity", e);
            }
        }
        return null;
    }

    /**
     * check if device is muted.
     */
    public static String isMuted(Context context) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        switch( audio.getRingerMode() ){
            case AudioManager.RINGER_MODE_SILENT:
                return "true";
            case AudioManager.RINGER_MODE_VIBRATE:
                return "true";
            default:
                return "false";
        }
    }
}
