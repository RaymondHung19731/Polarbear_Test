package com.ultraflymodel.polarbear.fragment;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VerticalSeekBar;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Cache;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.otto.Subscribe;
import com.ultraflymodel.polarbear.R;
import com.ultraflymodel.polarbear.activity.BaseActivity;
import com.ultraflymodel.polarbear.activity.PolarbearMainActivity;
import com.ultraflymodel.polarbear.common.Constants;
import com.ultraflymodel.polarbear.common.DBA;
import com.ultraflymodel.polarbear.common.HILog;
import com.ultraflymodel.polarbear.common.ISKey;
import com.ultraflymodel.polarbear.eventbus.EndMp3Event;
import com.ultraflymodel.polarbear.eventbus.InjuredEvent;
import com.ultraflymodel.polarbear.eventbus.PlayMp3Event;
import com.ultraflymodel.polarbear.eventbus.ResetCarDataEvent;
import com.ultraflymodel.polarbear.eventbus.RingOnEvent;
import com.ultraflymodel.polarbear.eventbus.StartBCEvent;
import com.ultraflymodel.polarbear.eventbus.StopMp3Event;
import com.ultraflymodel.polarbear.eventbus.TimeOutEvent;
import com.ultraflymodel.polarbear.eventbus.TimerEvent;
import com.ultraflymodel.polarbear.eventbus.UdpEvent;
import com.ultraflymodel.polarbear.eventbus.WinnerEvent;
import com.ultraflymodel.polarbear.mike.DEVICEINFO;
import com.ultraflymodel.polarbear.mike.P2PNatProcess;
import com.ultraflymodel.polarbear.mike.UDPBCNetwork;
import com.ultraflymodel.polarbear.mike.UDPNetwork;
import com.ultraflymodel.polarbear.mike.VideoDecoder;
import com.ultraflymodel.polarbear.model.MusicList;
import com.ultraflymodel.polarbear.model.Settings;
import com.ultraflymodel.polarbear.ultraflymodel.UltraflyModelApplication;
import com.ultraflymodel.polarbear.utils.CommonUtils;
import com.ultraflymodel.polarbear.utils.StringUtil;

import net.ralphpina.permissionsmanager.PermissionsManager;

import org.problemloeser.cta.mjpegplayer.MjpegInputStream;
import org.problemloeser.cta.mjpegplayer.MjpegView;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import gl.VideoTextureRenderer;

import static com.ultraflymodel.polarbear.activity.BaseActivity.mFlageVideo;
import static com.ultraflymodel.polarbear.activity.PolarbearMainActivity.IsDeviceNamed;
import static com.ultraflymodel.polarbear.activity.PolarbearMainActivity.mInsPolarbear;
import static com.ultraflymodel.polarbear.activity.PolarbearMainActivity.mQueryHost;
import static com.ultraflymodel.polarbear.activity.PolarbearMainActivity.mediaPlayer;
import static com.ultraflymodel.polarbear.activity.PolarbearMainActivity.wm;
import static com.ultraflymodel.polarbear.common.Constants.FIRESHOT_WAIT_INTERVAL;
import static com.ultraflymodel.polarbear.common.Constants.NOSELSHOOTING;


