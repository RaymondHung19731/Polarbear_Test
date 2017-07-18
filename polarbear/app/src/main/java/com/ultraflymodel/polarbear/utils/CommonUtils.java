package com.ultraflymodel.polarbear.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Typeface;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.view.inputmethod.InputMethodManager;

import com.ultraflymodel.polarbear.R;
import com.ultraflymodel.polarbear.common.Constants;
import com.ultraflymodel.polarbear.common.DBA;
import com.ultraflymodel.polarbear.common.HILog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by William on 2016/4/19.
 */
public class CommonUtils {
    private static final String TAG = CommonUtils.class.getSimpleName();

    public static int unsignedToBytes(byte b) {
        return b & 0xFF;
    }

    public static String getWifiName(Context context) {
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (manager.isWifiEnabled()) {
            WifiInfo wifiInfo = manager.getConnectionInfo();
            if (wifiInfo != null) {
                NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
                if (state == NetworkInfo.DetailedState.CONNECTED || state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                    return wifiInfo.getSSID();
                }
            }
        }
        return null;
    }

    public  static String getTimeCurrentTimeZone(long timestamp) {
        try{
            Calendar calendar = Calendar.getInstance();
            TimeZone tz = TimeZone.getDefault();
            calendar.setTimeInMillis(timestamp * 1000);
            calendar.add(Calendar.MILLISECOND, tz.getOffset(calendar.getTimeInMillis()));
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
            Date currenTimeZone = (Date) calendar.getTime();
            return sdf.format(currenTimeZone);
        }catch (Exception e) {
        }
        return "";
    }

    public  static String getLongTimeCurrentTimeZone(long timestamp) {
        try{
            Calendar calendar = Calendar.getInstance();
            TimeZone tz = TimeZone.getDefault();
            calendar.setTimeInMillis(timestamp * 1000);
            calendar.add(Calendar.MILLISECOND, tz.getOffset(calendar.getTimeInMillis()));
            SimpleDateFormat sdf = new SimpleDateFormat("E, MMM dd, yyyy a hh:mm");
            Date currenTimeZone = (Date) calendar.getTime();
            return sdf.format(currenTimeZone);
        }catch (Exception e) {
        }
        return "";
    }
    public static Typeface getGothicFont(Context m_ctx){
        Typeface myfont;
        myfont = Typeface.createFromAsset(m_ctx.getAssets(), "century-gothic.ttf");
        return myfont;
    }

    public static File openFile(Context m_ctx, String fileName){
        HILog.d(TAG, "openFile: fileName = " + fileName);
        File file;
        String sdCard = Environment.getExternalStorageDirectory().getAbsolutePath();
        String path = sdCard + "/" + m_ctx.getResources().getString(R.string.app_name);
        HILog.d(TAG, "openFile: path =  " + path + ", fileName = " + fileName);
        file = new File(path, fileName);
        return file;
    }

