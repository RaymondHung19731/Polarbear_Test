package com.ultraflymodel.polarbear.common;

import android.util.Log;

/**
 * Created by rudy on 15/12/28.
 */
public class HILog {


    private static final boolean EnableLog = Constants.DEBUG;
    private static final boolean EnableSimpleLog = Constants.SIMPLEDEBUG;

    public static void d(boolean realtimetracking, String tag, String... msgs) {
        if(EnableSimpleLog||EnableLog) {
            if(realtimetracking){
                StringBuilder strBuilder = new StringBuilder();
                strBuilder.append("HILog:");
                for (String msg : msgs) {
                    strBuilder.append(msg);
                }
                Log.d(tag, strBuilder.toString());
            }
        }
    }

    public static void d(String tag, String... msgs) {
        if (true == EnableLog) {
            StringBuilder strBuilder = new StringBuilder();
            strBuilder.append("HILog:");
            for (String msg : msgs) {
                strBuilder.append(msg);
            }
            Log.d(tag, strBuilder.toString());
        }
    }

    public static void w(String tag, String... msgs) {
        w(tag, null, msgs);
    }

    public static void w(String tag, Exception e, String... msgs) {
        if (true == EnableLog) {
            StringBuilder strBuilder = new StringBuilder();
            for (String msg : msgs) {
                strBuilder.append(msg);
            }
            Log.w(tag, strBuilder.toString(), e);
        }
    }

    public static void e(String tag, String... msgs) {
        e(tag, null, msgs);
    }

    public static void e(String tag, Exception e, String... msgs) {
        if (true == EnableLog) {
            StringBuilder strBuilder = new StringBuilder();
            for (String msg : msgs) {
                strBuilder.append(msg);
            }
            Log.e(tag, strBuilder.toString(), e);
        }
    }

}