public class PolarbearMainFragment extends Fragment implements View.OnClickListener, View.OnLongClickListener,
        MediaPlayer.OnCompletionListener, View.OnTouchListener,
        SeekBar.OnSeekBarChangeListener,
        TextureView.SurfaceTextureListener
{
    private static final String TAG = PolarbearMainFragment.class.getSimpleName();
    private Button mBtnSetting, mBtnWakeup, mBtnVideo, mBtnMusic, mBtnSpeak, mBtnAbout;
    private UDPNetwork m_udpNetwork;
    private UDPBCNetwork m_udpBC;
    VideoDecoder  m_VideoDecoder = new VideoDecoder();
    private static String mCopperheadIp = null;
    List<DEVICEINFO> mDeviceList = new ArrayList<DEVICEINFO>();
    private FrameLayout fl_video_container;
    private static ImageView mIvWakeup;
    private static boolean mRingOn = false;
    private AnimationDrawable frameAnimation;
    private Cursor mWakeupListCursor=null;
    private static String mWakeupMp3Path = null;
    private static boolean mIsPlaying = false;
    boolean    m_bIsOpenAudio;
    private static int mPosition = 0;
    private static int mMaxPlaylist = 0;
    private static int mPlayToSendSize = 0;
    private boolean mAudioPermission = false;
    public static Activity mActivity;
    private static DEVICEINFO deviceinfo;
    private Thread m_RecieveThread;
    private DatagramSocket m_datagramSocket;
    private int m_TimerPeriod;
    private boolean mInBattle = false;
    private Handler mHandler;
    private int mAmIOwnTheCar = -1;
    private boolean mAliveTask = false;

    public Handler m_CountDownHandler = new Handler();
    private Runnable m_CountDownTimeoutTask = new Runnable() {
        public void run()
        {

            if(mInBattle){
                m_TimerPeriod -= 1;
                if(m_TimerPeriod>=0){
                    m_CountDownHandler.postDelayed(m_CountDownTimeoutTask, 1000);
                    ShowTimer(m_TimerPeriod);
                } else {
                    UltraflyModelApplication.getInstance().bus.post(new TimeOutEvent());
                }
            }


        }
    };

    public class m_TankAliveTimeoutTask extends TimerTask{
        public void run()
        {
            if(mAliveTask){
                HILog.d(true, TAG, "m_TankAliveTimeoutTask: Start of for loop.");
                for(int i=0; i<toycarGroupData.size(); i++){
                    String aliveornot = toycarGroupData.get(i).get(CAR_ALIVE);
                    String carip = toycarGroupData.get(i).get(CAR_IP);
                    HILog.d(true, TAG, "m_TankAliveTimeoutTask: carno = " + i + ",  aliveornot = " + aliveornot + ", ip = " + carip);
                    if(aliveornot.equals(String.valueOf(true))){
                        HILog.d(true, TAG, "m_TankAliveTimeoutTask: carno = " + i + " is alive!");
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        toycarGroupData.get(i).put(CAR_ALIVE, String.valueOf(false));
                        HILog.d(true, TAG, "m_TankAliveTimeoutTask: carno = " + i + " set to false.");

                    } else if(aliveornot.equals(String.valueOf(false))) {
                        HILog.d(true, TAG, "m_TankAliveTimeoutTask: carno = " + i + " aliveornot = " + aliveornot);
                        if(AmIOwnTheCar(i)){
                            HILog.d(TAG, "m_TankAliveTimeoutTask: I owned this car!");
                            if(mInBattle){
                                mInBattle = false;
                                resetscores();
                            }
                            mMotorCmdSending = false;
                            mAmIOwnTheCar = -1;
//                            ShowOnlySettting();
                            Message uiMessage=new Message();
                            uiMessage.what = Constants.UPDATEUI_SHOWONLYSETTING;
                            UpdateUIEventHandler.sendMessage(uiMessage);
                        }
                        toycarGroupData.remove(i);
                        HILog.d(true, TAG, "m_TankAliveTimeoutTask: carno = " + i + " is gone!");
//                        ShowToyCarList();
                        Message uiMessage=new Message();
                        uiMessage.what = Constants.UPDATEUI_SHOWTOYCARLIST;
                        UpdateUIEventHandler.sendMessage(uiMessage);
                    }
                    HILog.d(true, TAG, "m_TankAliveTimeoutTask: inner for loop i = " + i);
                }

                HILog.d(true, TAG, "m_TankAliveTimeoutTask: End of for loop.");
            }

        }
    }


    public Handler m_RingOnHandler = new Handler();
    private Runnable m_RingOnTimeoutTask = new Runnable() {
        public void run()
        {
            if( mIsPlaying){
                m_RingOnHandler.postDelayed(m_RingOnTimeoutTask, Constants.RINGONTIMEOUT);
            } else  doRingOff();
        }
    };

    public Handler m_QueryCarHandler = new Handler();
     private Runnable m_QueryCarTimeoutTask = new Runnable() {
        public void run()
        {
            for(int i=0; i<toycarGroupData.size();i++){
                String ipaddress = toycarGroupData.get(i).get(CAR_IP);
                HILog.d(TAG, "m_QueryCarTimeoutTask: ipaddress = " + ipaddress);
                QueryHostInThread(ipaddress);
            }

        }
    };

    private ImageButton ibCar_1, ibCar_2, ibCar_3, ibCar_4, ibCar_5;
    private ImageButton ib_hp_1, ib_hp_2, ib_hp_3, ib_hp_4, ib_hp_5, ib_hp_6, ib_hp_7, ib_hp_8, ib_hp_9;
    private static ImageButton CarPark[] = new ImageButton[Constants.CAR_MAX];
    private static Integer Injured[] = new Integer[Constants.CAR_MAX];
    private static Integer Winner[] = new Integer[Constants.CAR_MAX];
    private static Integer Scores[] = new Integer[Constants.CAR_MAX];
    private static ImageButton MyCarHP[] = new ImageButton[9];

    private ImageView iv_crosshair, iv_injured;
    private ImageButton ib_fireshot, ib_fps;
    private ImageButton ib_queryhost;
    private ImageButton ib_demomode, ib_fireshot_demo;
    private ImageButton ibRecorder;
    private ImageButton ibLife;
    private LinearLayout ll_hp;
    private TextView tv_hp, tv_winner;
    private LinearLayout ll_carlist;
    private ImageButton ibClock;
    private TextView tv_clock;
    private ImageButton ibSettings;
    private LinearLayout ll_hitpoint;
    private ImageButton ibHitpoint;
    private RelativeLayout rl_channel6;
    private LinearLayout ll_seekbars;
    private RelativeLayout rl_ch3, rl_ch4, rl_ch5;

    private ImageButton ibLeft;
    private ImageButton ibRight;

    private VerticalSeekBar seekBarLeft;
    private VerticalSeekBar seekBarRight;
    private VerticalSeekBar seekBar_ch4;
    private VerticalSeekBar seekBar_ch5;
    private VerticalSeekBar seekBar_ch3;

    private static boolean m_Demomode = Constants.DEMOMODE;



    int m_iWheel=1500,m_iWheel2=1500,m_iWheel3=1500, m_iWheel4=1500,m_iWheel5=1500,m_iWheel6=1500;
    Timer timer;
    protected static final int START_VIDEO = 0;



    public static MjpegView videoView = null;

    //View mainview;
    FileOutputStream m_OutputStream;
    private int surfaceWidth;
    private int surfaceHeight;
    private VideoTextureRenderer renderer;
    private TextureView m_surface;
    List<ByteArrayOutputStream> m_listBuffer = new ArrayList<ByteArrayOutputStream>();

    boolean    m_bStopThread       = false;
    boolean    m_bIsOpenVideo      = false;
    boolean    m_bCanMoveSeekBar   = false;

    private static final String CAR_NAME = "NAME";
    private final String CAR_IP = "IP";
    private final String CAR_HP = "HP";
    private final String CAR_HIT = "HIT";
    private final String CAR_OWNER = "OWNER";
    private final String CAR_STATUS = "STATUS";
    private final String CAR_TYPE = "TYPE";
    private final String CAR_ALIVE = "ALIVE";
    public static ArrayList<HashMap<String, String>> toycarGroupData = new ArrayList<HashMap<String, String>>();
    private static boolean mMotorCmdSending = false;

    private String MyTank_Name = null;
    private String MyTank_IP = null;

    Timer AliveTimer;
    private boolean mAliveChecking = false;

    private static long mLatestFireshotTime;
    private static boolean mQVGA = false;

    private static int mFps = 0;
    private static boolean mEnable_Fps = false;

/*
T-01”  代表的是 Tiger 車型
T-02” 代表的是 T34 車型
V-01 = Loader
V-02 = Bulldozer
V-03 = Truck
 */
    private static String[] CarType = {"T-01", "T-02", "V-01", "V-02", "V-03"};
    public static int getCarType(String carname){
        HILog.d(TAG, "getCarType: carname = " + carname);
        int value = -1;

        for(int i=0; i<CarType.length; i++){
            HILog.d(TAG, "getCarType: CarType[i] = " + CarType[i]);
            if(carname.contains(CarType[i])) {
                value = i;
                break;
            }
        }
        HILog.d(TAG, "getCarType: value = " + value);
        return value;
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View  mainview = inflater.inflate(R.layout.polarbear_main2, container, false);

        HILog.d(TAG, "onCreateView:");
        mHandler = new Handler();

        mActivity = getActivity();

        UltraflyModelApplication.getInstance().bus.register(this);

        m_udpNetwork = PolarbearMainActivity.m_udpNetwork;
        m_udpBC = PolarbearMainActivity.m_udpBC;



        iv_crosshair = (ImageView) mainview.findViewById(R.id.iv_crosshair);
        iv_injured = (ImageView) mainview.findViewById(R.id.iv_injured);
        ib_fireshot = (ImageButton) mainview.findViewById(R.id.ib_fireshot);

        ibCar_1 = (ImageButton) mainview.findViewById(R.id.ibCar_1);
        ibCar_1.setOnClickListener(this);
        CarPark[0] = ibCar_1;
        ibCar_2 = (ImageButton) mainview.findViewById(R.id.ibCar_2);
        ibCar_2.setOnClickListener(this);
        CarPark[1] = ibCar_2;
        ibCar_3 = (ImageButton) mainview.findViewById(R.id.ibCar_3);
        ibCar_3.setOnClickListener(this);
        CarPark[2] = ibCar_3;
        ibCar_4 = (ImageButton) mainview.findViewById(R.id.ibCar_4);
        ibCar_4.setOnClickListener(this);
        CarPark[3] = ibCar_4;
        ibCar_5 = (ImageButton) mainview.findViewById(R.id.ibCar_5);
        ibCar_5.setOnClickListener(this);
        CarPark[4] = ibCar_5;
        HILog.d(TAG, "onCreateView: CarPark.size() = " + CarPark.length);
        resetscores();

        ib_hp_1 = (ImageButton) mainview.findViewById(R.id.ib_hp_1);
        MyCarHP[0] = ib_hp_1;
        ib_hp_2 = (ImageButton) mainview.findViewById(R.id.ib_hp_2);
        MyCarHP[1] = ib_hp_2;
        ib_hp_3 = (ImageButton) mainview.findViewById(R.id.ib_hp_3);
        MyCarHP[2] = ib_hp_3;
        ib_hp_4 = (ImageButton) mainview.findViewById(R.id.ib_hp_4);
        MyCarHP[3] = ib_hp_4;
        ib_hp_5 = (ImageButton) mainview.findViewById(R.id.ib_hp_5);
        MyCarHP[4] = ib_hp_5;
        ib_hp_6 = (ImageButton) mainview.findViewById(R.id.ib_hp_6);
        MyCarHP[5] = ib_hp_6;
        ib_hp_7 = (ImageButton) mainview.findViewById(R.id.ib_hp_7);
        MyCarHP[6] = ib_hp_7;
        ib_hp_8 = (ImageButton) mainview.findViewById(R.id.ib_hp_8);
        MyCarHP[7] = ib_hp_8;
        ib_hp_9 = (ImageButton) mainview.findViewById(R.id.ib_hp_9);
        MyCarHP[8] = ib_hp_9;


        ib_fireshot.setOnClickListener(this);
        ib_demomode = (ImageButton) mainview.findViewById(R.id.ib_demomode);
        ib_demomode.setOnClickListener(this);
        ib_fireshot_demo = (ImageButton) mainview.findViewById(R.id.ib_fireshot_demo);
        ib_fireshot_demo.setOnClickListener(this);
        ib_queryhost = (ImageButton) mainview.findViewById(R.id.ib_queryhost);
        ib_queryhost.setOnClickListener(this);
        ibRecorder = (ImageButton) mainview.findViewById(R.id.ibRecorder);
        ibRecorder.setOnClickListener(this);
        ibLife = (ImageButton) mainview.findViewById(R.id.ibLife);
        ibLife.setOnClickListener(this);
        ll_hp = (LinearLayout) mainview.findViewById(R.id.ll_hp);
        tv_hp = (TextView) mainview.findViewById(R.id.tv_hp);
        tv_winner = (TextView) mainview.findViewById(R.id.tv_winner);
        ll_carlist = (LinearLayout) mainview.findViewById(R.id.ll_carlist);
        ibClock = (ImageButton) mainview.findViewById(R.id.ibClock);
        ibClock.setOnClickListener(this);
        tv_clock = (TextView) mainview.findViewById(R.id.tv_clock);
        ibSettings = (ImageButton) mainview.findViewById(R.id.ibSettings);
        ibSettings.setOnClickListener(this);

        ib_fps = (ImageButton) mainview.findViewById(R.id.ib_fps);
        ib_fps.setOnClickListener(this);


        ll_hitpoint = (LinearLayout) mainview.findViewById(R.id.ll_hitpoint);
        ibHitpoint = (ImageButton) mainview.findViewById(R.id.ibHitpoint);
        ibHitpoint.setOnClickListener(this);

        rl_channel6 = (RelativeLayout) mainview.findViewById(R.id.rl_channel6);
        ll_seekbars = (LinearLayout) mainview.findViewById(R.id.ll_seekbars);
        rl_ch3 = (RelativeLayout) mainview.findViewById(R.id.rl_ch3);
        rl_ch4 = (RelativeLayout) mainview.findViewById(R.id.rl_ch4);
        rl_ch5 = (RelativeLayout) mainview.findViewById(R.id.rl_ch5);

        ibLeft = (ImageButton) mainview.findViewById(R.id.ibLeft);
        ibLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) { //ToDo: channel_6
                if (event.getAction() == MotionEvent.ACTION_DOWN )
                {
                    HILog.d(TAG, "ibLeft: ACTION_DOWN:");
                    m_iWheel6 = 1000;
                    ibLeft.setBackgroundResource(R.mipmap.btn_left_h);
                    return true;
                }else if(event.getAction() == MotionEvent.ACTION_UP)
                {
                    HILog.d(TAG, "ibLeft: ACTION_UP:");
                    m_iWheel6 = 1500;
                    ibLeft.setBackgroundResource(R.mipmap.btn_left_n);
                    return true;
                }
                return false;
            }
        });

        ibRight = (ImageButton) mainview.findViewById(R.id.ibRight);
        ibRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) { //ToDo: channel_6
                if (event.getAction() == MotionEvent.ACTION_DOWN )
                {
                    HILog.d(TAG, "ibRight: ACTION_DOWN:");
                    m_iWheel6 = 2000;
                    ibRight.setBackgroundResource(R.mipmap.btn_right_h);
                    return true;
                }else if(event.getAction() == MotionEvent.ACTION_UP)
                {
                    HILog.d(TAG, "ibRight: ACTION_UP:");
                    m_iWheel6 = 1500;
                    ibRight.setBackgroundResource(R.mipmap.btn_right_n);
                    return true;
                }
                return false;
            }
        });


        seekBarLeft = (VerticalSeekBar) mainview.findViewById(R.id.seekBarLeft);
        seekBarRight = (VerticalSeekBar) mainview.findViewById(R.id.seekBarRight);
        seekBar_ch4 = (VerticalSeekBar) mainview.findViewById(R.id.seekBar_ch4);
        seekBar_ch5 = (VerticalSeekBar) mainview.findViewById(R.id.seekBar_ch5);
        seekBar_ch3 = (VerticalSeekBar) mainview.findViewById(R.id.seekBar_ch3);

//        initStopwatch();


        seekBarLeft.setMax(1000);
        seekBarLeft.setProgress(500);
        seekBarRight.setMax(1000);
        seekBarRight.setProgress(500);
        seekBar_ch4.setMax(1000);
        seekBar_ch4.setProgress(500);
        seekBar_ch5.setMax(1000);
        seekBar_ch5.setProgress(500);
        seekBar_ch3.setMax(1000);
        seekBar_ch3.setProgress(500);

        seekBarLeft.setOnSeekBarChangeListener(this);
        seekBarRight.setOnSeekBarChangeListener(this);
        seekBar_ch4.setOnSeekBarChangeListener(this);
        seekBar_ch5.setOnSeekBarChangeListener(this);
        seekBar_ch3.setOnSeekBarChangeListener(this);



        m_surface = (TextureView) mainview.findViewById(R.id.textureViewVideo);
        m_surface.setSurfaceTextureListener(this);


        mDeviceList.clear();
        int broadcast_set = PolarbearMainActivity.getSettingIntValue(DBA.Field.BROADCASTSET);

        deviceinfo = new DEVICEINFO();

        UltraflyModelApplication.getInstance().bus.post(new StartBCEvent());
        if(IsApMode()){
            ib_demomode.setVisibility(View.VISIBLE);
            ib_demomode.performClick();
        } else {
            ShowOnlySettting();
        }
        ib_demomode.setVisibility(View.INVISIBLE);