    /**
     * <p>Checks if two dates are on the same day ignoring time.</p>
     * @param date1  the first date, not altered, not null
     * @param date2  the second date, not altered, not null
     * @return true if they represent the same day
     * @throws IllegalArgumentException if either date is <code>null</code>
     */
    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return isSameDay(cal1, cal2, Constants.HIS_DAY);
    }

    /**
     * <p>Checks if two calendars represent the same day ignoring time.</p>
     * @param cal1  the first calendar, not altered, not null
     * @param cal2  the second calendar, not altered, not null
     * @return true if they represent the same day
     * @throws IllegalArgumentException if either calendar is <code>null</code>
     */
    public static boolean isSameDay(Calendar cal1, Calendar cal2, int m_dmy) {
        boolean value = false;
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        switch(m_dmy){
            case Constants.HIS_DAY:
                value = (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                        cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
                break;
            case Constants.HIS_MONTH:
                value = (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                        cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                        cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH));
                break;
            case Constants.HIS_YEAR:
                value = (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                        cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR));
                break;
        }
        return value;
    }

    public static boolean isCachefileFresh(Context m_ctx, String cachefile){
        boolean value = false;
        String sdCard = Environment.getExternalStorageDirectory().getAbsolutePath();
        String filepath = sdCard + "/" + m_ctx.getResources().getString(R.string.app_name) + "/" + cachefile;
        HILog.d(TAG, "isCachefileFresh : " + filepath );
        File file = new File(filepath);
        Date lastModDate = new Date(file.lastModified());
        HILog.d(TAG, "isCachefileFresh :" + lastModDate.toString());

        Date now = new Date();
        value = isSameDay(lastModDate, now);

        return value;
    }

    public static File createFile(Context m_ctx, String fileName){
        HILog.d(TAG, "createFile: fileName = " + fileName);
        File file;
        String sdCard = Environment.getExternalStorageDirectory().getAbsolutePath();
        String path = sdCard + "/" + m_ctx.getResources().getString(R.string.app_name);
        HILog.d(TAG, "createFile: path = " + path + ", fileName = " + fileName);
        File dir = new File(path);
        dir.mkdirs();
        file = new File(dir, fileName);
        return file;
    }

    public static boolean DeleteAnyfile(Context m_ctx, String path, String filename){
        boolean value = false;
        HILog.d(TAG, "DeleteAnyfile: path =  " + path + ", filename = " + filename);
        if(!StringUtil.isStrNullOrEmpty(filename)){
            File dir = new File(path);
            dir.mkdirs();
            File file = new File(dir, filename);
            value = file.delete();
        }
        return value;
    }

    public static void DeletePhotofile(Context m_ctx, String photoname){
        HILog.d(TAG, "DeletePhotofile: photoname = " + photoname);
        String sdCard = Environment.getExternalStorageDirectory().getAbsolutePath();
        String path = sdCard + "/" + m_ctx.getResources().getString(R.string.app_name);
        HILog.d(TAG, "DeletePhotofile: path =  " + path + ", photoname = " + photoname);
        DeleteAnyfile(m_ctx, sdCard, photoname);
    }


    public static String getDateString(){
        String value = null;
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        value = sdf.format(date);
        return value;
    }

    public static void hideKeyboard(final Context context, boolean flag){
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if(flag){
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
        } else {
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
        }
    }

    public static String CopyAssest2SD(final Context context, int drawableid, String ext) {
        HILog.d(TAG, "CopyAssest2SD: drawableid = " + drawableid);
        String value = null;

        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();
            HILog.d(TAG, "CommonUtils: CopyAssest2SD: sd = " + sd);

            String backupDBPath = "/" + context.getResources().getString(R.string.app_name) + "/" + String.valueOf(drawableid) + ext;
            HILog.d(TAG, "CopyAssest2SD: backupDBPath = " + backupDBPath);
            File backupDB = new File(sd, backupDBPath);

            AssetFileDescriptor afd = context.getResources().openRawResourceFd(drawableid);
            FileInputStream fis = afd.createInputStream();
            FileChannel src = fis.getChannel();
            FileChannel dst = new FileOutputStream(backupDB).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();
            value = sd.toString() + backupDBPath;

        } catch (Exception e) {
            HILog.d(TAG, "CopyAssest2SD: Exception = " + e.toString());
        }
        return value;
    }

    public static boolean checkDataBase(Context context) {
        File dbFile = context.getDatabasePath(DBA.DATABASE_NAME);
        HILog.d(TAG, "checkDataBase: " + DBA.DATABASE_NAME + " = " + dbFile.exists());
        return dbFile.exists();
    }

    public static String CopyDBFromData2SD(final Context context) {
        HILog.d(TAG, "CopyDBFromData2SD: ");
        String value = null;
        if (!Constants.DEBUG) return value;

        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();
            HILog.d(TAG, "CommonUtils: CopyDBFromData2SD: sd = " + sd);

//	        if (sd.canWrite()) {
            String currentDBPath = "//data//" + context.getApplicationContext().getPackageName() + "//databases//" + DBA.DATABASE_NAME;
            HILog.d(TAG, "CopyDBFromData2SD: currentDBPath = " + currentDBPath);
//				LogUtils.println("CommonUtils: CopyDBFromData2SD: currentDBPath = " + currentDBPath);
            String backupDBPath = "/Download/" + DBA.DATABASE_NAME;
            HILog.d(TAG, "CopyDBFromData2SD: backupDBPath = " + backupDBPath);
//				LogUtils.println("CommonUtils: CopyDBFromData2SD: backupDBPath = " + backupDBPath);
            File currentDB = new File(data, currentDBPath);
            File backupDB = new File(sd, backupDBPath);

            FileChannel src = new FileInputStream(currentDB).getChannel();
            FileChannel dst = new FileOutputStream(backupDB).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();
            value = sd.toString() + backupDBPath;
//	        } else {
//				LogUtils.println("LocalControlSwipeActivity: CopyDBFromData2SD: !!sd.canWrite() NONONO!!!");
//	        }
        } catch (Exception e) {
            HILog.d(TAG, "CopyDBFromData2SD: Exception = " + e.toString());
        }
        return value;
    }

}
