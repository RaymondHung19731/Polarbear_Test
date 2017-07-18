package com.ultraflymodel.polarbear.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ConfigurationInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.text.format.Formatter;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.otto.Subscribe;
import com.ultraflymodel.polarbear.R;
import com.ultraflymodel.polarbear.common.Constants;
import com.ultraflymodel.polarbear.common.DBA;
import com.ultraflymodel.polarbear.common.HILog;
import com.ultraflymodel.polarbear.common.ISKey;
import com.ultraflymodel.polarbear.eventbus.EndMp3Event;
import com.ultraflymodel.polarbear.eventbus.InjuredEvent;
import com.ultraflymodel.polarbear.eventbus.PlayMp3Event;
import com.ultraflymodel.polarbear.eventbus.StartBCEvent;
import com.ultraflymodel.polarbear.eventbus.StopMp3Event;
import com.ultraflymodel.polarbear.eventbus.TimerEvent;
import com.ultraflymodel.polarbear.eventbus.UdpEvent;
import com.ultraflymodel.polarbear.eventbus.UdpScanListEvent;
import com.ultraflymodel.polarbear.eventbus.WifiUdpEvent;
import com.ultraflymodel.polarbear.eventbus.WinnerEvent;
import com.ultraflymodel.polarbear.fragment.PolarbearMainFragment;
import com.ultraflymodel.polarbear.mike.AudioPlayer;
import com.ultraflymodel.polarbear.mike.NetworkCallback;
import com.ultraflymodel.polarbear.mike.P2PNatProcess;
import com.ultraflymodel.polarbear.mike.UDPBCNetwork;
import com.ultraflymodel.polarbear.mike.UDPNetwork;
import com.ultraflymodel.polarbear.model.Settings;
import com.ultraflymodel.polarbear.ultraflymodel.UltraflyModelApplication;
import com.ultraflymodel.polarbear.utils.CommonUtils;
import com.ultraflymodel.polarbear.utils.InternalStore;
import com.ultraflymodel.polarbear.utils.StringUtil;

import net.ralphpina.permissionsmanager.PermissionsManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.ultraflymodel.polarbear.fragment.PolarbearMainFragment.IsApMode;
import static com.ultraflymodel.polarbear.fragment.PolarbearMainFragment.getCarType;

public class PolarbearMainActivity extends BaseActivity implements View.OnClickListener, View.OnLongClickListener, NetworkCallback, MediaPlayer.OnCompletionListener {
    private static final String TAG = PolarbearMainActivity.class.getSimpleName();
    int NowOrientation;
    int orientation;
    ConfigurationInfo configurationInfo;
    private static int saveOrientation = 0;
    public static boolean mNotInFront = false;
    private LinearLayout mLl_Portrait, mLl_Landscape;
    private boolean mBusRegister = false;
    public static UDPNetwork m_udpNetwork;
    public static UDPBCNetwork m_udpBC;
    public static AudioPlayer m_AudioPlayer;
    public static MaterialDialog md_diaog=null;
    public static Activity mActivity = null;

    public static Intent launchIntent;
    String m_strFilePlay;
//    private static String m_strFilePlay_mono="0001_8000Hz_mono.wav";
    private static String m_strFilePlay_mono=null;
    int m_sBufferLen = 0;
    byte[] m_byteBuffer = new byte[8190 + 16];