//        ShowAllControls();
        mLatestFireshotTime = System.currentTimeMillis() - FIRESHOT_WAIT_INTERVAL; //Allow fireshot at once
        return mainview;
	}

    private void ShowAllHealth(){
        for(int i=0; i<MyCarHP.length; i++){
            MyCarHP[i].setBackgroundResource(R.mipmap.ic_hp_n);
        }
    }

    private void ShowInjuryHP(int score){
        ShowAllHealth();
        switch(score){
            case 9:
                MyCarHP[0].setBackgroundResource(R.mipmap.ic_hp_d);
            case 8:
                MyCarHP[1].setBackgroundResource(R.mipmap.ic_hp_d);
            case 7:
                MyCarHP[2].setBackgroundResource(R.mipmap.ic_hp_d);
            case 6:
                MyCarHP[3].setBackgroundResource(R.mipmap.ic_hp_d);
            case 5:
                MyCarHP[4].setBackgroundResource(R.mipmap.ic_hp_d);
            case 4:
                MyCarHP[5].setBackgroundResource(R.mipmap.ic_hp_d);
            case 3:
                MyCarHP[6].setBackgroundResource(R.mipmap.ic_hp_d);
            case 2:
                MyCarHP[7].setBackgroundResource(R.mipmap.ic_hp_d);
            case 1:
                MyCarHP[8].setBackgroundResource(R.mipmap.ic_hp_d);
                break;
        }

    }

    private boolean AmIWinner(){
        boolean value = false;
        HILog.d(TAG, "AmIWinner:");
        int tankno = IsThisTankInList(MyTank_Name);
        calculatescores();
        int myscore = Scores[tankno];
        for(int i=0; i<Constants.CAR_MAX; i++){
            if(i!=tankno){
                if(myscore<Scores[i]) {
                    value = false;
                    break;
                }
            } else {
                value = true;
            }
        }
        return value;
    }

    private void ShowMyScore(){
        int tankno = IsThisTankInList(MyTank_Name);
        HILog.d(TAG, "ShowMyScore: tankno = " + tankno + "; score = " + Injured[tankno]);
        if(tankno>=0) {
            if(Injured[tankno]>9){
                tv_hp.setVisibility(View.VISIBLE);
                tv_hp.setText(String.valueOf(Injured[tankno]));
            } else {
                tv_hp.setVisibility(View.INVISIBLE);
                ShowInjuryHP(Injured[tankno]);
            }
            tv_winner.setText(String.valueOf(Winner[tankno]));
        }
    }

    private void resetscores(){
        for(int i=0; i<Constants.CAR_MAX; i++){
            Injured[i] = 0;
            Winner[i] = 0;
            Scores[i] = 0;
        }
    }

    private void calculatescores(){
        for(int i=0; i<Constants.CAR_MAX; i++){
            Scores[i] = Winner[i] - Injured[i];
        }
    }

    private void ShowFireArms(boolean firemode){
        if(firemode){
            iv_crosshair.setVisibility(View.VISIBLE);
            ib_fireshot.setVisibility(View.VISIBLE);
//            videoView.setVisibility(View.VISIBLE);
        } else {
            iv_crosshair.setVisibility(View.INVISIBLE);
            ib_fireshot.setVisibility(View.VISIBLE);
//            videoView.setVisibility(View.GONE);
        }

    }

    private void ShowAllTankControls(){
        iv_crosshair.setVisibility(View.INVISIBLE);
        ib_fireshot.setVisibility(View.VISIBLE);
        ib_fireshot_demo.setVisibility(View.INVISIBLE);
        ibRecorder.setVisibility(View.VISIBLE); //ib_queryhost
        ib_queryhost.setVisibility(View.VISIBLE); //ib_queryhost
        ibLife.setVisibility(View.VISIBLE);
        ll_hp.setVisibility(View.VISIBLE);
        tv_hp.setVisibility(View.INVISIBLE);
        ll_carlist.setVisibility(View.VISIBLE);
        ibClock.setVisibility(View.VISIBLE);
        tv_clock.setVisibility(View.VISIBLE);
        ibSettings.setVisibility(View.VISIBLE);
        ll_hitpoint.setVisibility(View.VISIBLE);
        rl_channel6.setVisibility(View.VISIBLE);
        ll_seekbars.setVisibility(View.VISIBLE);
        seekBarLeft.setVisibility(View.VISIBLE);
        seekBarRight.setVisibility(View.VISIBLE);
        rl_ch3.setVisibility(View.INVISIBLE);
        rl_ch4.setVisibility(View.INVISIBLE);
        rl_ch5.setVisibility(View.INVISIBLE);
    }
