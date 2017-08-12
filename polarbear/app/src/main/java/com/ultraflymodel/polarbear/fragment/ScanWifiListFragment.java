package com.ultraflymodel.polarbear.fragment;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;


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

    Boolean xmitStarted = false;
    xmitterTask xmitter;
 //   static HashMap<String, String> passhash = new HashMap<String, String>();
 //   static TextView TxtDebug;
 //   Button BtnStartStop;
    WifiInfo wifiInf;


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
//        mWifiName = PolarbearMainActivity.getSettingStringValue(DBA.Field.WIFINAME);
        mWifiName = PolarbearMainActivity.wm.getConnectionInfo().getSSID();
//        PolarbearMainActivity.wm.getConnectionInfo();
// set from WifiManager wifiMgr;
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

        checkWiFi();

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

    public static boolean setSsidAndPassword(Context context, String ssid, String ssidPassword) {
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
 //                   m_udpNetwork.ChangeClientSSID(mWifiName);
 //                   m_udpNetwork.ChangeClientPassword(mPassword);
                    onClick_action();
                }
                break;
            case R.id.btn_phonewifi:
                HILog.d(TAG, "onClick: btn_phonewifi:");
                mScanning = false;
//                mScanning = true;
//                mScanning = !mScanning;
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

//    @Override
    private   boolean validate(String pass, String key) {

        if ((pass.length() != 0) && (pass.length() < 8 || pass.length() > 63)) {
 //           TxtDebug.setText("Invalid passphrase. Passphrase must be 8 to 63 characters long.");
            mMessages = "Invalid passphrase. Passphrase must be 8 to 63 characters long.";
            tv_messages.clearComposingText();
            tv_messages.setText(mMessages);

            return false;
        }
        if (key.length() > 16 && key.length() < 8) {
  //          TxtDebug.setText("Invalid key. Key must be 8 to 16 characters long.");
            mMessages = "Invalid key. Key must be 8 to 16 characters long.";
            tv_messages.clearComposingText();
            tv_messages.setText(mMessages);

            return false;
        }
        return true;
    }

    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 42) {
                Log.d("MRVL", "ADI ASync task exited");
                xmitStarted = false;
    //            BtnStartStop.setText("Start");
    //            TxtDebug.setText("Please check indicators on the device.\n The device should have been provisioned.\n If not, please retry.");
                mMessages = "Please check indicators on the device.  The device should been connected to your home router.  If not, please retry.";
                tv_messages.clearComposingText();
                tv_messages.setText(mMessages);


            } else if (msg.what == 43) {
     //           TxtDebug.setText("Information sent " + msg.arg1 / 2 + " times.");
                mMessages = "Information sent " + msg.arg1 / 2 + " times.";
                tv_messages.clearComposingText();
                tv_messages.setText(mMessages);

            }
            super.handleMessage(msg);
        }
    };

    private static byte[] hexStringToByteArray(String s, int blockLen) {
        int len = s.length();
        byte[] data = new byte[blockLen];
        Arrays.fill(data, (byte)0);
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        Log.d("MRVL", data.toString());
        return data;
    }

    private boolean validateCustomData(String s)
    {
        if (s.length() % 2 == 1) {
            return false;
        }
        boolean isHex = s.matches("[0-9A-Fa-f]*");
        return isHex;
    }


    public static byte[] myEncryptPassphrase(String key, byte[] plainText, String ssid) {

        byte[] iv = new byte[16];
        for (int i = 0; i < 16; i++)
            iv[i] = 0;

        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher;
        byte[] encrypted = null;
        try {
            int iterationCount = 4096;
            int keyLength = 256;
            //int saltLength = keyLength / 8;
            byte salt[] = ssid.getBytes();

            Log.d("MRVL", "key salt itercount " + key + " " + ssid + " " + iterationCount);
            KeySpec keySpec = new PBEKeySpec(key.toCharArray(), salt, iterationCount, keyLength);

            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

            byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
            SecretKeySpec skeySpec = new SecretKeySpec(keyBytes, "AES");

            cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
            encrypted = cipher.doFinal(plainText);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BadPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return encrypted;
    }


    public static byte[] myEncryptCustomData(String key, byte[] plainText, String ssid) {

        byte[] iv = new byte[16];
        for (int i = 0; i < 16; i++)
            iv[i] = 0;

        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher;
        byte[] encrypted = null;
        try {
            int iterationCount = 4096;
            int keyLength = 256;
            //int saltLength = keyLength / 8;
            byte salt[] = ssid.getBytes();

            Log.d("MRVL", "key salt itercount " + key + " " + ssid + " " + iterationCount);
            KeySpec keySpec = new PBEKeySpec(key.toCharArray(), salt, iterationCount, keyLength);

            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

            byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
            SecretKeySpec skeySpec = new SecretKeySpec(keyBytes, "AES");

            cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
            encrypted = cipher.doFinal(plainText);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BadPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return encrypted;
    }



    private void onClick_action() {
        Log.d("MRVL", "Btn clicked");

        String pass = mPassword;
//		String key = EditDeviceKey.getText().toString();
//		String data = EditCustomData.getText().toString();
        String key = "device_key";
        String data = "";
        // validate data
 /*
        if (validateCustomData(data) == false) {
            TxtDebug.setText("Invalid custom data. Custom data must be hexadecimal string with even length.");
            return;
        }
*/
/*
        if (RememberPassphrase.isChecked()) {
            saveToPhoneMemory(EditSSID.getText().toString(), EditPass.getText()
                    .toString());
        } else {
            saveToPhoneMemory(EditSSID.getText().toString(), "");
        }
*/
        try {
            if (xmitStarted == false) {

                if (!validate(pass, key))
                    return;
                xmitter = new xmitterTask();
                xmitter.handler = handler;

                xmitStarted = true;
//                BtnStartStop.setText("Stop");

                CRC32 crc32 = new CRC32();
                crc32.reset();
                crc32.update(pass.getBytes());
                xmitter.passCRC = (int) crc32.getValue() & 0xffffffff;
                Log.d("MRVL", Integer.toHexString(xmitter.passCRC));

                xmitter.ssid = PolarbearMainActivity.wm.getConnectionInfo().getSSID();
                xmitter.ssidLen = PolarbearMainActivity.wm.getConnectionInfo().getSSID().length();

                xmitter.customDataLen = data.length() / 2;
                if (xmitter.customDataLen % 16 == 0) {
                    xmitter.cipherDataLen = xmitter.customDataLen;
                } else {
                    xmitter.cipherDataLen = ((xmitter.customDataLen / 16) + 1) * 16;
                }
                xmitter.customData = hexStringToByteArray(data, xmitter.cipherDataLen);

                CRC32 crc32_customdata = new CRC32();
                crc32_customdata.reset();
                crc32_customdata.update(xmitter.customData);
                xmitter.customDataCRC = (int) crc32_customdata.getValue() & 0xffffffff;
                Log.d("MRVL", "CRC is " + Integer.toHexString((xmitter.customDataCRC)));
                Log.d("MRVL", "Length is " + xmitter.customData.length);

                int deviceVersion = Build.VERSION.SDK_INT;

                if (deviceVersion >= 17) {
                    if (xmitter.ssid.startsWith("\"") && xmitter.ssid.endsWith("\"")) {
                        xmitter.ssidLen = PolarbearMainActivity.wm.getConnectionInfo().getSSID().length() - 2;
                        xmitter.ssid = xmitter.ssid.substring(1, xmitter.ssid.length() - 1);
                    }
                }
                Log.d("MRVL", "SSID LENGTH IS " + xmitter.ssidLen);
                CRC32 crc32_ssid = new CRC32();
                crc32_ssid.reset();
                crc32_ssid.update(xmitter.ssid.getBytes());
                xmitter.ssidCRC = (int) crc32_ssid.getValue() & 0xffffffff;

                if (key.length() != 0) {
                    if (pass.length() % 16 == 0) {
                        xmitter.passLen = pass.length();
                    } else {
                        xmitter.passLen = (16 - (pass.length() % 16))
                                + pass.length();
                    }

                    byte[] plainPass = new byte[xmitter.passLen];

                    for (int i = 0; i < pass.length(); i++)
                        plainPass[i] = pass.getBytes()[i];

                    xmitter.passphrase = myEncryptPassphrase(key, plainPass, xmitter.ssid);
                    xmitter.cipherData = myEncryptCustomData(key, xmitter.customData, xmitter.ssid);
                    Log.d("MRVL", "AmeyRocks" + xmitter.cipherDataLen + " " + xmitter.cipherData.length);
                } else {
                    xmitter.passphrase = pass.getBytes();
                    xmitter.passLen = pass.length();
                }
                wifiInf = PolarbearMainActivity.wm.getConnectionInfo();
                xmitter.mac = new char[6];
                xmitter.preamble = new char[6];
                String[] macParts = wifiInf.getBSSID().split(":");

                xmitter.preamble[0] = 0x45;
                xmitter.preamble[1] = 0x5a;
                xmitter.preamble[2] = 0x50;
                xmitter.preamble[3] = 0x52;
                xmitter.preamble[4] = 0x32;
                xmitter.preamble[5] = 0x32;

                Log.d("MRVL", wifiInf.getBSSID());
                for (int i = 0; i < 6; i++)
                    xmitter.mac[i] = (char) Integer.parseInt(macParts[i], 16);
                xmitter.resetStateMachine();
                xmitter.execute("");
            } else {
                xmitStarted = false;
//                BtnStartStop.setText("Start");
                xmitter.cancel(true);
            }
        } catch (Error err) {
            Log.e("MRVL", err.toString());
        }
    }

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

    public static void user_change_to_home_router() {
        md_diaog = new MaterialDialog.Builder(mActivity)
                .title(R.string.app_name)
                .content(mActivity.getString(R.string.change_to_home_router))
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .show();
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


    private class xmitterTask extends AsyncTask<String, Void, String> {
        byte[] passphrase;
        byte[] customData;
        byte[] cipherData;
        String ssid;
        char[] mac;
        char[] preamble;
        int passLen;
        int ssidLen;
        int customDataLen;
        int cipherDataLen;
        int passCRC;
        int ssidCRC;
        int customDataCRC;
        Handler handler;

        private int state, substate;

        public void resetStateMachine() {
            state = 0;
            substate = 0;
        }

        private void xmitRaw(int u, int m, int l) {
            MulticastSocket ms;
            InetAddress sessAddr;
            DatagramPacket dp;

            byte[] data = new byte[2];
            data = "a".getBytes();

            u = u & 0x7f; /* multicast's uppermost byte has only 7 chr */

            try {
				Log.d("MRVL", "239." + u + "." + m + "." + l);
                sessAddr = InetAddress.getByName("239." + u + "." + m + "." + l);
                ms = new MulticastSocket(1234);
                dp = new DatagramPacket(data, data.length, sessAddr, 5500);
                ms.send(dp);
                ms.close();
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
                Log.e("MRVL", "Exiting 5");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        private void xmitState0(int substate) {
            int i, j, k;

            // Frame-type for preamble is 0b11110<substate1><substate0>
            // i = <frame-type> | <substate> i.e. 0x78 | substate
            k = preamble[2 * substate];
            j = preamble[2 * substate + 1];
            i = substate | 0x78;

            xmitRaw(i, j, k);
        }

        private void xmitState1(int substate, int len) {
            // Frame-type for SSID is 0b10<5 substate bits>
            // u = <frame-type> | <substate> i.e. 0x40 | substate
            if (substate == 0) {
                int u = 0x40;
                xmitRaw(u, ssidLen, ssidLen);
            } else if (substate == 1 || substate == 2) {
                int k = (int) (ssidCRC >> ((2 * (substate - 1) + 0) * 8)) & 0xff;
                int j = (int) (ssidCRC >> ((2 * (substate - 1) + 1) * 8)) & 0xff;
                int i = substate | 0x40;
                xmitRaw(i, j, k);
            } else {
                int u = 0x40 | substate;
                int l = (0xff & ssid.getBytes()[(2 * (substate - 3))]);
                int m;
                if (len == 2)
                    m = (0xff & ssid.getBytes()[(2 * (substate - 3)) + 1]);
                else
                    m = 0;
                xmitRaw(u, m, l);
            }
        }

        private void xmitState2(int substate, int len) {
            // Frame-type for Passphrase is 0b0<6 substate bits>
            // u = <frame-type> | <substate> i.e. 0x00 | substate
            if (substate == 0) {
                int u = 0x00;
                xmitRaw(u, passLen, passLen);
            } else if (substate == 1 || substate == 2) {
                int k = (int) (passCRC >> ((2 * (substate - 1) + 0) * 8)) & 0xff;
                int j = (int) (passCRC >> ((2 * (substate - 1) + 1) * 8)) & 0xff;
                int i = substate;
                xmitRaw(i, j, k);
            } else {
                int u = substate;
                int l = (0xff & passphrase[(2 * (substate - 3))]);
                int m;
                if (len == 2)
                    m = (0xff & passphrase[(2 * (substate - 3)) + 1]);
                else
                    m = 0;
                xmitRaw(u, m, l);
            }
        }

        private void xmitState3(int substate, int len) {
            if (substate == 0) {
                int u = 0x60;
                xmitRaw(u, customDataLen, customDataLen);
            } else if (substate == 1 || substate == 2) {
                int k = (int) (customDataCRC >> ((2 * (substate - 1) + 0) * 8)) & 0xff;
                int j = (int) (customDataCRC >> ((2 * (substate - 1) + 1) * 8)) & 0xff;
                int i = substate | 0x60;
                xmitRaw(i, j, k);
            } else {
                int u = 0x60 | substate;
                int l = (0xff & cipherData[(2 * (substate - 3))]);
                int m;
                if (len == 2)
                    m = (0xff & cipherData[(2 * (substate - 3)) + 1]);
                else
                    m = 0;
                xmitRaw(u, m, l);
            }
        }

        private void stateMachine() {
            switch (state) {
                case 0:
                    if (substate == 3) {
                        state = 1;
                        substate = 0;
                    } else {
                        xmitState0(substate);
                        substate++;
                    }
                    break;
                case 1:
                    xmitState1(substate, 2);
                    substate++;
                    if (ssidLen % 2 == 1) {
                        if (substate * 2 == ssidLen + 5) {
                            xmitState1(substate, 1);
                            state = 2;
                            substate = 0;
                        }
                    } else {
                        if ((substate - 1) * 2 == (ssidLen + 4)) {
                            state = 2;
                            substate = 0;
                        }
                    }
                    break;
                case 2:
                    xmitState2(substate, 2);
                    substate++;
                    if (passLen % 2 == 1) {
                        if (substate * 2 == passLen + 5) {
                            xmitState2(substate, 1);
                            state = 3;
                            substate = 0;
                        }
                    } else {
                        if ((substate - 1) * 2 == (passLen + 4)) {
                            state = 3;
                            substate = 0;
                        }
                    }
                    break;
                case 3:
                    xmitState3(substate, 2);
                    substate++;
                    if (cipherDataLen % 2 == 1) {
                        if (substate * 2 == cipherDataLen + 5) {
                            xmitState3(substate, 1);
                            state = 0;
                            substate = 0;
                        }
                    } else {
                        if ((substate - 1) * 2 == cipherDataLen + 4) {
                            state = 0;
                            substate = 0;
                        }
                    }
                    break;
                default:
                    Log.e("MRVL", "I shouldn't be here");
            }
        }

        protected String doInBackground(String... params) {
 //           WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            MulticastLock mcastLock = PolarbearMainActivity.wm.createMulticastLock("mcastlock");
            mcastLock.acquire();

            int i = 0;

            while (true) {
                if (state == 0 && substate == 0)
                    i++;

                if (i % 5 == 0) {
                    Message msg = handler.obtainMessage();
                    msg.what = 43;
                    msg.arg1 = i;
                    handler.sendMessage(msg);
                }

				/* Stop trying after doing 50 iterations. Let user retry. */
                if (i >= 600)
                    break;

                if (isCancelled())
                    break;

                stateMachine();

//				try {
//					Thread.sleep(10);
//				} catch (InterruptedException e) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
//					break;
//				}
            }

            mcastLock.release();

            if (i >= 50) {
                Message msg = handler.obtainMessage();
                msg.what = 42;
                handler.sendMessage(msg);
            }
            return null;
        }


        @Override
        protected void onPostExecute(String result) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
    private int checkWiFi() {
//        wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiInf = PolarbearMainActivity.wm.getConnectionInfo();
        Log.d("MRVL", "In checkWiFi");
        try {
            if (PolarbearMainActivity.wm.isWifiEnabled() == false) {
                mMessages = "WiFi not enabled. Please enable WiFi and connect to the home network.\n";
                tv_messages.clearComposingText();
                tv_messages.setText(mMessages);
 //               TxtDebug.setText("WiFi not enabled. Please enable WiFi and connect to the home network.");
                return -1;
            } else if (PolarbearMainActivity.wm.getConnectionInfo().getSSID().length() == 0) {
 //               TxtDebug.setText("WiFi is enabled but device is not connected to any network.");
                mMessages = "WiFi is enabled but device is not connected to any network.\n";
                tv_messages.clearComposingText();
                tv_messages.setText(mMessages);

                return -1;
            }
        } catch (NullPointerException e) {
  //          TxtDebug.setText("WiFi is enabled but device is not connected to any network.");
            mMessages = "WiFi is enabled but device is not connected to any network.\n";
            tv_messages.clearComposingText();
            tv_messages.setText(mMessages);

        }

   //     EditSSID.setText(wifiMgr.getConnectionInfo().getSSID());
    //    EditPass.setText(getPassphrase(EditSSID.getText().toString()));
     //   BtnStartStop.setEnabled(true);
        return 0;
    }
}