    Handler m_time_handler = new Handler();
    Runnable m_time_restore_orientation_task = new Runnable()
    {
        public void run()
        {
            HILog.d(TAG, "m_time_restore_orientation_task:");
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
    };

    public static MediaPlayer mediaPlayer = null;
    private static boolean mIsPlaying = false;
    public static WifiManager wm;
    public static boolean mVIDEORECORDING = false;
    public static String mVR_filename = null;
    public static String mVR_Play_filename = "/Download/test-h264.mp4";;
    public static FileOutputStream mVR_OS = null;
    public static File mVR_File = null;
    public static boolean mQueryHost = false;
    public static String myCopperIpInDB;
    int   m_IndexCount = 0;
    boolean   m_bIsHeader = true;
    int   PACKET_LENS =  8192;
    List<ByteArrayOutputStream> m_listBuffer = new ArrayList<ByteArrayOutputStream>();
    public static Object m_objectBuffer = new  Object();
    public static InternalStore mInsPolarbear;
    public static boolean mDeviceNamed = false;
    public static String myIPAddress = null;
    public static String mInjureSeqNo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        HILog.d(TAG, "onCreate:");
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        mActivity = PolarbearMainActivity.this;
        mInsPolarbear = new InternalStore(mActivity);
        mDeviceNamed = IsDeviceNamed();

        m_udpNetwork = new UDPNetwork(this);
        m_udpBC = new UDPBCNetwork(this);

        if(!mBusRegister){
            UltraflyModelApplication.getInstance().bus.register(this);
            mBusRegister = true;
        }
        if(!AskForStoragePermission()){
            HILog.d(TAG, "AskForStoragePermission : Denied!");
        }

        mNotInFront = false;
        NowOrientation = this.getResources().getConfiguration().orientation;
        saveOrientation = NowOrientation;
        setNowOrientation(NowOrientation);
        HILog.d(TAG, "onCreate: NowOrientation = " + NowOrientation);
        m_time_handler.postDelayed(m_time_restore_orientation_task,  50);

        wm = (WifiManager) getSystemService(WIFI_SERVICE);

//        Settings settings = new Settings();
//        settings.save();
        myIPAddress = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        HILog.d(TAG, "onCreate: myIPAddress = " + myIPAddress);

        myCopperIpInDB = getSettingStringValue(DBA.Field.MYCOPPERIP);
        HILog.d(TAG, "myCopperIpInDB  = " + myCopperIpInDB);

        if(!StringUtil.isStrNullOrEmpty(myCopperIpInDB)) {

            m_udpNetwork.mNetworkCallBack = this;
            m_udpNetwork.SetP2PAddress(myCopperIpInDB, Constants.CMDPORT);
            m_udpNetwork.StartReceviceServer();

           m_udpBC.mNetworkCallBack = this;
           m_udpBC.SetP2PAddress(StringUtil.getBCAddress(myCopperIpInDB), Constants.IBCPORT);
           m_udpBC.StartReceviceServer();

        } else {
            m_udpNetwork.mNetworkCallBack = this;
            m_udpNetwork.SetP2PAddress(Constants.Polarbear_IP, Constants.CMDPORT);
            m_udpNetwork.StartReceviceServer();

            m_udpBC.mNetworkCallBack = this;
            m_udpBC.SetP2PAddress("192.168.15.2", Constants.IBCPORT);
            m_udpBC.StartReceviceServer();

        }
        m_AudioPlayer  =  new AudioPlayer(this);
 //       finish();
    }

    public static boolean CheckIfSameClassC(String ipa, String ipb){
        HILog.d(TAG, "CheckIfSameClassC:");
        boolean value = false;
        String[] a, b;
        if(!StringUtil.isStrNullOrEmpty(ipa) && !StringUtil.isStrNullOrEmpty(ipb)){
            a = ipa.split("\\.");
            b = ipb.split("\\.");
            HILog.d(TAG, "CheckIfSameClassC: a.length" + a.length + ", [2] = " + a[2]);
            HILog.d(TAG, "CheckIfSameClassC: b.length" + b.length + ", [2] = " + b[2]);
            if(a[2].equals(b[2])) value = true;
        }
        HILog.d(TAG, "CheckIfSameClassC: value = " + value);
        return value;
    }

    public static String getDeviceName(){
        String value = "";
        if(IsDeviceNamed()){
            value = mInsPolarbear.getString(ISKey.DEVICE_NAME, "");
        }
        HILog.d(TAG, "getDeviceName: value = " + value);
        return value;
    }

    public static boolean IsDeviceNamed(){
        boolean value = false;
/*
        String devicename = mInsPolarbear.getString(ISKey.DEVICE_NAME, "");
        if(StringUtil.isStrNullOrEmpty(devicename)){
            HILog.d(TAG, "IsDeviceNamed: devicename is null!!!");
        } else {
            HILog.d(TAG, "IsDeviceNamed: devicename = " + devicename);
            value = true;
        }
*/
        return value;
    }

    public boolean AskForStoragePermission() {
        boolean value = false;
        if (PermissionsManager.get().isStorageGranted()) {
            value = true;
        } else if (PermissionsManager.get().neverAskForStorage(this)) {
            HILog.d(TAG, "user_selected_never_ask_again");

        } else {
            PermissionsManager.get().requestStoragePermission(this);
            HILog.d(TAG, "requestStoragePermission");
            value = true;
        }
        return value;
    }