/*
T-01”  代表的是 Tiger 車型
T-02” 代表的是 T34 車型
V-01 = Loader
V-02 = Bulldozer
V-03 = Truck
 */
    private void ShowCarControls(String cartype){
        int whichcar = getCarType(cartype);
        switch (whichcar){
            case 0:
            case 1:
                iv_crosshair.setVisibility(View.VISIBLE);
                ib_fireshot.setVisibility(View.VISIBLE);
                ib_fireshot_demo.setVisibility(View.INVISIBLE);
                ibRecorder.setVisibility(View.VISIBLE);
                ib_queryhost.setVisibility(View.VISIBLE);
                ibLife.setVisibility(View.VISIBLE);
                ll_hp.setVisibility(View.VISIBLE);
                tv_hp.setVisibility(View.VISIBLE);
                ll_carlist.setVisibility(View.VISIBLE);
                ibClock.setVisibility(View.VISIBLE);
                tv_clock.setVisibility(View.VISIBLE);
                ibSettings.setVisibility(View.VISIBLE);
                ll_hitpoint.setVisibility(View.VISIBLE);
                rl_channel6.setVisibility(View.VISIBLE);
                ll_seekbars.setVisibility(View.VISIBLE);
                rl_ch3.setVisibility(View.INVISIBLE);
                rl_ch4.setVisibility(View.INVISIBLE);
                rl_ch5.setVisibility(View.INVISIBLE);
                break;
            case 2:
            case 3:
            case 4:
                iv_crosshair.setVisibility(View.INVISIBLE);
                ib_fireshot.setVisibility(View.INVISIBLE);
                ib_fireshot_demo.setVisibility(View.INVISIBLE);
                ibRecorder.setVisibility(View.VISIBLE);
                ib_queryhost.setVisibility(View.VISIBLE);
                ibLife.setVisibility(View.INVISIBLE);
                ll_hp.setVisibility(View.INVISIBLE);
                tv_hp.setVisibility(View.INVISIBLE);
                ll_carlist.setVisibility(View.VISIBLE);
                ibClock.setVisibility(View.VISIBLE);
                tv_clock.setVisibility(View.VISIBLE);
                ibSettings.setVisibility(View.VISIBLE);
                ll_hitpoint.setVisibility(View.INVISIBLE);
                rl_channel6.setVisibility(View.VISIBLE);
                ll_seekbars.setVisibility(View.VISIBLE);
                break;
        }

        switch (whichcar){
            case 2:
                rl_ch3.setVisibility(View.VISIBLE);
                rl_ch4.setVisibility(View.VISIBLE);
                rl_ch5.setVisibility(View.INVISIBLE);
                break;
            case 3:
            case 4:
                rl_ch3.setVisibility(View.INVISIBLE);
                rl_ch4.setVisibility(View.VISIBLE);
                rl_ch5.setVisibility(View.INVISIBLE);
                break;
        }

    }

    private void ShowAllControls(){
        iv_crosshair.setVisibility(View.VISIBLE);
        ib_fireshot.setVisibility(View.VISIBLE);
        ib_fireshot_demo.setVisibility(View.INVISIBLE);
        ibRecorder.setVisibility(View.VISIBLE);
        ib_queryhost.setVisibility(View.VISIBLE);
        ibLife.setVisibility(View.VISIBLE);
        ll_hp.setVisibility(View.VISIBLE);
        tv_hp.setVisibility(View.VISIBLE);
        ll_carlist.setVisibility(View.VISIBLE);
        ibClock.setVisibility(View.VISIBLE);
        tv_clock.setVisibility(View.VISIBLE);
        ibSettings.setVisibility(View.VISIBLE);
        ll_hitpoint.setVisibility(View.VISIBLE);
        rl_channel6.setVisibility(View.VISIBLE);
        ll_seekbars.setVisibility(View.VISIBLE);
        rl_ch3.setVisibility(View.INVISIBLE);
        rl_ch4.setVisibility(View.INVISIBLE);
        rl_ch5.setVisibility(View.INVISIBLE);
    }

    private void ShowOnlySettting(){
        HILog.d(TAG, "OnlySetupLeft:");
        iv_crosshair.setVisibility(View.INVISIBLE);
        ib_fireshot.setVisibility(View.INVISIBLE);
        ibRecorder.setVisibility(View.INVISIBLE);
        ib_queryhost.setVisibility(View.INVISIBLE);
        ibLife.setVisibility(View.INVISIBLE);
        ll_hp.setVisibility(View.INVISIBLE);
        tv_hp.setVisibility(View.INVISIBLE);
        ll_carlist.setVisibility(View.INVISIBLE);
        ibClock.setVisibility(View.INVISIBLE);
        tv_clock.setVisibility(View.INVISIBLE);
        ibSettings.setVisibility(View.VISIBLE);
        ll_hitpoint.setVisibility(View.INVISIBLE);
        rl_channel6.setVisibility(View.INVISIBLE);
        ll_seekbars.setVisibility(View.INVISIBLE);
    }

    public class timerTask extends TimerTask
    {
        public void run()
        {
//            HILog.d(TAG, "timerTask: mMotorCmdSending = " + mMotorCmdSending);
            if(mMotorCmdSending) {
                m_udpNetwork.SendControlMotorCommand(m_iWheel,m_iWheel2,m_iWheel3,m_iWheel4,m_iWheel5,m_iWheel6);
            } else {

            }

        }
    };

    @Override
    public void onPause() {
        super.onPause();
        HILog.d(TAG, "onPause:");
        if(videoView!=null)
        if (videoView.isPlaying())
            ibRecorder.performClick();

        mMotorCmdSending = false;
        if (renderer != null)
            renderer.onPause();
    }

    @Override
    public void onResume() {
        HILog.d(TAG, "onResume:");
        super.onResume();
        if(mAmIOwnTheCar>=0) mMotorCmdSending = true;
//        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        System.gc();
    }

    @Override
    public void onDestroyView() {
        HILog.d(TAG, "onDestroyView:");
        PolarbearMainActivity.mNotInFront = false;
        mEnable_Fps = false;
        mFps = 0;
        mAliveTask = false;
        if (videoView!=null && videoView.isPlaying())
        {
//            m_udpNetwork.SendVideoOff();
            videoView.stopPlayback();
            videoView.setVisibility(View.GONE);
            videoView = null;
        }

        m_udpNetwork.StopReceiveServer();
        m_udpBC.StopReceiveServer();

        UltraflyModelApplication.getInstance().bus.unregister(this);

        m_Demomode = Constants.DEMOMODE;

        super.onDestroyView();
        System.gc();
    }

    @Subscribe
    public void getRingOnEvent(RingOnEvent ringOnEvent) {
        HILog.d(TAG, "Subscribe : getRingOnEvent: ");
        doRingOn();
    }

    private void setMusicOff(){
        HILog.d(TAG, "setMusicOff:");
        BaseActivity.mFlagMusic = false;
        PolarbearMainActivity.setSettingIntValue(DBA.Field.MUSICON, 0);
        StopMp3Event stopMp3Event = new StopMp3Event();
        stopMp3Event.mp3path = null;
        stopMp3Event.remote = true;
        UltraflyModelApplication.getInstance().bus.post(stopMp3Event);
    }

    private void ClearAllCarOwner(){
        HILog.d(TAG, "ClearAllCarOwner:");
        for(int i=0; i<toycarGroupData.size(); i++){
            toycarGroupData.get(i).put(CAR_OWNER, Constants.OWNER_NONE);
        }
    }

    private void QueryHostInThread(final String ipaddress){
        new Thread(new Runnable()
        {
            public void run()
            {
                m_udpNetwork.QueryHost(ipaddress);
            }
        }).start();
    }

    public static String getSSID(){
        String ssid = wm.getConnectionInfo().getSSID();
        return ssid;
    }

    public static boolean IsApMode(){
        boolean value = false;

        String ssid = getSSID();
        HILog.d(TAG, "IsApMode: ssid = " + ssid);

        if(getCarType(ssid)>=0){
            value = true;
        }
/*
        String db_copperhead_ip = PolarbearMainActivity.getSettingStringValue(DBA.Field.MYCOPPERIP);
        if(db_copperhead_ip.equals(Constants.Polarbear_IP)){
            value = true;
        }
*/
        HILog.d(TAG, "IsApMode:" + value);
        return value;
    }

    private void AutoSelectMyCar(){
        String myCarName = mInsPolarbear.getString(ISKey.DEVICE_NAME, "");
        HILog.d(TAG, "AutoSelectMyCar: myCarName = " + myCarName);
        if(!StringUtil.isStrNullOrEmpty(myCarName)){
            int myCarNo = IsThisTankInList(myCarName);
            HILog.d(TAG, "AutoSelectMyCar: found my car!");
            if(myCarNo>=0&&IsThisCarAvailable(myCarNo)){
                switch(myCarNo){
                    case 0:
                        ibCar_1.performClick();
                        break;
                    case 1:
                        ibCar_2.performClick();
                        break;
                    case 2:
                        ibCar_3.performClick();
                        break;
                    case 3:
                        ibCar_4.performClick();
                        break;
                    case 4:
                        ibCar_5.performClick();
                        break;
                }
            }
        }

    }

    private String getRandomName(){
        String value = StringUtil.getRandomString(6);
        HILog.d(TAG, "getRandomName: value = " + value);
        return value;
    }

    private void GetSetCarName(int carno){
        String newcarname = getRandomName();
        toycarGroupData.get(carno).get(CAR_NAME);
        for(int j=0;j<10;j++)
        {
            m_udpNetwork.SendChangeCarName(newcarname);
            for (int m=0;m<100;m++)
            {;}
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//         m_udpNetwork.SendChangeCarName(newcarname);
        for(int j=0;j<10;j++)
        {
            m_udpNetwork.SendChangeCarName(newcarname);
            for (int m=0;m<100;m++)
            {;}
        }
        for(int j=0;j<1;j++) {

            toycarGroupData.get(carno).put(CAR_NAME, newcarname);
            HILog.d(TAG, "GetSetCarName: Set DEVICE_NAME = " + newcarname);
            mInsPolarbear.putString(ISKey.DEVICE_NAME, newcarname);
            mInsPolarbear.save();
        }
    }

    public void playcannonfile(){
        mediaPlayer= MediaPlayer.create(mActivity, R.raw.cannon);
        mediaPlayer.start();
    }

    @Override
    public void onClick(View v) {
        HILog.d(TAG, "onClick:");
        System.gc();
        PolarbearMainActivity.mNotInFront = true;
        Bundle bundle = new Bundle();
        String carname=null;
        boolean mFlageVideo_saved, mQVGA_save;
        int carno=-1;
//        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        switch (v.getId()) {
            case R.id.ib_fps:
                HILog.d(TAG, "onClick: ib_fps:");
                if(videoView==null) break;
                mFps++;
                {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            HILog.d(TAG, "onClick: ib_fps: run: mFps = " + mFps);
                            if(mFps==3){
                                mEnable_Fps = !mEnable_Fps;
                                if(videoView!=null)
                                videoView.showFps(mEnable_Fps);
                            }
                            mFps = 0;
                        } //run
                    }, Constants.FPS_WAIT_INTERVAL);
                }
                if(mFps>=3) DoVibrate(Constants.FIREPERIOD);
                break;
            case R.id.ibClock:
                if(!mInBattle) {
                    HILog.d(TAG, "onClick: ib_fps:");
                    dialog_Clock();
                }
                break;
            case R.id.ib_demomode:
                if(!IsApMode()){
                    break;
                }
                m_Demomode = !m_Demomode;
                HILog.d(TAG, "onClick: ib_demomode: m_Demomode = " + m_Demomode);

                if(m_Demomode) {
                    m_udpNetwork.SetP2PAddress(Constants.Polarbear_IP, Constants.CMDPORT);
                    m_udpNetwork.StartReceviceServer();
                    String ssid = getSSID();
                    int id = getCarType(ssid);
                    HILog.d(TAG, "onClick: ib_demomode: id = " + id);
                    ShowCarControls(ssid);
                    if(timer!=null) {
                        timer.cancel();
                        timer.purge();
                    }
                    timer = new Timer(true);
                    timer.schedule(new timerTask(), 1000, 20);
                    mMotorCmdSending = true;
                    HashMap<String, String> newToycarData = new HashMap<String, String>();
                    newToycarData.put(CAR_NAME, ssid);
                    newToycarData.put(CAR_IP, Constants.Polarbear_IP);
                    newToycarData.put(CAR_TYPE, ssid);
                    newToycarData.put(CAR_STATUS, "0");
                    newToycarData.put(CAR_HP, Constants.ZERO);
                    newToycarData.put(CAR_HIT, Constants.ZERO);
                    newToycarData.put(CAR_OWNER, Constants.OWNER_NONE);
                    newToycarData.put(CAR_ALIVE, String.valueOf(true));
                    toycarGroupData.add(newToycarData);
                    if(id>=0){ //Reset  DEVICE_NAME
                        HILog.d(TAG, "UdpEventHandler: BROADCAST: Reset  DEVICE_NAME");
                        mInsPolarbear.putString(ISKey.DEVICE_NAME, "");
                        mInsPolarbear.save();
                        ShowOneCar(0, ibCar_1);
                    }

                } else {
                    m_udpNetwork.SetP2PAddress(PolarbearMainActivity.myCopperIpInDB, Constants.CMDPORT);
                    m_udpNetwork.StartReceviceServer();
                    ShowOnlySettting();
                }
                break;
            case R.id.ibCar_1:
            case R.id.ibCar_2:
            case R.id.ibCar_3:
            case R.id.ibCar_4:
            case R.id.ibCar_5:
                if(toycarGroupData.size()==0) break;
                switch(v.getId()){
                    case R.id.ibCar_1:
                        carno = 0;
                        break;
                    case R.id.ibCar_2:
                        carno = 1;
                        break;
                    case R.id.ibCar_3:
                        carno = 2;
                        break;
                    case R.id.ibCar_4:
                        carno = 3;
                        break;
                    case R.id.ibCar_5:
                        carno = 4;
                        break;
                }
                HILog.d(TAG, "onClick: Car # = " + carno);
                mQueryHost = false;
                String status = toycarGroupData.get(carno).get(CAR_STATUS);
                if(status.equals(Constants.STATUS_OWNED)) break;
                mMotorCmdSending = false;
                m_udpNetwork.ResetDevice();
                mFlageVideo_saved = mFlageVideo;
                mQVGA_save = mQVGA;
                if(mFlageVideo) {
                    mFlageVideo = false;
                    videoView.stopPlayback();
                    videoView.setVisibility(View.GONE);
                    videoView = null;
                }
                String ipaddress = toycarGroupData.get(carno).get(CAR_IP);
                m_udpNetwork.SetP2PAddress(ipaddress, Constants.CMDPORT);
                m_udpNetwork.StartReceviceServer();

                MyTank_IP = ipaddress;
                if(!StringUtil.isStrNullOrEmpty(MyTank_Name)){
                    m_udpNetwork.StopReceiveServer();
                    m_udpBC.StopReceiveServer();
                    m_udpNetwork.SetP2PAddress(ipaddress, Constants.CMDPORT);
                    m_udpNetwork.StartReceviceServer();
                    m_udpBC.SetP2PAddress(ipaddress, Constants.IBCPORT);
                    m_udpBC.StartReceviceServer();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if(timer!=null) {
                    timer.cancel();
                    timer.purge();
                }

                timer = new Timer(true);
                timer.schedule(new timerTask(), 1000, 20);

                HILog.d(TAG, "onClick: timer started.");

                mMotorCmdSending = true;


// remark for change name

                int m=0;
                if(!IsDeviceNamed()){
                   GetSetCarName(carno);
//                    for(m=0;m<1000;m++)
//                    {;}
                }

                ClearAllCarOwner();
//                for(m=0;m<1000;m++)
//                {;}

                toycarGroupData.get(carno).put(CAR_OWNER, Constants.OWNER_ME);
                carname = toycarGroupData.get(carno).get(CAR_NAME);
                mInsPolarbear.putString(ISKey.DEVICE_NAME, carname);
                mInsPolarbear.save();
                MyTank_Name = carname;
                ShowMyScore();
                SaveCarInfoIntoDB(ipaddress, carname);
//                m_udpNetwork.SendVideoOff();
                ShowToyCarList();
                String myCarName = mInsPolarbear.getString(ISKey.DEVICE_NAME, "");
                String myCarType = toycarGroupData.get(carno).get(CAR_TYPE);
//                if(StringUtil.isStrNullOrEmpty(myCarName)) myCarName = Constants.NULL_STRING;
//                if(carname.contains(Constants.TANK_LEADING) || carname.equals(myCarName)){
//                    ShowAllTankControls();
//                }
                ShowCarControls(myCarType);
                if(timer!=null) {
                    timer.cancel();
                    timer.purge();
                }

                timer = new Timer(true);
                timer.schedule(new timerTask(), 1000, 20);
                HILog.d(TAG, "onClick: timer started.");
                for(m=0;m<1000;m++)
                {;}

                mMotorCmdSending = true;
                for(m=0;m<1000;m++)
                {;}

                mAmIOwnTheCar = carno;
                QueryHostInThread(ipaddress);
//                m_QueryCarHandler.postDelayed(m_QueryCarTimeoutTask, Constants.QUERYCAR_TIMEOUT);
                if(mFlageVideo_saved) {
//                    SleepDelayTime(1000);
                    if(mQVGA_save) ib_queryhost.performClick();
                    else ibRecorder.performClick();
                    if(mQVGA_save) m_udpNetwork.SendQVGAVideoOn();
                    else m_udpNetwork.SendVideoOn();
                }
                break;
            case R.id.ibHitpoint:
                HILog.d(TAG, "onClick: ibHitpoint:");
                m_udpNetwork.SendAlive();
                break;
            case R.id.ib_fireshot_demo:
            case R.id.ib_fireshot:
                HILog.d(TAG, "onClick: R.id.ib_fireshot:");
                if(System.currentTimeMillis()-mLatestFireshotTime > FIRESHOT_WAIT_INTERVAL){
                    mLatestFireshotTime = System.currentTimeMillis();
                    m_udpNetwork.FireOneShot();
                    playcannonfile();
                    DoVibrate(Constants.FIREPERIOD);
                    ib_fireshot.setBackgroundResource(R.mipmap.btn_fire_hk);
                    ib_fireshot_demo.setBackgroundResource(R.mipmap.btn_fire_hk);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ib_fireshot.setBackgroundResource(R.drawable.selector_btn_fireshot);
                            ib_fireshot_demo.setBackgroundResource(R.drawable.selector_btn_fireshot);
                        } //run
                    }, FIRESHOT_WAIT_INTERVAL);
                }
                break;
            case R.id.ibSettings:
                HILog.d(TAG, "onClick: R.id.ibSettings:");
