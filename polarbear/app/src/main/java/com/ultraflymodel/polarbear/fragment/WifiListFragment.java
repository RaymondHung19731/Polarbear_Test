package com.ultraflymodel.polarbear.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.activeandroid.Cache;
import com.activeandroid.query.Select;
import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.otto.Subscribe;
import com.ultraflymodel.polarbear.R;
import com.ultraflymodel.polarbear.activity.PolarbearMainActivity;
import com.ultraflymodel.polarbear.adapter.MusicListViewCursorAdapter;
import com.ultraflymodel.polarbear.common.Constants;
import com.ultraflymodel.polarbear.common.DBA;
import com.ultraflymodel.polarbear.common.HILog;
import com.ultraflymodel.polarbear.eventbus.WifiListReset;
import com.ultraflymodel.polarbear.eventbus.WifiListUpdate;
import com.ultraflymodel.polarbear.eventbus.WifiSelectedEvent;
import com.ultraflymodel.polarbear.mike.UDPNetwork;
import com.ultraflymodel.polarbear.model.WifiList;
import com.ultraflymodel.polarbear.ultraflymodel.UltraflyModelApplication;
import com.ultraflymodel.polarbear.utils.StringUtil;


public class WifiListFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = WifiListFragment.class.getSimpleName();

    private AbsListView mWifiListView;
    private Cursor mWifiListCursor=null;
    private MusicListViewCursorAdapter wifiAdapter;
    private static UDPNetwork m_udpNetwork;
    private static Activity m_ctx;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mainview = inflater.inflate(R.layout.layout_playlist, container, false);
        HILog.d(TAG, "onCreateView:");
        m_udpNetwork = PolarbearMainActivity.m_udpNetwork;
        m_ctx = getActivity();

        mWifiListView = (ListView) mainview.findViewById(R.id.lv_play_list);
        mWifiListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mWifiListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mWifiListCursor.moveToPosition(position);
                String wifi_name = mWifiListCursor.getString(mWifiListCursor.getColumnIndex(DBA.Field.WIFINAME));
                HILog.d(TAG, "mWifiListView: position = " + position + ", wifi_name = " + wifi_name);
//                dialog_password(wifi_name);
                WifiSelectedEvent wifiSelectedEvent = new WifiSelectedEvent();
                wifiSelectedEvent.wifiname = wifi_name;
                UltraflyModelApplication.getInstance().bus.post(wifiSelectedEvent);
            }
        });
        showWifiListView();
        UltraflyModelApplication.getInstance().bus.register(this);
        return mainview;
    }

    @SuppressLint("NewApi")
    private void showWifiListView() {
        HILog.d(TAG, "showWifiListView");

        mWifiListCursor = getWifiListCursor();
        if(mWifiListCursor==null)return;
        int size = mWifiListCursor.getCount();
        HILog.d(TAG, "onCreateView : mWifiListCursor.size = " + size);
        wifiAdapter = new MusicListViewCursorAdapter(getActivity(), mWifiListCursor, Constants.WIFILIST);
        mWifiListView.setAdapter(wifiAdapter);
    }

    @Override
    public void onStart() {
        HILog.d(TAG, "onStart : ");
        super.onStart();
    }

    private Cursor getWifiListCursor(){
        Cursor resultCursor=null;
        HILog.d(TAG, "getWifiListCursor:");
        Select select = new Select();
        String sqlcommand = select.from(WifiList.class)
                .orderBy("wifi_dbm ASC")
                .toSql();
        HILog.d(TAG, "getWifiListCursor: sqlcommand = " + sqlcommand);
        resultCursor = Cache.openDatabase().rawQuery(sqlcommand, null);
        HILog.d(TAG, "getWifiListCursor: count = " + resultCursor.getCount());
        return resultCursor;
    }



    @Override
    public void onDestroyView() {
        HILog.d(TAG, "onDestroyView:");
        super.onDestroyView();
        UltraflyModelApplication.getInstance().bus.unregister(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        }
    }

    public static void dialog_password(final String wifiname){
        HILog.d(TAG, "dialog_password:");
        MaterialDialog dialog = new MaterialDialog.Builder(m_ctx)
                .titleColorRes(R.color.bc_text_fg)
                .positiveColorRes(R.color.bc_text_fg)
                .widgetColorRes(R.color.md_divider_white)
                .title(wifiname)
                .content(R.string.password)
                .positiveText(R.string.ok)
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                .input(null, null, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        // Do something
                        HILog.d(TAG, "onClick: btn_setting: onInput = " + input);
                        String strPassword = String.valueOf(input).trim();
                        HILog.d(TAG, "onClick: btn_setting: strPassword = " + strPassword);
                        if(!StringUtil.isStrNullOrEmpty(strPassword)){
                            m_udpNetwork.ChangeClientSSID(wifiname);
                            m_udpNetwork.ChangeClientPassword(strPassword);
                            PolarbearMainActivity.setSettingStringValue(DBA.Field.WIFINAME, wifiname);
                            HILog.d(TAG, "onClick: wifiname = " + wifiname + " write into settings!");
                            PolarbearMainActivity.setSettingStringValue(DBA.Field.WIFIPASSWORD, strPassword);
                            WifiSelectedEvent wifiSelectedEvent = new WifiSelectedEvent();
                            wifiSelectedEvent.wifiname = wifiname;
                            UltraflyModelApplication.getInstance().bus.post(wifiSelectedEvent);
                        } else {
                            HILog.d(TAG, "onClick: strPassword is null!");
                        }
                    }
                }).show();
    }



    @Subscribe
    public void getWifiListUpdate(WifiListUpdate wifiListUpdate) {
        HILog.d(TAG, "Subscribe : getWifiListUpdate: ");
        showWifiListView();
    }

    @Subscribe
    public void WifiListReset(WifiListReset wifiListReset) {
        HILog.d(TAG, "Subscribe : WifiListReset: ");
        ScanWifiListFragment.DeleteWifiList();
        showWifiListView();
    }
}