    private void setNowOrientation(int orientation){
        HILog.d(TAG, "setNowOrientation: orientation = " + orientation);
        String popbackname = getPopBackName();
        HILog.d(TAG, "setNowOrientation : " + popbackname);

        if(saveOrientation!=orientation) {
            BaseActivity.fragmentManager.popBackStack(popbackname, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        PolarbearMainFragment polarbearMainFragment = new PolarbearMainFragment();
        loadFragment(polarbearMainFragment, Constants.JumpTo.PMAIN.toString(), true);
        BaseActivity.setPopBackName(Constants.JumpTo.PMAIN.toString());

        saveOrientation = orientation;

    }

    public void onConfigurationChanged(Configuration newConfig) {
        int newOrientation = this.getResources().getConfiguration().orientation;
        HILog.d(TAG, "onConfigurationChanged: newOrientation = " + newOrientation);
        HILog.d(TAG, "onConfigurationChanged: mNotInFront = " + mNotInFront);
        super.onConfigurationChanged(newConfig);
        if (!mNotInFront) {
            NowOrientation = newOrientation;
            //Send onConfigurationChanged event
            setNowOrientation(newOrientation);

        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            m_time_handler.postDelayed(m_time_restore_orientation_task,  1000);
        }
    }
    @Override
    protected void onPause(){
        HILog.d(TAG, "onPause:");
        mNotInFront = true;
//        finishAndRemoveTask();
        super.onPause();
//        onStop();
    }

    @Override
    protected void onResume() {
        HILog.d(TAG, "onResume:");
        super.onResume();
        m_udpNetwork.mNetworkCallBack = this;
    }

    @Override
    protected void onStop() {
        HILog.d(TAG, "onStop:");
        mNotInFront = true;
        super.onStop();
//       finishAndRemoveTask();
    }

    @Override
    protected void onDestroy() {
        HILog.d(TAG, "onDestroy:");
        super.onDestroy();
        m_udpNetwork.StopMP3();
        m_AudioPlayer.release();
        UltraflyModelApplication.getInstance().bus.unregister(this);
        mBusRegister = false;
        mNotInFront = true;
 //       finish();

    }


    @Override
    public boolean onLongClick(View v) {
        HILog.d(TAG, "onLongClick:");
        return false;
    }

    private Handler Mp3EventHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case Constants.STOPMP3:
                    HILog.d(TAG, "Mp3EventHandler: STOPMP3:");
                    m_udpNetwork.StopMP3();
                    break;
                case Constants.STARTMP3:
/*
                    PlayMp3Event mp3Event = (PlayMp3Event) msg.obj;
                    HILog.d(TAG, "Mp3EventHandler: STARTMP3: mp3path = " + mp3Event.mp3path);
                    m_strFilePlay = mp3Event.mp3path;
                    if(!StringUtil.isStrNullOrEmpty(m_strFilePlay_mono)){
                        String monofile = "android.resource://" + getPackageName() + "/"+ R.raw.mono_0001_8000;
                        HILog.d(TAG, "Mp3EventHandler : monofile = " + monofile);
                        Uri monouri = Uri.parse(monofile);
                        HILog.d(TAG, "Mp3EventHandler : monouri.getPath = " + monouri.getPath());
                        HILog.d(TAG, "Mp3EventHandler : m_strFilePlay = " + m_strFilePlay);
                        m_strFilePlay_mono = m_strFilePlay; // save it for next usage.
                        m_strFilePlay = monouri.getPath();
                        m_udpNetwork.StartMP3();
                    }  else {
                        HILog.d(TAG, "Mp3EventHandler: STARTMP3:");
                        m_udpNetwork.StartMP3();
                    }
*/
                    break;
                case P2PNatProcess.MP3_PLAY_END:
                    HILog.d(TAG, "Mp3EventHandler: MP3_PLAY_END:");
                    m_udpNetwork.StopMP3();
                    if(!StringUtil.isStrNullOrEmpty(m_strFilePlay_mono)){
                        HILog.d(TAG, "Mp3EventHandler: MP3_PLAY_END: m_strFilePlay_mono is not null: m_strFilePlay_mono = " + m_strFilePlay_mono);
                        m_strFilePlay = m_strFilePlay_mono;
                        m_strFilePlay_mono = null;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        HILog.d(TAG, "Mp3EventHandler: MP3_PLAY_END: StartMP3 again to the " + m_strFilePlay);
                        m_udpNetwork.StartMP3();
                    } else {
                        HILog.d(TAG, "Mp3EventHandler: MP3_PLAY_END:");
                        EndMp3Event endMp3Event = new EndMp3Event();
                        endMp3Event.mp3path = m_strFilePlay;
                        UltraflyModelApplication.getInstance().bus.post(endMp3Event);
                    }
                    break;
                case P2PNatProcess.MP3_FILE_INIT_SUCCESS:
                case P2PNatProcess.MP3_REQUEST_SECSSION:
                    int iCommand = msg.what;
                    int  iResponse = msg.arg1;
                    int iLen  = msg.arg2;
                    byte[] byResponse=null;
                    if(iLen>0)
                    {
                        ByteBuffer byBuf = (ByteBuffer) msg.obj;
                        byResponse = byBuf.array();
                    }
                    HILog.d(TAG, "Mp3EventHandler: iLen = " + iLen);
                    if(iCommand==P2PNatProcess.MP3_FILE_INIT_SUCCESS){
                        HILog.d(TAG, "Mp3EventHandler: MP3_FILE_INIT_SUCCESS:");
                        short sLen = GetFileSection();
                        HILog.d(TAG, "Mp3EventHandler: MP3_FILE_INIT_SUCCESS: sLen = " + sLen);
                        m_udpNetwork.StartMP3FileSection(sLen);
                    } else if (iCommand == P2PNatProcess.MP3_REQUEST_SECSSION) {
                        HILog.d(TAG, "Mp3EventHandler: MP3_REQUEST_SECSSION: Response=" + iResponse);
                        //Send MP3 Files......
                        byte[] byBuffer = ReadFileByte(iResponse);
                        if (byBuffer != null)
                        {
                            m_udpNetwork.SendMP3FileBuffer(byBuffer, (short) m_sBufferLen,(short)iResponse);
                        }
                    }
                    break;
            }
        }
    };