/*
                SetupFragment setupFragment = new SetupFragment();
                setupFragment.setArguments(bundle);
                BaseActivity.loadFragment(setupFragment, Constants.JumpTo.PMAIN.toString(), false);
                BaseActivity.setPopBackName(Constants.JumpTo.PMAIN.toString());
*/
                ScanWifiListFragment scanWifiListFragment = new ScanWifiListFragment();
                scanWifiListFragment.setArguments(bundle);
                BaseActivity.loadFragment(scanWifiListFragment, Constants.JumpTo.WIFISETUP.toString(), true);
                BaseActivity.setPopBackName(Constants.JumpTo.WIFISETUP.toString());

                break;
            case R.id.ibLife:
                HILog.d(TAG, "onClick: ibLife:");
                dialog_broadcasting();
                break;
            case R.id.ib_queryhost:
                mEnable_Fps = false;
                mFps = 0;
                mQVGA = false;
//                ipaddress = toycarGroupData.get(carno).get(CAR_IP);
//                m_udpNetwork.SetP2PAddress(MyTank_IP, Constants.CMDPORT);

                HILog.d(TAG, "onClick: ib_queryhost: mEnable_Fps = " + mEnable_Fps);
                mFlageVideo = !mFlageVideo;
                if(!m_Demomode) ShowFireArms(mFlageVideo);
                onPlay();
                break;
            case R.id.ibRecorder:
                HILog.d(TAG, "onClick: ibRecorder: mEnable_Fps = " + mEnable_Fps);
                mEnable_Fps = false;
                mFps = 0;
                mQVGA = true;
//                ipaddress = toycarGroupData.get(carno).get(CAR_IP);
//                m_udpNetwork.SetP2PAddress(MyTank_IP, Constants.CMDPORT);

                mFlageVideo = !mFlageVideo;
                if(!m_Demomode) ShowFireArms(mFlageVideo);
                onPlay();
