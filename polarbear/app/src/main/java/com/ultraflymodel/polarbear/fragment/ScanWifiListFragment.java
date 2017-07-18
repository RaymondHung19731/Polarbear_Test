package com.ultraflymodel.polarbear.fragment;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Delete;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.otto.Subscribe;
import com.ultraflymodel.polarbear.R;
import com.ultraflymodel.polarbear.activity.PolarbearMainActivity;
import com.ultraflymodel.polarbear.common.Constants;
import com.ultraflymodel.polarbear.common.DBA;
import com.ultraflymodel.polarbear.common.HILog;
import com.ultraflymodel.polarbear.eventbus.OnBackPresssedEvent;
import com.ultraflymodel.polarbear.eventbus.ResetCarDataEvent;
import com.ultraflymodel.polarbear.eventbus.UdpScanListEvent;
import com.ultraflymodel.polarbear.eventbus.WifiListReset;
import com.ultraflymodel.polarbear.eventbus.WifiListUpdate;
import com.ultraflymodel.polarbear.eventbus.WifiSelectedEvent;
import com.ultraflymodel.polarbear.eventbus.WifiUdpEvent;
import com.ultraflymodel.polarbear.mike.P2PNatProcess;
import com.ultraflymodel.polarbear.mike.UDPNetwork;
import com.ultraflymodel.polarbear.mike.WIfiInfo;
import com.ultraflymodel.polarbear.model.WifiList;
import com.ultraflymodel.polarbear.ultraflymodel.UltraflyModelApplication;
import com.ultraflymodel.polarbear.utils.CommonUtils;
import com.ultraflymodel.polarbear.utils.StringUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class ScanWifiListFragment extends Fragment implements View.OnClickListener, View.OnLongClickListener {
    private static final String TAG = ScanWifiListFragment.class.getSimpleName();
    private static FrameLayout fl_wifilist_container;
    private ImageButton btn_wifi, btn_back, btn_phonewifi;
    private WifiListFragment wifiListFragment = null;
    public static UDPNetwork m_udpNetwork;
    public static MaterialDialog md_diaog=null;
    public static Activity mActivity = null;

    List<WIfiInfo> m_WifiInfoList = new ArrayList<WIfiInfo>();
    List<String> m_WifiStringList = new ArrayList<String>();
    private TextView tv_wifi_you_connect;
    private ImageButton ib_ok;
    private String mWifiName = null;
    private EditText et_password;
    private static boolean mScanning = false;
    private TextView tv_messages;
    private static String mMessages=null;
    private String mPassword = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mainview = inflater.inflate(R.layout.layout_setup_wifi_black, container, false);
        HILog.d(TAG, "onCreateView:");
        m_udpNetwork = PolarbearMainActivity.m_udpNetwork;
        mActivity = getActivity();
        PolarbearMainActivity.mNotInFront = true;
//        btn_wifi = (ImageButton) mainview.findViewById(R.id.btn_wifi);
//        btn_wifi.setOnClickListener(this);
        tv_messages = (TextView) mainview.findViewById(R.id.tv_messages);
        tv_messages.setText("");
        tv_messages.setMovementMethod(new ScrollingMovementMethod());
        btn_back = (ImageButton) mainview.findViewById(R.id.btn_back);
        btn_back.setOnClickListener(this);
        btn_phonewifi = (ImageButton) mainview.findViewById(R.id.btn_phonewifi);
        btn_phonewifi.setOnClickListener(this);
        tv_wifi_you_connect = (TextView) mainview.findViewById(R.id.tv_wifi_you_connect);
        ib_ok = (ImageButton) mainview.findViewById(R.id.ib_ok);
        ib_ok.setOnClickListener(this);
        et_password = (EditText) mainview.findViewById(R.id.et_password);
        mWifiName = PolarbearMainActivity.getSettingStringValue(DBA.Field.WIFINAME);
        if(!StringUtil.isStrNullOrEmpty(mWifiName)) {
            tv_wifi_you_connect.setText(mWifiName);
        }
        DeleteWifiList();
        fl_wifilist_container = (FrameLayout) mainview.findViewById(R.id.fl_wifilist_container);
        if (wifiListFragment == null) {
            Bundle bundle = new Bundle();
            bundle.putInt(Constants.BC_BUNDLE_SELECTED, Constants.PLAYLIST);
            wifiListFragment = new WifiListFragment();
            wifiListFragment.setArguments(bundle);
            getChildFragmentManager().beginTransaction().replace(fl_wifilist_container.getId(), wifiListFragment).commit();
        }
        UltraflyModelApplication.getInstance().bus.register(this);
        btn_phonewifi.performClick();



        return mainview;
    }

    @Override
    public void onStart() {
        HILog.d(TAG, "onStart : ");
        super.onStart();
    }

    public static void DeleteWifiList(){
        HILog.d(TAG, "DeleteWifiList:");
        From from = new Select().from(WifiList.class);
        boolean exists = from.exists();
        HILog.d(TAG, "DeleteWifiList: exists  = " + exists);
        if(!exists) return;
        new Delete().from(WifiList.class).execute();
    }

    @Override
    public void onDestroyView() {
        HILog.d(TAG, "onDestroyView:");
        PolarbearMainActivity.mNotInFront = false;
        UltraflyModelApplication.getInstance().bus.unregister(this);
        if(wifiListFragment!=null)
            getChildFragmentManager().beginTransaction().remove(wifiListFragment).commitAllowingStateLoss();
        System.gc();
        super.onDestroyView();
    }

    public boolean setSsidAndPassword(Context context, String ssid, String ssidPassword) {
        HILog.d(TAG, "setSsidAndPassword: ssid = " + ssid + ", ssidPassword = " + ssidPassword);
        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
            Method getConfigMethod = wifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration wifiConfig = (WifiConfiguration) getConfigMethod.invoke(wifiManager);

            wifiConfig.SSID = ssid;
            wifiConfig.preSharedKey = ssidPassword;

            Method setConfigMethod = wifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
            setConfigMethod.invoke(wifiManager, wifiConfig);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ib_ok:
                HILog.d(TAG, "onClick: ib_ok:");
                mPassword = et_password.getText().toString().trim();
                if(!StringUtil.isStrNullOrEmpty(mPassword)&&!StringUtil.isStrNullOrEmpty(mWifiName)){
                    m_udpNetwork.ChangeClientSSID(mWifiName);
                    m_udpNetwork.ChangeClientPassword(mPassword);
                    PolarbearMainActivity.setSettingStringValue(DBA.Field.WIFINAME, mWifiName);
                    HILog.d(TAG, "onClick: wifiname = " + mWifiName + " write into settings!");
                    PolarbearMainActivity.setSettingStringValue(DBA.Field.WIFIPASSWORD, mPassword);
                    PolarbearMainActivity.setSettingStringValue(DBA.Field.MYCOPPERIP, "");
//                    getActivity().getSupportFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
                }
                break;
            case R.id.btn_phonewifi:
                HILog.d(TAG, "onClick: btn_phonewifi:");
                mScanning = !mScanning;
                if(mScanning){
                    dialog_scanlist();
                    UltraflyModelApplication.getInstance().bus.post(new WifiListReset());
                    m_udpNetwork.ScanClientSSID();
                    mMessages = "Scanning...\n";
                    tv_messages.setText(mMessages);

                } else {
                    dialog_scanlist_dismiss();
                }
                break;
/*
            case R.id.btn_wifi:
                HILog.d(TAG, "onClick: btn_wifi:");
                break;
*/
            case R.id.btn_back:
                HILog.d(TAG, "onClick: btn_back:");
                int count = mActivity.getFragmentManager().getBackStackEntryCount();
                HILog.d(TAG, "onClick : btn_back: count = " + count);
                UltraflyModelApplication.getInstance().bus.post(new OnBackPresssedEvent());
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.btn_menu:
                HILog.d(TAG, "onLongClick: btn_menu:");
                break;
            case R.id.btn_wake_up_small:
                HILog.d(TAG, "onLongClick: btn_wake_up_small:");
                break;
        }
        return false;
    }

    public void dialog_showmessage(String message){
        md_diaog = new MaterialDialog.Builder(mActivity)
                .title(R.string.app_name)
                .content(message)
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .show();
    }

    public void dialog_scanlist(){
        md_diaog = new MaterialDialog.Builder(mActivity)
                .title(R.string.app_name)
                .content(mActivity.getString(R.string.wifi_scanning))
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .show();
    }

    public static void dialog_scanlist_dismiss(){
        HILog.d(TAG, "dialog_scanlist_dismiss:");
        if(md_diaog!=null){
            if(mScanning) mScanning = false;
            md_diaog.dismiss();
            md_diaog = null;
            HILog.d(TAG, "dialog_scanlist_dismiss: done");
            WifiUdpEvent wifiUdpEvent = new WifiUdpEvent();
            wifiUdpEvent.mDialog = false;
            wifiUdpEvent.message = "WiFi done scanning.";
            UltraflyModelApplication.getInstance().bus.post(wifiUdpEvent);
        }
    }

    @Subscribe
    public void getWifiSelectedEvent(WifiSelectedEvent wifiSelectedEvent) {
        HILog.d(TAG, "Subscribe : getWifiSelectedEvent: ");
        mWifiName = wifiSelectedEvent.wifiname;
        tv_wifi_you_connect.setText(mWifiName);
/*
                            m_udpNetwork.ChangeClientSSID(wifiname);
                            m_udpNetwork.ChangeClientPassword(strPassword);
                            PolarbearMainActivity.setSettingStringValue(DBA.Field.WIFINAME, wifiname);
                            HILog.d(TAG, "onClick: wifiname = " + wifiname + " write into settings!");
                            PolarbearMainActivity.setSettingStringValue(DBA.Field.WIFIPASSWORD, strPassword);
                            WifiSelectedEvent wifiSelectedEvent = new WifiSelectedEvent();
                            wifiSelectedEvent.wifiname = wifiname;
                            UltraflyModelApplication.getInstance().bus.post(wifiSelectedEvent);
 */
    }

    private void SetMessage(Context context, final String text){

        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView)tv_messages).setText(text);
                tv_messages.postInvalidate();
            }
        });

    }

    private Handler UdpEventHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            super.handleMessage(msg);

            HILog.d(TAG, "UdpEventHandler: mMessages = " + mMessages);
            WifiUdpEvent wifiUdpEvent = (WifiUdpEvent) msg.obj;
            String wifimessage = wifiUdpEvent.message;
            HILog.d(TAG, "UdpEventHandler: wifimessage = " + wifimessage);

            if(wifiUdpEvent.mDialog){
                dialog_showmessage(wifimessage);
            }
            StringBuilder str = new StringBuilder();
            HILog.d(TAG, "UdpEventHandler : str = " + str.toString());

            str.append(mMessages);
            HILog.d(TAG, "UdpEventHandler : str = " + str.toString());
            str.append(wifimessage);
            HILog.d(TAG, "UdpEventHandler : str = " + str.toString());
            str.append("\n");
            HILog.d(TAG, "UdpEventHandler : str = " + str.toString());
            HILog.d(TAG, "UdpEventHandler : mMessages = " + mMessages);
            mMessages = str.toString();
            HILog.d(TAG, "UdpEventHandler : str = " + str.toString());
