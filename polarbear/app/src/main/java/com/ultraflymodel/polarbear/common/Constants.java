package com.ultraflymodel.polarbear.common;

/**
 * Created by William on 2016/7/28.
 */
public class Constants {

    public enum JumpTo {
        HOME, ABOUT, MUSICLIST, PMAIN, LMAIN, WIFISETUP, SETUP
    }
    public static final boolean DEBUG = true;
    public static final boolean SIMPLEDEBUG = true;
    public static final int WAKEUP = 1000;

    public final static int HIS_DAY					= 2;
    public final static int HIS_MONTH				= 4;
    public final static int HIS_YEAR					= 8;
    public final static int HIS_HOURS					= 24;
    public final static int HIS_DAYS					= 30;
    public final static int HIS_MONTHS				= 12;

    public static final String EMPTY_STRING = "";
    public static final String NULL_STRING = "NULL";
    public final static int SWIPPED	= 1;
    public final static int CLEARED	= 0;

    public final static int NONE = -1;
    public final static int MUSICLIST = 0;
    public final static int PLAYLIST = 1;
    public final static int WAKEUPLIST = 2;
    public final static int WIFILIST = 3;
    public final static int SWIPEWAIT = 500;

    public static final String BC_BUNDLE_SELECTED = "selected";
    public final static int ON	= 1;
    public final static int OFF	= 0;

    public final static int RINGONTIMEOUT	= 1000 *60 *1; //1 minutes
    public final static int TANKALIVETIMEOUT	= 1000 *40;
    public final static int STARTMP3 = -1;
    public final static int STOPMP3 = -2;

//    public final static String CHEAD = "COPPERHEAD";
//    public final static String CHEAD_IP = "192.168.15.1";
    public final static int RXBUFSIZE = 50;
    public final static boolean VIDEORECORDING = true;

    public final static String TANK_LEADING = "T-";
    public final static String AMTANK_LEADING = "AM:Tank-";
    public final static boolean DEMOMODE = false;
    public final static String OWNER_NONE = "NONE";
    public final static String OWNER_ME = "ME";
    public final static String ZERO = "0";
    public final static int CARTYPE_TANK = 0;
    public final static int CAR_MAX = 5;
    public final static String BCPORT = "8520";
    public final static int CMDPORT = 852;
    public final static int IBCPORT = Integer.valueOf(BCPORT);
    public final static String STATUS_AVAILABLE = "0";
    public final static String STATUS_OWNED = "1";
    public final static int QUERYCAR_TIMEOUT = 6000;
    public final static String Polarbear_IP = "192.168.15.1";
    public static final int INJUREDPERIOD = 1000;
    public static final int FIREPERIOD = 300;
    public final static boolean NOSELSHOOTING = true;

    public final static int UPDATEUI_SHOWTOYCARLIST = 1;
    public final static int UPDATEUI_SHOWONLYSETTING = 2;

    public final static int POLARBEAR_EVENT_QUERYHOST = 1;
    public final static int POLARBEAR_EVENT_RESETCARS = 2;
    public final static int FIRESHOT_WAIT_INTERVAL = 5000;
    public final static int FPS_WAIT_INTERVAL = 2000;
}