/*
                if(BaseActivity.mFlageVideo){
                    showMjpegVideo();
                } else {
                    m_udpNetwork.SendVideoOff();
                    CloseCam();
                    if(videoView!=null || videoView.isPlaying()){
                        videoView.stopPlayback();
                    }
                    videoView.setVisibility(View.GONE);
                }
*/
                break;
        }
        show5Buttons();
    }


    private void setEnableDisable5Buttons(Boolean value){
        int dbvalue;
        HILog.d(TAG, "setEnableDisable5Buttons: value = " + value);
    }

    private void show5Buttons(){
        HILog.d(TAG, "show5Buttons:");
        if(mFlageVideo) {
            ib_queryhost.setBackgroundResource(R.mipmap.btn_video_h);
            ibRecorder.setBackgroundResource(R.mipmap.btn_video_h);
        }
        else {
            ib_queryhost.setBackgroundResource(R.mipmap.btn_video_n);
            ibRecorder.setBackgroundResource(R.mipmap.btn_video_n);
        }
/*
        if(BaseActivity.mFlagMusic) mBtnMusic.setBackgroundResource(R.mipmap.ic_music_active);
        else mBtnMusic.setBackgroundResource(R.mipmap.ic_music_normal);
        if(BaseActivity.mFlagSetting) mBtnSetting.setBackgroundResource(R.mipmap.ic_setting_active);
        else mBtnSetting.setBackgroundResource(R.mipmap.ic_setting_normal);
        if(BaseActivity.mFlagWakeup) mBtnWakeup.setBackgroundResource(R.mipmap.ic_wake_up_active);
        else mBtnWakeup.setBackgroundResource(R.mipmap.ic_wake_up_normal);
        if(BaseActivity.mFlageVideo) mBtnVideo.setBackgroundResource(R.mipmap.ic_video_active);
        else mBtnVideo.setBackgroundResource(R.mipmap.ic_video_normal);
*/
    }

    @Subscribe
    public void getTimerEvent(TimerEvent timerEvent) {
        HILog.d(TAG, "Subscribe : getTimerEvent: ");
        Message timerMessage=new Message();
        timerMessage.what = timerEvent.period;
        timerMessage.obj = timerEvent;
        TimerEventHandler.sendMessage(timerMessage);
    }

    @Subscribe
    public void getUdpEvent(UdpEvent udpEvent) {
        HILog.d(TAG, "Subscribe : getUdpEvent: ");
        Message udpMessage=new Message();
        udpMessage.what = udpEvent.iCommand;
        udpMessage.obj = udpEvent;
        UdpEventHandler.sendMessage(udpMessage);
    }

    private void doRingOn(){
        HILog.d(TAG, "doRingOn: mRingOn = " + mRingOn);
        if(!mRingOn) {
            mRingOn = true;
            mIvWakeup.setVisibility(View.VISIBLE);
            frameAnimation = (AnimationDrawable)mIvWakeup.getDrawable();
            frameAnimation.setCallback(mIvWakeup);
            frameAnimation.setVisible(true, true);
            m_RingOnHandler.postDelayed(m_RingOnTimeoutTask, Constants.RINGONTIMEOUT);
            playWakeupMp3();
        }
    }

    public void stopWakeupMp3(){
        HILog.d(TAG, "stopWakeupMp3:");
        StopMp3Event stopMp3Event = new StopMp3Event();
        stopMp3Event.mp3path = mWakeupMp3Path;
        stopMp3Event.remote = false;
        UltraflyModelApplication.getInstance().bus.post(stopMp3Event);
    }

    private void playWakeupMp3(){
        HILog.d(TAG, "playWakeupMp3:");
        mWakeupListCursor = getWakeupListCursor();
        if(mWakeupListCursor==null||mWakeupListCursor.getCount()==0) {
            HILog.d(TAG, "playWakeupMp3: mWakeupListCursor is null or empty!");
            return;
        }
        int count = mWakeupListCursor.getCount();
        HILog.d(TAG, "playWakeupMp3: count = " + count);
        mWakeupListCursor.moveToFirst();
        mWakeupMp3Path = mWakeupListCursor.getString(mWakeupListCursor.getColumnIndex(DBA.Field.PATH));
        HILog.d(TAG, "playWakeupMp3: mp3filepath = " + mWakeupMp3Path);
        if(count>0 && !StringUtil.isStrNullOrEmpty(mWakeupMp3Path)){
            PlayMp3Event playMp3Event = new PlayMp3Event();
            playMp3Event.mp3path = mWakeupMp3Path;
            playMp3Event.remote = false;
            UltraflyModelApplication.getInstance().bus.post(playMp3Event);
            mIsPlaying = true;
        }
    }

    private void doRingOff(){
        HILog.d(TAG, "doRingOff: mRingOn = " + mRingOn);
        if(mRingOn) {
            frameAnimation.stop();
            mRingOn = false;
            mIvWakeup.setVisibility(View.INVISIBLE);
            stopWakeupMp3();
        }
    }

    @Subscribe
    public void getTimeOutEvent(TimeOutEvent timeOutEvent) {
        HILog.d(TAG, "getTimeOutEvent:");
        dialog_score();
    }

    @Subscribe
    public void getInjuredEvent(InjuredEvent injuredEvent) {
        String tankname = injuredEvent.tankname;
        HILog.d(TAG, "getInjuredEvent: tankname = " + tankname);
        int tankno = IsThisTankInList(tankname);
        if(tankno>=0){
            Injured[tankno] += 1;
            HILog.d(TAG, "getInjuredEvent: tankno = " + tankno + ": " + Injured[tankno]);
            Message scoreMessage=new Message();
            scoreMessage.what = tankno;
            scoreMessage.obj = injuredEvent;
            InjuredEventHandler.sendMessage(scoreMessage);
        } else {
            HILog.d(TAG, "getInjuredEvent: no injuured tank found!");
        }
    }

    public void DoVibrate(int period){
        // Get instance of Vibrator from current Context
        HILog.d(TAG, "DoVibrate:");
        Vibrator v = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(period);
    }

    @Subscribe
    public void getWinnerEvent(WinnerEvent winnerEvent) {
        String tankname = winnerEvent.tankname;
        HILog.d(TAG, "getWinnerEvent: tankname = " + tankname);
        int tankno = IsThisTankInList(tankname);
        if(tankno>=0&&tankno!=mAmIOwnTheCar){
            Winner[mAmIOwnTheCar] += 1;
            HILog.d(TAG, "getWinnerEvent: tankno = " + tankno + ": " + Winner[tankno]);
            Message scoreMessage=new Message();
            scoreMessage.what = tankno;
            scoreMessage.obj = winnerEvent;
            WinnerEventHandler.sendMessage(scoreMessage);
        } else {
            HILog.d(TAG, "getInjuredEvent: no winner tank found!");
        }
    }

    private Handler PolabearEventHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            HILog.d(TAG, "PolabearEventHandler:");
            switch (msg.what) {
                case Constants.POLARBEAR_EVENT_RESETCARS:
                    HILog.d(TAG, "PolabearEventHandler: POLARBEAR_EVENT_RESETCARS:");
                    ShowOnlySettting();
                    UltraflyModelApplication.getInstance().bus.post(new StartBCEvent());
                    break;

            }
        }
    };

    @Subscribe
    public void getResetCarDataEvent(ResetCarDataEvent resetCarDataEvent) {
        HILog.d(TAG, "Subscribe : getResetCarDataEvent: toycarGroupData.size = " + toycarGroupData.size());
        toycarGroupData.clear();
        HILog.d(TAG, "Subscribe : getResetCarDataEvent: toycarGroupData.size = " + toycarGroupData.size());
        Message polarbearMessage=new Message();
        polarbearMessage.what = Constants.POLARBEAR_EVENT_RESETCARS;
        PolabearEventHandler.sendMessage(polarbearMessage);
    }

    @Subscribe
    public void getEndMp3Event(EndMp3Event endMp3Event) {
        HILog.d(TAG, "Subscribe : getEndMp3Event: remote = " + endMp3Event.remote);
        if(!endMp3Event.remote) {
            doRingOff();
        } else {

        }
    }

    private Cursor getWakeupListCursor(){
        String whereclause = null;
        Cursor resultCursor=null;
        HILog.d(TAG, "getPlayListCursor:");
        Select select = new Select();
        HILog.d(TAG, "getPlayListCursor: WAKEUPSELECTED!");
        whereclause = DBA.Field.WAKEUPSELECTED  + " = " + StringUtil.addquote(String.valueOf(Constants.SWIPPED));

        String sqlcommand = select.from(MusicList.class)
                .where(whereclause)
                .toSql();
        HILog.d(TAG, "getPlayListCursor: sqlcommand = " + sqlcommand);
        resultCursor = Cache.openDatabase().rawQuery(sqlcommand, null);
        HILog.d(TAG, "getPlayListCursor: count = " + resultCursor.getCount());
        return resultCursor;
    }

    private Handler WinnerEventHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            HILog.d(TAG, "WinnerEventHandler:");
            super.handleMessage(msg);
            ShowMyScore();
        }
    };

    private Handler InjuredEventHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
        HILog.d(TAG, "InjuredEventHandler:");
        super.handleMessage(msg);
        ShowMyScore();
        int carno = msg.what;
        if(!NOSELSHOOTING | AmIOwnTheCar(carno)){
            PlayMp3Event playMp3Event = new PlayMp3Event();
            playMp3Event.mp3path = "android.resource://" + mActivity.getPackageName() + "/"+ R.raw.bomb;
            playMp3Event.remote = false;
            UltraflyModelApplication.getInstance().bus.post(playMp3Event);
            iv_injured.setVisibility(View.VISIBLE);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    iv_injured.setVisibility(View.INVISIBLE);
                } //run
            }, 1000);
            DoVibrate(Constants.INJUREDPERIOD);
        }
    }
};

    private void ShowTimer(int period){
//        HILog.d(TAG, "StartAndShowTimer:");
        StringBuilder sb = new StringBuilder();
        int min = period / 60;
        sb.append(String.format("%02d", min));
        sb.append(":");
        sb.append(String.format("%02d", (period-min*60)));
        tv_clock.setText(sb.toString());
    }

    private Handler TimerEventHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            HILog.d(TAG, "TimerEventHandler:");
            super.handleMessage(msg);
            m_TimerPeriod = msg.what;
            ShowTimer(msg.what);
            m_CountDownHandler.postDelayed(m_CountDownTimeoutTask, 1000);
            resetscores();
            ShowMyScore();
            mInBattle = true;
        }
    };

    private Handler UpdateUIEventHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            HILog.d(TAG, "UpdateUIEventHandler:");
            super.handleMessage(msg);
            switch(msg.what){
                case Constants.UPDATEUI_SHOWTOYCARLIST:
                    HILog.d(TAG, "UpdateUIEventHandler: UPDATEUI_SHOWTOYCARLIST");
                    ShowToyCarList();
                    break;
                case Constants.UPDATEUI_SHOWONLYSETTING:
                    HILog.d(TAG, "UpdateUIEventHandler: UPDATEUI_SHOWONLYSETTING");
                    ShowOnlySettting();
                    break;
            }

        }
    };


    private Handler UdpEventHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            HILog.d(TAG, "UdpEventHandler:");
            super.handleMessage(msg);
            int iCommand = msg.what;
            UdpEvent udpEvent = (UdpEvent) msg.obj;
            int iResponse = udpEvent.iResponse;
            int iLen  = udpEvent.iLen;
            byte[] byResponse=null;
            String localport = udpEvent.LocalPort;
            if(iLen>0) byResponse = (byte[]) udpEvent.byResponse;

            switch(msg.what){
                case P2PNatProcess.RECEIVE_AUDIO_DATA:
                    HILog.d(TAG, "UdpEventHandler: RECEIVE_AUDIO_DATA:");
//                    byte[] byAudioDate = P2PNatProcess.get(byResponse, 4);
//                    if(m_AudioPlayer!=null) m_AudioPlayer.WriteAudioData(byResponse,iLen-4);
                    break;
                case P2PNatProcess.RING_ON:
                    HILog.d(TAG, "UdpEventHandler: RING_ON:");
                    doRingOn();
                    break;
                case P2PNatProcess.MP3_FILE_INIT_SUCCESS:
                    HILog.d(TAG, "UdpEventHandler: MP3_FILE_INIT_SUCCESS:");
//                    short sLen = PolarbearMainActivity.GetFileSection();
//                    m_udpNetwork.StartMP3FileSection(sLen);
                    break;
                case P2PNatProcess.BROADCAST:
/*
10-28 12:38:46.247 11140-11140/com.ultraflymodel.polarbear D/PolarbearMainFragment: HILog:UdpEventHandler: BROADCAST: iCommand = 27, iLen = 28,  localport = 8520
10-28 12:38:46.247 11140-11140/com.ultraflymodel.polarbear D/PolarbearMainFragment: HILog:setEnableDisable5Buttons: value = true
10-28 12:38:46.247 11140-11140/com.ultraflymodel.polarbear D/PolarbearMainFragment: HILog:UdpEventHandler: BROADCAST: strbyData = Tank-65;192.168.0.101
10-28 12:38:46.247 11140-11140/com.ultraflymodel.polarbear D/PolarbearMainFragment: HILog:UdpEventHandler: BROADCAST: DEVICEINFO: deviceinfo.strName = Tank-65
10-28 12:38:46.247 11140-11140/com.ultraflymodel.polarbear D/PolarbearMainFragment: HILog:UdpEventHandler: BROADCAST: DEVICEINFO: deviceinfo.strIPAddress = 192.168.0.101
 */
                    HILog.d(true, TAG, "UdpEventHandler: BROADCAST: iCommand = " + iCommand + ", iLen = " + iLen + ",  localport = " + localport);

                    PolarbearMainActivity.dialog_broadcast_dismiss();
//                    setEnableDisable5Buttons(true);
                    if(!localport.equals(Constants.BCPORT)){
                        HILog.d(TAG, "UdpEventHandler: BROADCAST: Ignoring the  localport = " + localport);
                        break;
                    }

                    List<Byte> byListBuffer = new ArrayList<Byte>();
                    for(int i=4;i<=iLen;i++)
                    {
                        if(byResponse[i]!=0)
                        {
                            byListBuffer.add(byResponse[i]);
                        }
                    }
                    byte[]  byData = new byte[byListBuffer.size()];
                    for (int index = 0; index < byListBuffer.size(); index++) {
                        byData[index] = byListBuffer.get(index);
                    }

                    String strbyData = new String(byData);
                    HILog.d(true, TAG, "UdpEventHandler: BROADCAST: strbyData = " + strbyData);

                    final String[]  strDevice = strbyData.split(";");
                    if(strDevice.length!=4) break;
                    final int carno = IsThisCarInList(strDevice[1]);
                    String status = strDevice[3];
                    if(carno>=0){
                        toycarGroupData.get(carno).put(CAR_NAME, strDevice[0]);
                        toycarGroupData.get(carno).put(CAR_IP, strDevice[1]);
                        toycarGroupData.get(carno).put(CAR_TYPE, strDevice[2]);
                        toycarGroupData.get(carno).put(CAR_STATUS, status);
                        toycarGroupData.get(carno).put(CAR_ALIVE, String.valueOf(true));
                    } else {
                        HashMap<String, String> newToycarData = new HashMap<String, String>();
                        newToycarData.put(CAR_NAME, strDevice[0]);
                        newToycarData.put(CAR_IP, strDevice[1]);
                        newToycarData.put(CAR_TYPE, strDevice[2]);
                        newToycarData.put(CAR_STATUS, status);
                        newToycarData.put(CAR_HP, Constants.ZERO);
                        newToycarData.put(CAR_HIT, Constants.ZERO);
                        newToycarData.put(CAR_OWNER, Constants.OWNER_NONE);
                        newToycarData.put(CAR_ALIVE, String.valueOf(true));
                        toycarGroupData.add(newToycarData);
                        if(getCarType(strDevice[0])>=0){ //Reset  DEVICE_NAME
                            HILog.d(TAG, "UdpEventHandler: BROADCAST: Reset  DEVICE_NAME");
                            mInsPolarbear.putString(ISKey.DEVICE_NAME, "");
                            mInsPolarbear.save();
                        }
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if(carno==-1){
                                    if(toycarGroupData.size()>0)
                                    toycarGroupData.get(0).put(CAR_ALIVE, String.valueOf(true));
                                    if(!mAliveTask){ //Start only once.
                                        mAliveTask = true;
//                                        m_TankAliveHandler.postDelayed(m_TankAliveTimeoutTask, Constants.TANKALIVETIMEOUT);
                                        if(AliveTimer!=null) {
                                            AliveTimer.cancel();
                                            AliveTimer.purge();
                                        }
                                        AliveTimer = new Timer(true);
                                        AliveTimer.schedule(new m_TankAliveTimeoutTask(), Constants.TANKALIVETIMEOUT, Constants.TANKALIVETIMEOUT);
                                        HILog.d(true, TAG, "m_TankAliveHandler: m_TankAliveTimeoutTask: started just now.");
                                    }
                                    AutoSelectMyCar();
                                }
                            } //run
                        }, 5000);
                        HILog.d(TAG, "UdpEventHandler: BROADCAST: toycarGroupData.size = " + toycarGroupData.size());
                    }

                    ShowToyCarList();