//            tv_messages.setText(wifimessage);
            SetMessage(mActivity, mMessages);
            tv_messages.setText(mMessages);
            tv_messages.postInvalidate();
//            tv_messages.setText(mMessages);
            if(wifimessage.contains("Home Router")){
//                dialog_showmessage(mActivity.getString(R.string.wifi_reset));
                HILog.d(TAG, "UdpEventHandler: before setSsidAndPassword:");
                setSsidAndPassword(mActivity, mWifiName, mPassword);
                UltraflyModelApplication.getInstance().bus.post(new ResetCarDataEvent());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                btn_back.performClick();
            } else if (wifimessage.contains("mismatch")){
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                dialog_showmessage(mActivity.getString(R.string.wifi_ng));
            }

            if(wifiUdpEvent.mDialog&&md_diaog!=null){
                md_diaog.dismiss();
            }
        }
    };

    @Subscribe
    public void getWifiUdpEvent(WifiUdpEvent wifiUdpEvent) {
        HILog.d(TAG, "Subscribe : getWifiUdpEvent: mMessages = " + mMessages);

/*
        if(StringUtil.isStrNullOrEmpty(mMessages)){
            mMessages = wifimessage + "\n";
        } else {
            mMessages += wifimessage + "\n";
        }
*/
        HILog.d(TAG, "Subscribe : getWifiUdpEvent: mMessages = " + mMessages);
        Message udpMessage=new Message();
        udpMessage.obj = wifiUdpEvent;
        UdpEventHandler.sendMessage(udpMessage);


    }

    @Subscribe
    public void getUdpScanListEvent(UdpScanListEvent udpScanListEvent) {
        HILog.d(TAG, "Subscribe : getUdpScanListEvent: ");
        Message udpMessage=new Message();
        udpMessage.what = udpScanListEvent.iCommand;
        udpMessage.obj = udpScanListEvent;
        UdpScanListEventHandler.sendMessage(udpMessage);
        dialog_scanlist_dismiss();
    }

    @Subscribe
    public void getOnBackPresssedEvent(OnBackPresssedEvent onBackPresssedEvent) {
        HILog.d(TAG, "Subscribe: getOnBackPresssedEvent: ");
        getActivity().getSupportFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
    }

    private Handler UdpScanListEventHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            HILog.d(TAG, "UdpScanListEventHandler:");
            super.handleMessage(msg);
            int iCommand = msg.what;
            UdpScanListEvent udpScanListEvent = (UdpScanListEvent) msg.obj;
            int iResponse = udpScanListEvent.iResponse;
            int iLen = udpScanListEvent.iLen;
            byte[] byResponse = null;
            if (iLen > 0) byResponse = (byte[]) udpScanListEvent.byResponse;

            if(P2PNatProcess.SCAN_LIST==iCommand)
            {

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

                m_WifiStringList.add(new String(byData));
                mActivity.runOnUiThread(new Runnable() {
                    public void run()
                    {
                        String strClientWifi = m_WifiStringList.get(0);
                        HILog.d(TAG, "UdpScanListEventHandler: Client ID = " + strClientWifi);

                        m_WifiStringList.remove(0);
                        WIfiInfo  wIfiInfo = new WIfiInfo();
                        String[]  strWifiInfo = strClientWifi.split(";");
                        if(strWifiInfo.length>2) {
                            wIfiInfo.strName = strWifiInfo[1].trim().split("=")[1].replace("\"","").trim();
                            wIfiInfo.iCH = Integer.valueOf(strWifiInfo[2].trim().split("=")[1].trim());
                            wIfiInfo.strDBm = strWifiInfo[3].trim().split("=")[1].trim();
                            wIfiInfo.strSecurity = strWifiInfo[4].trim().split("=")[1];
                            m_WifiInfoList.add(wIfiInfo);

                            ActiveAndroid.beginTransaction();
                            WifiList wifiList = new WifiList();
                            wifiList.wifi_name = wIfiInfo.strName;
                            wifiList.wifi_channel = wIfiInfo.iCH;
                            wifiList.wifi_dbm = wIfiInfo.strDBm;
                            wifiList.wifi_security = wIfiInfo.strSecurity;
                            wifiList.save();

                            ActiveAndroid.setTransactionSuccessful();
                            ActiveAndroid.endTransaction();
                            CommonUtils.CopyDBFromData2SD(getActivity());
                            UltraflyModelApplication.getInstance().bus.post(new WifiListUpdate());
                        }
                    }
                });
            }
        }
    };
}