/*
    @Subscribe
    public void getOnBackPresssedEvent(OnBackPresssedEvent onBackPresssedEvent) {
        HILog.d(TAG, "Subscribe : getOnBackPresssedEvent: ");
        finish();
    }
*/
    @Subscribe
    public void getStopMp3Event(StopMp3Event stopMp3Event) {
        HILog.d(TAG, "Subscribe : getStopMp3Event: ");
        if(stopMp3Event.remote) {
            Message mp3Message=new Message();
            mp3Message.what = Constants.STOPMP3;
            mp3Message.obj = stopMp3Event;
            Mp3EventHandler.sendMessage(mp3Message);
        } else {
            stopMp3File();
        }
    }

    @Subscribe
    public void getPlayMp3Event(PlayMp3Event playMp3Event) {
        HILog.d(TAG, "Subscribe : getPlayMp3Event: ");
        if(playMp3Event.remote) {
            Message mp3Message=new Message();
            mp3Message.what = Constants.STARTMP3;
            mp3Message.obj = playMp3Event;
            Mp3EventHandler.sendMessage(mp3Message);
        } else {
            playbombfile();
//            PlayMp3File(playMp3Event.mp3path);
/*
            mediaPlayer = new MediaPlayer();
            mediaPlayer = MediaPlayer.create(this, playMp3Event.resId);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.start();
            mIsPlaying = true;
*/
        }
    }

    private Handler PolabearEventHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            HILog.d(TAG, "PolabearEventHandler:");
            switch (msg.what) {
                case Constants.POLARBEAR_EVENT_QUERYHOST:
                    HILog.d(TAG, "PolabearEventHandler: POLARBEAR_EVENT_QUERYHOST:");
                    final String myCopperIp = (String) msg.obj;
                    new Thread(new Runnable()
                    {
                        public void run()
                        {
                            m_udpNetwork.QueryHost(myCopperIp);
                        }
                    }).start();
                    break;

            }
        }
    };


    @Subscribe
    public void getStartBCEvent(StartBCEvent startBCEvent) {
        HILog.d(TAG, "Subscribe : getStartBCEvent: ");
        String ssid = wm.getConnectionInfo().getSSID();
        HILog.d(TAG, "getStartBCEvent: ssid = " + ssid);

        String devicename = getDeviceName();
        if(!StringUtil.isStrNullOrEmpty(devicename)){
            if(ssid.equals(devicename)){
                String myCopperIp = Constants.Polarbear_IP;
                setSettingStringValue(DBA.Field.MYCOPPERIP, myCopperIp);
                HILog.d(TAG, "getStartBCEvent: myCopperIp = " + myCopperIp);
                Message polarbearMessage=new Message();
                polarbearMessage.what = Constants.POLARBEAR_EVENT_QUERYHOST;
                polarbearMessage.obj = myCopperIp;
                PolabearEventHandler.sendMessage(polarbearMessage);
            }
        }
        if(getCarType(ssid)>=0){
            String myCopperIp = Constants.Polarbear_IP;
            setSettingStringValue(DBA.Field.MYCOPPERIP, myCopperIp);
            HILog.d(TAG, "getStartBCEvent: myCopperIp = " + myCopperIp);
            Message polarbearMessage=new Message();
            polarbearMessage.what = Constants.POLARBEAR_EVENT_QUERYHOST;
            polarbearMessage.obj = myCopperIp;
            PolabearEventHandler.sendMessage(polarbearMessage);
        } else onBroadCast();
    }

    short GetFileSection() {
        HILog.d(TAG, "GetFileSection: m_strFilePlay = " + m_strFilePlay);

        //File sdCard = Environment.getExternalStorageDirectory();
        //File queryImg = new File(sdCard.getAbsolutePath() + "/MP3/IntroCinematic.mp3");
        File queryImg;
        queryImg = new File(m_strFilePlay);
        int imageLen = (int) queryImg.length();
        short sLen = (short) ((imageLen + 1023) / 1024);
        HILog.d(TAG, "GetFileSection: sLen = " + sLen);
        return sLen;
    }

    byte[] ReadFileByte(int iSection) {
        //File sdCard = Environment.getExternalStorageDirectory();
        HILog.d(TAG, "ReadFileByte : iSection = " + iSection);
        File queryImg;

        if(StringUtil.isStrNullOrEmpty(m_strFilePlay)) return null;
        queryImg = new File(m_strFilePlay);
        int imageLen = (int) queryImg.length();
        FileInputStream fis;
        try {
            fis = new FileInputStream(queryImg);
            long iSize = imageLen;

            byte[] buffer = new byte[1024]; // Or whatever constant you feel like using
            try {
                fis.skip(iSection * 1024);
                int read = fis.read(buffer, 0, 1024);
                if (read > 0) {

                    m_sBufferLen = read;
                    fis.close();

                    return buffer;
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void success(int iCommand, int iResponse, byte[] byResponse, int iLen , String LocalPort) {
//      HILog.d(TAG, "success : iCommand = " + iCommand + ", iResponse = " + iResponse + ", iLen = " + iLen);

        switch(iCommand){
            case P2PNatProcess.BROADCAST:
                HILog.d(false, TAG, "success: BROADCAST received.");
                SendUDPEvent(iCommand, iResponse, byResponse, iLen , LocalPort);
                break;
            case P2PNatProcess.COUNTDOWN:
//                HILog.d(TAG, "success : P2PNatProcess.COUNTDOWN = " + iCommand + ", iResponse = " + iResponse + ", iLen = " + iLen);
                if(iLen==9){
                    int l = CommonUtils.unsignedToBytes(byResponse[7]);
                    int h = CommonUtils.unsignedToBytes(byResponse[8]);
                    int period = l + h*255;
                    HILog.d(TAG, "success : P2PNatProcess.COUNTDOWN period = " + period );
                    TimerEvent timerEvent = new TimerEvent();
                    timerEvent.period = period;
                    UltraflyModelApplication.getInstance().bus.post(timerEvent);
                }
                break;

            case P2PNatProcess.RECEIVE_VIDEO_DATA:
                byte[] byVideoDate = P2PNatProcess.get(byResponse, 4);


                if( PolarbearMainFragment.videoView!=null)
                {
//HILog.d(TAG, "success : P2PNatProcess.RECEIVE_VIDEO_DATA = " + iCommand  + ", iLen = " + iLen);

                    PolarbearMainFragment.videoView.ReceiveVideoData(byVideoDate, byVideoDate.length);
                }
                break;
            case P2PNatProcess.TANK_HIT: //strHit = d4vgnv;d4vgnv;d4vgnv33;��������; mInjureSeqNo
                HILog.d(TAG, "success : TANK_HIT");
                byte[] byHitString = Arrays.copyOfRange(byResponse, 4, iLen+4);
                String strHit = new String(byHitString);
                HILog.d(TAG, "success: TANK_HIT: strHit = " + strHit);
                if(strHit.contains("=")) break;
                String[] strHits = strHit.split(";");
                HILog.d(TAG, "success: TANK_HIT: strHits.length = " + strHits.length);
                if(strHits.length>=3){
                    HILog.d(TAG, "success: TANK_HIT: Injured Tank = " + strHits[0]);
                    HILog.d(TAG, "success: TANK_HIT: Winner Tank = " + strHits[1]);
                    if(strHits.length==4&&!StringUtil.isStrNullOrEmpty(strHits[2])) {
                        HILog.d(TAG, "success: TANK_HIT: mInjureSeqNo = " + strHits[2]);
                        if(StringUtil.isStrNullOrEmpty(mInjureSeqNo) && !StringUtil.isStrNullOrEmpty(strHits[2])) mInjureSeqNo = strHits[2];
                        else if(mInjureSeqNo.equals(strHits[2])) break;
                        else {
                            mInjureSeqNo = strHits[2];
                        }
                    }
                    int injured_tank = PolarbearMainFragment.IsThisTankInList(strHits[0]);
                    int winner_tank = PolarbearMainFragment.IsThisTankInList(strHits[1]);
                    if(Constants.NOSELSHOOTING&&(injured_tank==winner_tank)) break;
                    if(injured_tank>=0) {

//                        if(strHits[0].contains(Constants.TANK_LEADING)){
//                            String strInjuredIp = strHits[0].substring(5);
//                        int InjuredIp = Integer.decode("0x" + strInjuredIp);
                            HILog.d(TAG, "success: TANK_HIT: Injured Tank String= " + strHits[0]);
//                        HILog.d(TAG, "success: TANK_HIT: Injured Tank = " + InjuredIp);
                            InjuredEvent injuredEvent = new InjuredEvent();
                            injuredEvent.tankname = strHits[0].trim();
//                        injuredEvent.tankip = strInjuredIp.trim();
                            UltraflyModelApplication.getInstance().bus.post(injuredEvent);

//                        }

//                        if(strHits[1].contains(Constants.AMTANK_LEADING)){
//                            String strWinnerIp = strHits[1].trim().substring(8);
//                            int WinnerIp = Integer.decode("0x" + strWinnerIp);
//                            HILog.d(TAG, "success: TANK_HIT: Winner Tank String = " + strHits[1].trim());
//                            HILog.d(TAG, "success: TANK_HIT: Winner Tank = " + WinnerIp);
//                        } else if(strHits[1].contains(Constants.TANK_LEADING)){
//                            String strWinnerIp = strHits[1].trim().substring(5);
//                        int WinnerIp = Integer.decode("0x" + strWinnerIp);
                            HILog.d(TAG, "success: TANK_HIT: Winner Tank String = " + strHits[1].trim());
//                        HILog.d(TAG, "success: TANK_HIT: Winner Tank = " + WinnerIp);
                            WinnerEvent winnerEvent = new WinnerEvent();
                            winnerEvent.tankname = strHits[0].trim();
//                        winnerEvent.tankip = strWinnerIp.trim();
                            UltraflyModelApplication.getInstance().bus.post(winnerEvent);
//                        }
                    }

                }
//                SendUDPEvent(iCommand, iResponse, byResponse, iLen , LocalPort);
                break;
            case P2PNatProcess.WIFI_SSID_OK:
                HILog.d(TAG, "success : WIFI_SSID_OK");

//                m_udpNetwork.StopReceiveServer();
//                m_udpBC.StopReceiveServer();

/*
                WifiUdpEvent wifiUdpEvent = new WifiUdpEvent();
                wifiUdpEvent.mDialog = true;
                wifiUdpEvent.message = mActivity.getString(R.string.wifi_ssid_ok);
                UltraflyModelApplication.getInstance().bus.post(wifiUdpEvent);
*/

/*
                new Thread() {
                    public void run() {
                        Looper.prepare();
                        Toast.makeText(PolarbearMainActivity.this, "WIFI_SSID_OK", Toast.LENGTH_LONG).show();
                        Looper.loop();
                    };
                }.start();
*/
                break;
            case P2PNatProcess.WIFI_SSID_NG:
                HILog.d(TAG, "success : WIFI_SSID_NG");

                WifiUdpEvent wifiUdpEvent = new WifiUdpEvent();
                wifiUdpEvent.mDialog = true;
                wifiUdpEvent.message = mActivity.getString(R.string.wifi_ssid_ng);
                UltraflyModelApplication.getInstance().bus.post(wifiUdpEvent);

/*
                new Thread() {
                    public void run() {
                        Looper.prepare();
                        Toast.makeText(PolarbearMainActivity.this, "WIFI_SSID_NG", Toast.LENGTH_LONG).show();
                        Looper.loop();
                    };
                }.start();
*/
                break;
            case P2PNatProcess.WIFI_SSID_STARTSCAN:
                HILog.d(TAG, "success : WIFI_SSID_STARTSCAN");

//                wifiUdpEvent = new WifiUdpEvent();
//                wifiUdpEvent.message = "WIFI_SSID_STARTSCAN";
//                UltraflyModelApplication.getInstance().bus.post(wifiUdpEvent);
/*
                new Thread() {
                    public void run() {
                        Looper.prepare();
                        Toast.makeText(PolarbearMainActivity.this, "WIFI_SSID_STARTSCAN", Toast.LENGTH_LONG).show();
                        Looper.loop();
                    };
                }.start();
*/
                break;
            case P2PNatProcess.RECEIVE_AUDIO_DATA:
                HILog.d(TAG, "success : RECEIVE_AUDIO_DATA");
                byte[] byAudioDate = P2PNatProcess.get(byResponse, 4);
                boolean mEmpty = true;
                for(int i=0; i<iLen-4; i++){
                    if(byAudioDate[i]!=0){
                        mEmpty = false;
                        break;
                    }
                }
                if(!mEmpty) m_AudioPlayer.WriteAudioData(byAudioDate,iLen-4);
                else {
                    HILog.d(TAG, "success : RECEIVE_AUDIO_DATA: Empty");
                }
                return;
            case P2PNatProcess.MP3_FILE_INIT_SUCCESS:
            case P2PNatProcess.MP3_REQUEST_SECSSION:
            case P2PNatProcess.MP3_PLAY_END:
                switch(iCommand){
                    case P2PNatProcess.MP3_FILE_INIT_SUCCESS:
                        HILog.d(TAG, "success : iCommand = MP3_FILE_INIT_SUCCESS");
                        break;
                    case P2PNatProcess.MP3_REQUEST_SECSSION:
                        HILog.d(TAG, "success : iCommand = MP3_REQUEST_SECSSION");
                        break;
                    case P2PNatProcess.MP3_PLAY_END:
                        HILog.d(TAG, "success : iCommand = MP3_PLAY_END");
                        break;
                }
                Message message = Message.obtain();
                message.what = iCommand;
                message.arg1 = iResponse;
                message.arg2 = iLen;
                if(byResponse!=null) {
                    ByteBuffer byBuf = ByteBuffer.allocate(byResponse.length);
                    byBuf.put(byResponse);
                    message.obj = byBuf;
                }
                Mp3EventHandler.sendMessage(message);
                break;
            case P2PNatProcess.SCAN_LIST:
                HILog.d(TAG, "success : SCAN_LIST");
                UdpScanListEvent udpScanListEvent = new UdpScanListEvent();
                udpScanListEvent.iCommand = iCommand;
                udpScanListEvent.iResponse = iResponse;
                udpScanListEvent.iLen = iLen;
                if(byResponse!=null) {
                    ByteBuffer byBuf = ByteBuffer.allocate(byResponse.length);
                    byBuf.put(byResponse);
                    udpScanListEvent.byResponse = byBuf.array();
                }
                UltraflyModelApplication.getInstance().bus.post(udpScanListEvent);
                break;
            default:
                HILog.d(TAG, "success : default");
                SendUDPEvent(iCommand, iResponse, byResponse, iLen , LocalPort);
                break;
        }
    }

    public void SendUDPEvent(int iCommand, int iResponse, byte[] byResponse, int iLen , String LocalPort) {
        HILog.d(TAG, "SendUDPEvent :");
        UdpEvent udpEvent = new UdpEvent();
        udpEvent.iCommand = iCommand;
        udpEvent.iResponse = iResponse;
        udpEvent.iLen = iLen;
        udpEvent.LocalPort = LocalPort;
        if(byResponse!=null) {
            ByteBuffer byBuf = ByteBuffer.allocate(byResponse.length);
            byBuf.put(byResponse);
            udpEvent.byResponse = byBuf.array();
        }
        UltraflyModelApplication.getInstance().bus.post(udpEvent);
    }

    public static String getSettingStringValue(String column){
        HILog.d(TAG, "getSettingStringValue: column = " + column);
        String result = null;
        From from = new Select().from(Settings.class);
        boolean exists = from.exists();
        HILog.d(TAG, "getSettingStringValue: exists  = " + exists);
        if(!exists) return result;

        List<Settings> settingsList = from.execute();
        HILog.d(TAG, "getSettingStringValue: settingsList.size() = " + settingsList.size());
        HILog.d(TAG, "getSettingStringValue: settingsList.get(0).my_copper_ip = " + settingsList.get(0).my_copper_ip);
        switch(column){
            case DBA.Field.MYCOPPERIP:
                result = settingsList.get(0).my_copper_ip;
                break;
            case DBA.Field.MYCOPPERNAME:
                result = settingsList.get(0).my_copper_name;
                break;
            case DBA.Field.WIFINAME:
                result = settingsList.get(0).wifi_name;
                break;
            case DBA.Field.WIFIPASSWORD:
                result = settingsList.get(0).wifi_password;
                break;
        }
        HILog.d(TAG, "getSettingStringValue: result = " + result);
        return result;
    }

    public static void setSettingStringValue(String column, String value){
        HILog.d(TAG, "setSettingStringValue: column = " + column + ", value = " + value);
        From from = new Select().from(Settings.class);
        boolean exists = from.exists();
        HILog.d(TAG, "setSettingStringValue: exists  = " + exists);
        if(!exists) return;

        List<Settings> settingsList = from.execute();
        ActiveAndroid.beginTransaction();
        switch(column){
            case DBA.Field.MYCOPPERIP:
                settingsList.get(0).my_copper_ip = value;
                break;
            case DBA.Field.MYCOPPERNAME:
                settingsList.get(0).my_copper_name = value;
                break;
            case DBA.Field.WIFINAME:
                settingsList.get(0).wifi_name = value;
                break;
            case DBA.Field.WIFIPASSWORD:
                settingsList.get(0).wifi_password = value;
                break;
        }
        settingsList.get(0).save();
        ActiveAndroid.setTransactionSuccessful();
        ActiveAndroid.endTransaction();
        CommonUtils.CopyDBFromData2SD(mActivity);
    }

    public static int getSettingIntValue(String column){
        HILog.d(TAG, "getSettingIntValue: column = " + column);
        int result = 0;
        From from = new Select().from(Settings.class);
        boolean exists = from.exists();
        HILog.d(TAG, "getSettingIntValue: exists  = " + exists);
        if(!exists) return result;

        List<Settings> settingsList = from.execute();
        switch(column){
            case DBA.Field.AUDIOON:
                result = settingsList.get(0).audio_on;
                break;
            case DBA.Field.BROADCASTSET:
                result = settingsList.get(0).broadcast_set;
                break;
            case DBA.Field.MICON:
                result = settingsList.get(0).mic_on;
                break;
            case DBA.Field.PIRON:
                result = settingsList.get(0).pir_on;
                break;
            case DBA.Field.VIDEOON:
                result = settingsList.get(0).video_on;
                break;

        }
        HILog.d(TAG, "getSettingStringValue: result = " + result);
        return result;
    }

    public static void setSettingIntValue(String column, int value){
        HILog.d(TAG, "setSettingIntValue: column = " + column + ", value = " + value);
        From from = new Select().from(Settings.class);
        boolean exists = from.exists();
        HILog.d(TAG, "setSettingIntValue: exists  = " + exists);
        if(!exists) return;

        List<Settings> settingsList = from.execute();
        ActiveAndroid.beginTransaction();
        switch(column){
            case DBA.Field.AUDIOON:
                settingsList.get(0).audio_on = value;
                break;
            case DBA.Field.BROADCASTSET:
                settingsList.get(0).broadcast_set = value;
                break;
            case DBA.Field.MICON:
                settingsList.get(0).mic_on = value;
                break;
            case DBA.Field.PIRON:
                settingsList.get(0).pir_on = value;
                break;
            case DBA.Field.VIDEOON:
                settingsList.get(0).video_on = value;
                break;
        }
        settingsList.get(0).save();
        ActiveAndroid.setTransactionSuccessful();
        ActiveAndroid.endTransaction();
        CommonUtils.CopyDBFromData2SD(mActivity);
    }

    public static short GetFileSection(String strFilePlay) {
        HILog.d(TAG, "GetFileSection: strFilePlay = " + strFilePlay);
        //File sdCard = Environment.getExternalStorageDirectory();
        //File queryImg = new File(sdCard.getAbsolutePath() + "/MP3/IntroCinematic.mp3");

        File queryImg = new File(strFilePlay);

        int imageLen = (int) queryImg.length();
        short sLen = (short) ((imageLen + 1023) / 1024);
        return sLen;
    }

    public static void dialog_showmessage(String message){
        md_diaog = new MaterialDialog.Builder(mActivity)
                .title(R.string.app_name)
                .content(message)
                .show();
/*
        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        md_diaog.dismiss();
*/
    }

    public static void dialog_broadcast(){
        md_diaog = new MaterialDialog.Builder(mActivity)
                .title(R.string.app_name)
                .content(mActivity.getString(R.string.broadcasting))
                .progress(true, 0)
                .progressIndeterminateStyle(false)
                .show();
    }

    public static void dialog_broadcast_dismiss(){
        if(md_diaog!=null) md_diaog.dismiss();
    }


    public void onBroadCast() {
        HILog.d(TAG, "onBroadCast:");
        if(!IsApMode()) dialog_broadcast();
//        final String myCopperIpInDB = getSettingStringValue(DBA.Field.MYCOPPERIP);
//        HILog.d(TAG, "onBroadCast: myCopperIpInDB = " + myCopperIpInDB);
/*
        if(!StringUtil.isStrNullOrEmpty(myCopperIpInDB)) {
            HILog.d(TAG, "onBroadCast: myCopperIp = " + myCopperIpInDB);

            new Thread(new Runnable() {
                public void run() {
                    m_udpNetwork.QueryHost(myCopperIpInDB);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            new Thread() {
                public void run() {
                    Looper.prepare();
                    Toast.makeText(PolarbearMainActivity.this, R.string.found_ip, Toast.LENGTH_SHORT).show();
                    Looper.loop();
                };
            }.start();
        } else {
            new Thread() {
                public void run() {
                    Looper.prepare();
                    Toast.makeText(PolarbearMainActivity.this, R.string.not_found_ip, Toast.LENGTH_LONG).show();
                    Looper.loop();
                };
            }.start();
        }
*/
        final String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        HILog.d(TAG, "onBroadCast: ip = " + ip);
        mQueryHost = true;
        //m_udpNetwork.QueryHost("192.168.0.117");
        new Thread(new Runnable()
        {
            public void run()
            {
                String[]  strIPAddress = ip.split("\\.");
                if(IsApMode()) m_udpNetwork.QueryHost(Constants.Polarbear_IP);
                else
                for(int i=1;i<254;i++)
                {
                    if(!mQueryHost){
                        Thread.interrupted();
                        break;
                    }
                    String strHostIP = strIPAddress[0] + "." + strIPAddress[1] + "." + strIPAddress[2]+ "." + i;
                    if(!strHostIP.equals(ip))
                    {
                        m_udpNetwork.QueryHost(strHostIP);
                        HILog.d(TAG, "onBroadCast: QueryHost: " + strHostIP);

                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        HILog.d(TAG, "onBroadCast: Skipped QueryHost: " + strHostIP);
                    }
                }
            }
        }).start();

    }

    public void stopMp3File(){
        HILog.d(TAG, "stopMp3File:");
        if(mediaPlayer!=null && mediaPlayer.isPlaying() && mIsPlaying)
        {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            mIsPlaying = false;
        }
    }

    public void playbombfile(){
        mediaPlayer= MediaPlayer.create(this, R.raw.bomb);
        mediaPlayer.start();
    }

    public void PlayMp3File(String mp3filepath){
        HILog.d(TAG, "PlayMp3File: mp3filepath: " + mp3filepath);
        m_strFilePlay = mp3filepath;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setLooping(false);
        try
        {
            mediaPlayer.setDataSource(mp3filepath);
            mediaPlayer.prepareAsync();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        }
        catch (IllegalStateException e)
        {
            e.printStackTrace();
        }

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
        {
            @Override
            public void onPrepared(MediaPlayer mp)
            {
                HILog.d(TAG, "PlayMp3File: onPrepared: mp = " + mp.toString());
                mp.start();
            }
        });

        mIsPlaying = true;

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        HILog.d(TAG, "onCompletion:");
        EndMp3Event endMp3Event = new EndMp3Event();
        endMp3Event.mp3path = m_strFilePlay;
        endMp3Event.remote = false;
        UltraflyModelApplication.getInstance().bus.post(endMp3Event);
        mIsPlaying = false;
    }
}