//                    PolarbearMainActivity.mQueryHost = false;
                    break;
            }
        }
    };

    private void HideAllCars(){
        ShowOneCar(ibCar_1, true, 0);
        ShowOneCar(ibCar_2, true, 0);
        ShowOneCar(ibCar_3, true, 0);
        ShowOneCar(ibCar_4, true, 0);
        ShowOneCar(ibCar_5, true, 0);
    }

    private void ShowOneCar(int id, ImageButton theCar){
        int resid = R.mipmap.btn_t01_n;
        if(id>=toycarGroupData.size()) return;

        String status = toycarGroupData.get(id).get(CAR_STATUS);
        String owner = toycarGroupData.get(id).get(CAR_OWNER);
        String cartype = toycarGroupData.get(id).get(CAR_TYPE);
        HILog.d(TAG, "ShowOneCar: id = " + id + ", status = " + status + ", owner = " + owner + ", cartype = " + cartype);
        int whichcar = getCarType(cartype);
        switch (whichcar){
            case 0:
                resid = R.mipmap.btn_t01_n;
                if(status.equals(Constants.STATUS_AVAILABLE)) {
                    resid = R.mipmap.btn_t01_n;
                } else if(owner.equals(Constants.OWNER_ME)){
                    resid = R.mipmap.btn_t0102_h;
                } else {
                    resid = R.mipmap.btn_t0102_d;
                }
                break;
            case 1:
                resid = R.mipmap.btn_t02_n;
                if(status.equals(Constants.STATUS_AVAILABLE)) {
                    resid = R.mipmap.btn_t02_n;
                } else if(owner.equals(Constants.OWNER_ME)){
                    resid = R.mipmap.btn_t0102_h;
                } else {
                    resid = R.mipmap.btn_t0102_d;
                }
                break;
            case 2:
                resid = R.mipmap.btn_v01_n;
                if(status.equals(Constants.STATUS_AVAILABLE)) {
                    resid = R.mipmap.btn_v01_n;
                } else if(owner.equals(Constants.OWNER_ME)){
                    resid = R.mipmap.btn_v01_h;
                } else {
                    resid = R.mipmap.btn_v01_d;
                }
                break;
            case 3:
                resid = R.mipmap.btn_v02_n;
                if(status.equals(Constants.STATUS_AVAILABLE)) {
                    resid = R.mipmap.btn_v02_n;
                } else if(owner.equals(Constants.OWNER_ME)){
                    resid = R.mipmap.btn_v02_h;
                } else {
                    resid = R.mipmap.btn_v02_d;
                }
                break;
            case 4:
                resid = R.mipmap.btn_v03_n;
                if(status.equals(Constants.STATUS_AVAILABLE)) {
                    resid = R.mipmap.btn_v03_n;
                } else if(owner.equals(Constants.OWNER_ME)){
                    resid = R.mipmap.btn_v03_h;
                } else {
                    resid = R.mipmap.btn_v03_d;
                }
                break;
        }

        if(id<toycarGroupData.size()){
            theCar.setVisibility(View.VISIBLE);
            theCar.setBackgroundResource(resid);
        }
    }

    private void ShowOneCar(ImageButton theCar, boolean tohide, int resid){
        if(tohide){
            theCar.setVisibility(View.INVISIBLE);
        } else {
            theCar.setVisibility(View.VISIBLE);
        }
        theCar.setBackgroundResource(resid);
    }

    public static int IsThisTankInList(String tankname_tocheck){
        HILog.d(TAG, "IsThisTANKInList:");
        int value = -1;
        for(int i=0; i<toycarGroupData.size(); i++){
            String tankname = toycarGroupData.get(i).get(CAR_NAME);
            if(tankname_tocheck.equals(tankname)) {
                value = i;
                HILog.d(TAG, "IsThisTANKInList: found in the list tankname = " + tankname);
                break;
            }
        }
        return value;
    }

    private int IsThisCarInList(String ip_tocheck){
        HILog.d(TAG, "IsThisCarInList:");
        int value = -1;
        for(int i=0; i<toycarGroupData.size(); i++){
            String carip = toycarGroupData.get(i).get(CAR_IP);
            if(ip_tocheck.equals(carip)) {
                value = i;
                HILog.d(TAG, "IsThisCarInList: found in the list ip = " + ip_tocheck);
                break;
            }
        }
        return value;
    }

    private boolean IsThisCarAvailable(int id){
        HILog.d(TAG, "IsThisCarAvailable: id = " + id);
        boolean value = false;
        if(toycarGroupData.size()>0){
            String status = toycarGroupData.get(id).get(CAR_STATUS);
            HILog.d(TAG, "IsThisCarAvailable: status = " + status);
            if(status.equals(Constants.STATUS_AVAILABLE)) value=true;
        }
        HILog.d(TAG, "IsThisCarAvailable: value = " + value);
        return value;
    }

    private boolean AmIOwnTheCar(int id){
        HILog.d(TAG, "AmIOwnTheCar: id = " + id);
        boolean value = false;
        if(toycarGroupData.size()>0){
            String owner = toycarGroupData.get(id).get(CAR_OWNER);
            if(!owner.equals(Constants.OWNER_NONE)) value=true;
        }
        return value;
    }

    private void ShowToyCarList(){
        HILog.d(TAG, "ShowToyCarList:");
        ll_carlist.setVisibility(View.VISIBLE);
        HideAllCars();
        for(int i=0; i<toycarGroupData.size(); i++){
            ShowOneCar(i, CarPark[i]);
        }
    }

    private void SaveCarInfoIntoDB(String strIPAddress, String strName){
        From from = new Select().from(Settings.class);
        boolean exists = from.exists();
        HILog.d(TAG, "SaveCarInfoIntoDB: exists  = " + exists);
        ActiveAndroid.beginTransaction();
        if(!exists) {
            Settings newsettings = new Settings();
            newsettings.my_copper_name = strName;
            newsettings.my_copper_name = strIPAddress;
            newsettings.broadcast_set = Constants.ON;
            newsettings.save();
        } else {
            List<Settings> settingsList = from.execute();
            settingsList.get(0).my_copper_name = strName;
            settingsList.get(0).my_copper_ip = strIPAddress;
            settingsList.get(0).broadcast_set = Constants.ON;
            HILog.d(TAG, "UdpEventHandler: BROADCAST: pir_on = " + settingsList.get(0).pir_on);
            settingsList.get(0).save();
        }
        ActiveAndroid.setTransactionSuccessful();
        ActiveAndroid.endTransaction();
        CommonUtils.CopyDBFromData2SD(getActivity());
    }

    private void dialog_score(){
        HILog.d(TAG, "dialog_score:");
        boolean result = AmIWinner();
        String sResult = getString(R.string.you_loose);
        if(result) sResult = getString(R.string.you_are_winner);
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .titleColorRes(R.color.text_style10)
                .titleGravity(GravityEnum.CENTER)
                .positiveColorRes(R.color.text_style10)
                .widgetColorRes(R.color.md_divider_white)
                .title(sResult)
                .positiveText(R.string.ok)
                .buttonsGravity(GravityEnum.CENTER)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        HILog.d(TAG, "dialog_score: OK");
                        resetscores();
                        ShowMyScore();
                    }
                })
                .show();
    }

    private void dialog_Clock(){
        HILog.d(TAG, "dialog_Clock:");
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .titleColorRes(R.color.bc_text_fg)
                .positiveColorRes(R.color.bc_text_fg)
                .widgetColorRes(R.color.md_divider_white)
                .title(R.string.period_to_countdown)
                .positiveText(R.string.ok)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(R.string.default_period, R.string.default_period, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {

                        String period = String.valueOf(input).trim();
                        HILog.d(TAG, "dialog_Clock: period = " + period);
//                        String[]  strPeriod = period.split(":");
//                        HILog.d(TAG, "dialog_Clock: strPeriod.length = " + strPeriod.length);
//                        if(strPeriod.length>0){
//                            int seconds = Integer.valueOf(strPeriod[0])*60+Integer.valueOf(strPeriod[1]);
                            if(!StringUtil.isStrNullOrEmpty(period)){
                                int seconds = Integer.valueOf(period)*60;
                                HILog.d(TAG, "dialog_Clock: seconds = " + seconds);
                                m_udpNetwork.SendCountDown2(seconds);
                                resetscores();
                                ShowMyScore();
                            }

//                        }


                    }
                }).show();
    }

    private void dialog_broadcasting(){
        HILog.d(TAG, "dialog_broadcasting:");
        String my_copperhead_ip = null;
        String db_copperhead_ip = PolarbearMainActivity.getSettingStringValue(DBA.Field.MYCOPPERIP);
        if(!StringUtil.isStrNullOrEmpty(db_copperhead_ip)){
            my_copperhead_ip = db_copperhead_ip;
        } else my_copperhead_ip = getString(R.string.my_copperhead_ip);
        final String finalMy_copperhead_ip = my_copperhead_ip;
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .titleColorRes(R.color.bc_text_fg)
                .positiveColorRes(R.color.bc_text_fg)
                .widgetColorRes(R.color.md_divider_white)
                .title(R.string.starting_ip_to_broadcasting)
                .positiveText(R.string.ok)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(my_copperhead_ip, my_copperhead_ip, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        // Do something

                        mCopperheadIp = String.valueOf(input).trim();

                        if(StringUtil.isStrNullOrEmpty(String.valueOf(input))){
                            mCopperheadIp = finalMy_copperhead_ip;
                        }
                        HILog.d(TAG, "onClick: btn_setting: onInput = " + mCopperheadIp);
                        PolarbearMainActivity.setSettingStringValue(DBA.Field.MYCOPPERIP, mCopperheadIp);
                        PolarbearMainActivity.dialog_broadcast();
                        mDeviceList.clear();
                        new Thread(new Runnable()
                        {
                            public void run()
                            {
                                m_udpNetwork.QueryHost(mCopperheadIp);
                            }
                        }).start();
                    }
                }).show();
    }


    @Override
    public boolean onLongClick(View v) {
        HILog.d(TAG, "onLongClick:");
        PolarbearMainActivity.mNotInFront = true;
        Bundle bundle = new Bundle();
        AboutFragment aboutFragment = new AboutFragment();
        aboutFragment.setArguments(bundle);
        BaseActivity.loadFragment(aboutFragment, Constants.JumpTo.ABOUT.toString(), false);
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        HILog.d(TAG, "onCompletion:");
        doRingOff();
    }

    private void ClearNowSending(){
        HILog.d(TAG, "ClearNowSending:");
        mPosition = 0;
        mPlayToSendSize = getPlayToSendSize();
        SearchMusicListFragment.ClearMusicListField(DBA.Field.NOWSENDING);
    }

    @Subscribe
    public void getEndMp3Event2(EndMp3Event endMp3Event) {
        HILog.d(TAG, "Subscribe : getEndMp3Event2: mp3path = " + endMp3Event.mp3path);
        HILog.d(TAG, "Subscribe : getEndMp3Event2: mPlayToSendSize = " + --mPlayToSendSize);
        if(BaseActivity.mFlagMusic) {
            if(mPlayToSendSize<=0) {
                ClearNowSending();
//                setSendToPlayMp3();
            } else  {
                SetPlayListField(DBA.Field.NOWSENDING, mPosition++, Constants.CLEARED);
                SendToPlayMp3();
            }
        }
    }

    private void SetPlayListField(String fieldname, int position, int value){
        HILog.d(TAG, "SetPlayListField: fieldname = " + fieldname + ", position: " + position + ", value = " + value);
        String whereclause = null;
        String title = null;
        HILog.d(TAG, "SetPlayListField:");
        From from = new Select().from(MusicList.class);
        boolean exists = from.exists();
        HILog.d(TAG, "SetPlayListField: exists  = " + exists);
        if(!exists) return;
        whereclause = DBA.Field.PLAYSELECTED ;
        whereclause += " = " + StringUtil.addquote(String.valueOf(Constants.SWIPPED));
        HILog.d(TAG, "SetPlayListField: whereclause = " + whereclause);

        List<MusicList> playlists = from.where(whereclause).execute();
        int size = playlists.size();
        HILog.d(TAG, "SetPlayListField: total size  = " + size);
        if(size>0&&position>=0){
            ActiveAndroid.beginTransaction();
            switch(fieldname){
                case DBA.Field.NOWPLAYING:
                    title = playlists.get(position).title;
                    HILog.d(TAG, "SetPlayListField: title = " + title + ", NOWPLAYING: set value to "  + value);
                    playlists.get(position).now_playing = value;
                    break;
                case DBA.Field.PLAYSELECTED:
                    title = playlists.get(position).title;
                    HILog.d(TAG, "SetPlayListField: title = " + title + ", PLAYSELECTED: set value to "  + value);
                    playlists.get(position).play_selected = value;
                    break;
                case DBA.Field.WAKEUPSELECTED:
                    title = playlists.get(position).title;
                    HILog.d(TAG, "SetPlayListField: title = " + title + ", WAKEUPSELECTED: set value to "  + value);
                    playlists.get(position).wakeup_selected = value;
                    break;
                default:
                    HILog.d(TAG, "SetPlayListField: not supported fieldname =: " + fieldname );
                    break;
            }
            playlists.get(position).save();
            ActiveAndroid.setTransactionSuccessful();
            ActiveAndroid.endTransaction();
            CommonUtils.CopyDBFromData2SD(getActivity());
        } else {
            HILog.d(TAG, "SetPlayListField: null or empty." );
        }
    }

    private int getPlayToSendSize(){
        HILog.d(TAG, "getPlayToSendSize:");
        int value = 0;
        String whereclause = null;
        Cursor resultCursor=null;
        HILog.d(TAG, "getPlayListCursor:");
        Select select = new Select();
        whereclause = DBA.Field.PLAYSELECTED ;
        whereclause += " = " + StringUtil.addquote(String.valueOf(Constants.SWIPPED));
        String sqlcommand = select.from(MusicList.class)
                .where(whereclause)
                .toSql();
        HILog.d(TAG, "getPlayToSendSize: sqlcommand = " + sqlcommand);
        resultCursor = Cache.openDatabase().rawQuery(sqlcommand, null);
        value = resultCursor.getCount();
        HILog.d(TAG, "getPlayToSendSize: value = " + value);
        return value;
    }

    private void SendToPlayMp3(){
        HILog.d(TAG, "SendToPlayMp3:");
        String whereclause = null;
        Select select = new Select();
        whereclause = DBA.Field.PLAYSELECTED + " = " + StringUtil.addquote(String.valueOf(Constants.SWIPPED));
        List<MusicList> playtosendLists = select.from(MusicList.class).where(whereclause).execute();
        mMaxPlaylist = playtosendLists.size();
        HILog.d(TAG, "SendToPlayMp3: mPlayToSendSize = " + mMaxPlaylist);
        if(mMaxPlaylist>0){
            HILog.d(TAG, "SendToPlayMp3: mPosition = " + mPosition);
            String strPath = playtosendLists.get(mPosition).path;
            String title = playtosendLists.get(mPosition).title;
            HILog.d(TAG, "SendToPlayMp3: tile = " + title);
            HILog.d(TAG, "SendToPlayMp3: strPath = " + strPath);
            ActiveAndroid.beginTransaction();
            playtosendLists.get(mPosition).now_sending = Constants.ON;
            playtosendLists.get(mPosition).save();
            ActiveAndroid.setTransactionSuccessful();
            ActiveAndroid.endTransaction();
            CommonUtils.CopyDBFromData2SD(getActivity());
            PlayMp3Event playMp3Event = new PlayMp3Event();
            playMp3Event.mp3path = strPath;
            playMp3Event.remote = true;
            UltraflyModelApplication.getInstance().bus.post(playMp3Event);
            HILog.d(TAG, "SendToPlayMp3: sent playMp3Event");
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
/*
        MicOnOffEvent micOnOffEvent;
        Message micMessage;
        if(v.getId()==R.id.btn_portait_speak){
            switch(event.getAction()){
                case MotionEvent.ACTION_UP:
                    HILog.d(TAG, "ACTION_UP:");
                    if(Constants.VIDEORECORDING){
                        PolarbearMainActivity.mVIDEORECORDING = false;
                        try {
                            PolarbearMainActivity.mVR_OS.flush();
                            PolarbearMainActivity.mVR_OS.close();
                            PolarbearMainActivity.mVR_OS = null;
                        } catch (IOException e) {
                            HILog.d(true, TAG, "IOException: ");
                            e.printStackTrace();
                        }
                        HILog.d(true, TAG, "ACTION_UP: saved mVR_filename = " + PolarbearMainActivity.mVR_filename);
//                        PolarbearMainActivity.mVR_Play_filename = PolarbearMainActivity.mVR_filename;
//                        HILog.d(true, TAG, "ACTION_UP: mVR_Play_filename = " + PolarbearMainActivity.mVR_Play_filename);
                        PolarbearMainActivity.mVR_filename = null;
                    } else {
                        micOnOffEvent = new MicOnOffEvent();
                        micOnOffEvent.OnOffFlag = false;
                        UltraflyModelApplication.getInstance().bus.post(micOnOffEvent);

                        m_udpNetwork.SendAudioOn();
                        m_bIsOpenAudio = true;
                    }
                    break;
                case MotionEvent.ACTION_DOWN:
                    HILog.d(TAG, "ACTION_DOWN:");
                    if(Constants.VIDEORECORDING){
                        PolarbearMainActivity.mVIDEORECORDING = true;
                        File sd = Environment.getExternalStorageDirectory();
                        PolarbearMainActivity.mVR_filename = "/Download/" + CommonUtils.getDateString() + ".mp4";
                        PolarbearMainActivity.mVR_File = new File(sd, PolarbearMainActivity.mVR_filename);
                        HILog.d(true, TAG, "ACTION_DOWN: mVR_File Path = " + PolarbearMainActivity.mVR_File.getPath());
                        HILog.d(true, TAG, "ACTION_DOWN: mVR_File getName = " + PolarbearMainActivity.mVR_File.getName());
                        try {
                            PolarbearMainActivity.mVR_OS = new FileOutputStream(PolarbearMainActivity.mVR_File, true);
                        } catch (FileNotFoundException e) {
                            HILog.d(true, TAG, "FileNotFoundException: ");
                            e.printStackTrace();
                        }
                    } else {
                        m_udpNetwork.SendAudioOFF();
                        m_bIsOpenAudio = false;

                        if(mAudioPermission) {
                            micOnOffEvent = new MicOnOffEvent();
                            micOnOffEvent.OnOffFlag = true;
                            UltraflyModelApplication.getInstance().bus.post(micOnOffEvent);
                        } else {
                            HILog.d(TAG, "ACTION_DOWN: mAudioPermission = " + mAudioPermission);
                        }
                    }
                    break;
            }
        }
*/
        return false;
    }

    public boolean AskForAudioRecordPermissio(){
        HILog.d(TAG, "AskForAudioRecordPermissio");
        boolean value = false;
        if(PermissionsManager.get().isAudioRecordingGranted()){
            value = true;
        } else  {
            PermissionsManager.get().requestAudioRecordingPermission(this);
            HILog.d(TAG, "requestAudioRecordingPermission");
        }
        return value;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
    {
        surfaceWidth = width;
        surfaceHeight = height;

        renderer = new VideoTextureRenderer(mActivity, m_surface.getSurfaceTexture(), surfaceWidth, surfaceHeight);

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        m_VideoDecoder.m_surface  = new Surface(renderer.getVideoTexture());
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch(seekBar.getId())
        {
            case  R.id.seekBarLeft: //ToDo: channel_1
                HILog.d(TAG, "seekBarLeft:");
                m_iWheel = progress + 1000;
                break;
            case  R.id.seekBarRight: //ToDo: channel_2
                HILog.d(TAG, "seekBarRight:");
                m_iWheel2 = progress + 1000;
                break;

            case  R.id.seekBar_ch4: ////ToDo: channel_4
                HILog.d(TAG, "seekBar_ch4:");
                m_iWheel3 = progress + 1000;
                break;
            case  R.id.seekBar_ch5://ToDo: channel_5
                HILog.d(TAG, "seekBar_ch5:");
                m_iWheel4 = progress + 1000;
                break;
            case  R.id.seekBar_ch3: //ToDo: channel_3
                HILog.d(TAG, "seekBar_ch3:");
                m_iWheel5 = progress + 1000;
                break;

        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        HILog.d(TAG, "onStartTrackingTouch:");
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        HILog.d(TAG, "onStopTrackingTouch:");
        VerticalSeekBar  bar = (VerticalSeekBar)seekBar;
        bar.setProgressAndThumb(500);
    }

    public boolean IsVideoOn(){
        boolean value = false;

        if (videoView != null){
            if (videoView.isPlaying())
                value = true;
        }

        return value;
    }

    public void onPlay()
    {
        HILog.d(TAG, "onPlay: ");
        if (videoView == null)
        {
            HILog.d(TAG, "onPlay: videoView is null!");
            showMjpegVideo();
        }
        else
        {
            if (videoView.isPlaying())
            {
                HILog.d(TAG, "videoView.isPlaying()!");
//               ibRecorder.setBackgroundResource(R.mipmap.btn_videoon_n);
                m_udpNetwork.SendVideoOff();
                videoView.stopPlayback();
                videoView.setVisibility(View.GONE);
                videoView = null;
            }
            else
            {
                HILog.d(TAG, "videoView is Not Playing()!");
//              ibRecorder.setBackgroundResource(R.mipmap.btn_videoon_h);
                if(mQVGA) m_udpNetwork.SendQVGAVideoOn();
                else m_udpNetwork.SendVideoOn();
                videoView.setVisibility(View.VISIBLE);
                videoView.startPlayback();
                videoView.showFps(mEnable_Fps);
//              mMotorCmdSending = false;
            }
        }
    }


    private void showMjpegVideo()
    {
        HILog.d(TAG, "showMjpegVideo:");

        videoView = (MjpegView) getView().findViewById(R.id.videoView1);

        videoView.setVisibility(View.VISIBLE);
//        ibRecorder.setBackgroundResource(R.mipmap.btn_videoon_h);
        if(mQVGA) m_udpNetwork.SendQVGAVideoOn();
        else m_udpNetwork.SendVideoOn();
        new DoRead().execute("MJ");
    }

    public class DoRead extends AsyncTask<String, Void, MjpegInputStream>
    {
        protected MjpegInputStream doInBackground(String... url)
        {
            HILog.d(TAG, "DoRead: doInBackground:");
            return MjpegInputStream.read(mActivity.getCacheDir());
        }

        protected void onPostExecute(MjpegInputStream result)
        {
            HILog.d(TAG, "DoRead: onPostExecute:");
            videoView.setSource( result);
            videoView.start();
            mEnable_Fps = false;
            videoView.showFps(mEnable_Fps);
        }
    }

}