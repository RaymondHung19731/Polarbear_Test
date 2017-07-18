package com.ultraflymodel.polarbear.fragment;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Cache;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.squareup.otto.Subscribe;
import com.ultraflymodel.polarbear.R;
import com.ultraflymodel.polarbear.activity.PolarbearMainActivity;
import com.ultraflymodel.polarbear.adapter.MusicListViewCursorAdapter;
import com.ultraflymodel.polarbear.common.Constants;
import com.ultraflymodel.polarbear.common.DBA;
import com.ultraflymodel.polarbear.common.HILog;
import com.ultraflymodel.polarbear.eventbus.EndMp3Event;
import com.ultraflymodel.polarbear.eventbus.MusicListClicked;
import com.ultraflymodel.polarbear.eventbus.MusicListUpdate;
import com.ultraflymodel.polarbear.eventbus.PlayItemSwiped;
import com.ultraflymodel.polarbear.eventbus.PlayListUpdate;
import com.ultraflymodel.polarbear.eventbus.PlayMp3Event;
import com.ultraflymodel.polarbear.eventbus.StopMp3Event;
import com.ultraflymodel.polarbear.eventbus.WakeupItemSwiped;
import com.ultraflymodel.polarbear.eventbus.WakeupListUpdate;
import com.ultraflymodel.polarbear.mike.UDPNetwork;
import com.ultraflymodel.polarbear.model.MusicList;
import com.ultraflymodel.polarbear.ultraflymodel.UltraflyModelApplication;
import com.ultraflymodel.polarbear.utils.CommonUtils;
import com.ultraflymodel.polarbear.utils.StringUtil;

import java.util.List;


public class PlayListFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = PlayListFragment.class.getSimpleName();

    private AbsListView mPlayListView;
    private Cursor mPlayListCursor=null;
    private Cursor mWakeupListCursor=null;
    private MusicListViewCursorAdapter playAdapter;
    private int mSelected = Constants.NONE;
    private static int mPlayToSendSize = 0;
    private UDPNetwork m_udpNetwork;
    private boolean mFlagSend2Play = false;
    private static int mPosition = -1;

    public Handler m_OneSecondHandler = new Handler();
    private Runnable m_OneSecondTimeoutTask = new Runnable() {
        public void run()
        {
            showPlayListView();
            UltraflyModelApplication.getInstance().bus.post(new MusicListUpdate());
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mainview = inflater.inflate(R.layout.layout_playlist, container, false);
        HILog.d(TAG, "onCreateView:");
        Bundle bundle = getArguments();
        mSelected = bundle.getInt(Constants.BC_BUNDLE_SELECTED);
        HILog.d(TAG, "onCreateView: mSelected: " + mSelected);
        m_udpNetwork = PolarbearMainActivity.m_udpNetwork;

        mPlayListView = (ListView) mainview.findViewById(R.id.lv_play_list);
        mPlayListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mPlayListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HILog.d(TAG, "mPlayListView: position = " + position);
                mFlagSend2Play = !mFlagSend2Play;
                HILog.d(TAG, "mPlayListView: mFlagSend2Play = " + mFlagSend2Play);
                ClearNowSending();
                if(mFlagSend2Play) {
                    HILog.d(TAG, "mPlayListView: Start SendToPlayMp3.");
                    SendToPlayMp3();
                } else {
                    StopMp3Event stopMp3Event = new StopMp3Event();
                    stopMp3Event.mp3path = null;
                    stopMp3Event.remote = true;
                    UltraflyModelApplication.getInstance().bus.post(stopMp3Event);
                }
            }
        });
        UltraflyModelApplication.getInstance().bus.register(this);
        return mainview;
    }

    @SuppressLint("NewApi")
    private void showPlayListView() {
        HILog.d(TAG, "showPlayListView");
        int lastViewedPosition = mPlayListView.getFirstVisiblePosition();
        View v = mPlayListView.getChildAt(0);
        int topOffset = (v == null) ? 0 : v.getTop();

        mPlayListCursor = getPlayListCursor();
        if(mPlayListCursor==null)return;
        int size = mPlayListCursor.getCount();
        HILog.d(TAG, "onCreateView : mPlayListCursor.size = " + size);
        if(mSelected==Constants.PLAYLIST){
            HILog.d(TAG, "showPlayListView: PLAYSELECTED!");
            playAdapter = new MusicListViewCursorAdapter(getActivity(), mPlayListCursor, Constants.PLAYLIST);
        } else {
            HILog.d(TAG, "showPlayListView: WAKEUPLIST!");
            playAdapter = new MusicListViewCursorAdapter(getActivity(), mPlayListCursor, Constants.WAKEUPLIST);
        }

        mPlayListView.setAdapter(playAdapter);
//        mPlayListView.setSelectionFromTop(lastViewedPosition, topOffset);
        ClearNowSending();
    }

    @Override
    public void onStart() {
        HILog.d(TAG, "onStart : ");
        super.onStart();
        showPlayListView();
    }

    private Cursor getPlayListCursor(){
        String whereclause = null;
        Cursor resultCursor=null;
        HILog.d(TAG, "getPlayListCursor:");
        Select select = new Select();
        if(mSelected==Constants.PLAYLIST){
            HILog.d(TAG, "getPlayListCursor: PLAYSELECTED!");
            whereclause = DBA.Field.PLAYSELECTED ;
        } else {
            HILog.d(TAG, "getPlayListCursor: WAKEUPSELECTED!");
            whereclause = DBA.Field.WAKEUPSELECTED ;
        }
        whereclause += " = " + StringUtil.addquote(String.valueOf(Constants.SWIPPED));

        String sqlcommand = select.from(MusicList.class)
                .where(whereclause)
                .toSql();
        HILog.d(TAG, "getPlayListCursor: sqlcommand = " + sqlcommand);
        resultCursor = Cache.openDatabase().rawQuery(sqlcommand, null);
        int count = resultCursor.getCount();
        HILog.d(TAG, "getPlayListCursor: count = " + resultCursor.getCount());
        if(mSelected==Constants.PLAYLIST && count>0){
            select = new Select();
            whereclause = DBA.Field.PLAYSELECTED + " = ?";
            List<MusicList> playlists = select.from(MusicList.class)
                    .where(whereclause, Constants.SWIPPED)
                    .execute();
            int size  = playlists.size();
            HILog.d(TAG, "getPlayListCursor: size = " + size);
        }
        return resultCursor;
    }

    @Subscribe
    public void getEndMp3Event(EndMp3Event endMp3Event) {
        HILog.d(TAG, "Subscribe : getEndMp3Event: mp3path = " + endMp3Event.mp3path);
        HILog.d(TAG, "Subscribe : getEndMp3Event: mPlayToSendSize = " + --mPlayToSendSize);
        if(mFlagSend2Play) {
            if(mPlayToSendSize<=0) {
                ClearNowSending();
//                setSendToPlayMp3();
            } else  {
                SetPlayListField(DBA.Field.NOWSENDING, mPosition++, Constants.CLEARED);
                SendToPlayMp3();
            }
        }

    }

    private void SendToPlayMp3(){
        HILog.d(TAG, "SendToPlayMp3:");
        String whereclause = null;
        Select select = new Select();
        whereclause = DBA.Field.PLAYSELECTED + " = " + StringUtil.addquote(String.valueOf(Constants.SWIPPED));
        List<MusicList> playtosendLists = select.from(MusicList.class).where(whereclause).execute();
        int size = playtosendLists.size();
        HILog.d(TAG, "SendToPlayMp3: mPlayToSendSize = " + size);
        if(size>0){
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
            showPlayListView();
            CommonUtils.CopyDBFromData2SD(getActivity());
            PlayMp3Event playMp3Event = new PlayMp3Event();
            playMp3Event.mp3path = strPath;
            playMp3Event.remote = true;
            UltraflyModelApplication.getInstance().bus.post(playMp3Event);
            HILog.d(TAG, "SendToPlayMp3: sent playMp3Event");
            scroll(mPosition);
        }
    }

    private void scroll(int position) {
        mPlayListView.setSelection(position);
    }

    private void scroll() {
        int position = mPlayListView.getCount() - mPlayToSendSize;
        HILog.d(TAG, "scroll: MP3 playlist position = " + position);
//        mPlayListView.setSelection(position);
        mPlayListView.smoothScrollToPosition(position);
    }

    private void ClearNowSending(){
        HILog.d(TAG, "ClearNowSending:");
        mPosition = 0;
        if(mPlayListCursor!=null) mPlayToSendSize = mPlayListCursor.getCount();
        SearchMusicListFragment.ClearMusicListField(DBA.Field.NOWSENDING);
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

        if(mSelected==Constants.PLAYLIST){
            whereclause = DBA.Field.PLAYSELECTED ;
        } else {
            whereclause = DBA.Field.WAKEUPSELECTED ;
        }
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


    public void setPlaySelectedColumn(int position, int value){
        String whereclause = null;
        HILog.d(TAG, "setPlaySelectedColumn: position: " + position + ", value = " + value);
        Select select = new Select();
        if(mSelected==Constants.PLAYLIST){
            HILog.d(TAG, "setPlaySelectedColumn: PLAYSELECTED!");
            whereclause = DBA.Field.PLAYSELECTED ;
        } else {
            HILog.d(TAG, "setPlaySelectedColumn: WAKEUPSELECTED!");
            whereclause = DBA.Field.WAKEUPSELECTED ;
        }
        whereclause += " = ?";
        List<MusicList> musicLists = select.from(MusicList.class)
                .where(whereclause, Constants.SWIPPED)
                .execute();
        ActiveAndroid.beginTransaction();
        HILog.d(TAG, "setPlaySelectedColumn: swipped title = " + musicLists.get(position).title);
        if(mSelected==Constants.PLAYLIST){
            musicLists.get(position).play_selected = value;
        } else {
            musicLists.get(position).wakeup_selected = value;
        }
        musicLists.get(position).save();
        ActiveAndroid.setTransactionSuccessful();
        ActiveAndroid.endTransaction();
        CommonUtils.CopyDBFromData2SD(getActivity());
    }

    private Handler PlayItemSwipedHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            HILog.d(TAG, "PlayItemSwipedHandler:");
            int pos = (int) msg.obj;
            SetPlayListField(DBA.Field.PLAYSELECTED, pos, Constants.CLEARED);
            m_OneSecondHandler.postDelayed(m_OneSecondTimeoutTask, Constants.SWIPEWAIT);
        }
    };

    private Handler MusicListClickHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            HILog.d(TAG, "MusicListClickHandler:");

        }
    };

    @Subscribe
    public void getPlayItemSwiped(PlayItemSwiped playItemSwiped) {
        HILog.d(TAG, "Subscribe : getPlayItemSwiped: ");
        Message myMessage=new Message();
        myMessage.obj = playItemSwiped.position;
        PlayItemSwipedHandler.sendMessage(myMessage);
    }

    @Subscribe
    public void getWakeupItemSwiped(WakeupItemSwiped wakeupItemSwiped) {
        HILog.d(TAG, "Subscribe : getWakeupItemSwiped: ");
        Message myMessage=new Message();
        myMessage.obj = wakeupItemSwiped.position;
        PlayItemSwipedHandler.sendMessage(myMessage);
    }

    @Subscribe
    public void getMusicListClicked(MusicListClicked musicListClicked) {
        HILog.d(TAG, "Subscribe : getMusicListClicked: ");
    }

    @Subscribe
    public void getPlayListUpdate(PlayListUpdate playListUpdate) {
        HILog.d(TAG, "Subscribe : getPlayListUpdate: ");
        showPlayListView();
        if(mPlayToSendSize==0){
//            setSendToPlayMp3();
//            SendToPlayMp3();
        }
    }

    @Subscribe
    public void getWakeupListUpdate(WakeupListUpdate wakeupListUpdate) {
        HILog.d(TAG, "Subscribe : getWakeupListUpdate: ");
        showPlayListView();
    }

    @Override
    public void onDestroyView() {
        HILog.d(TAG, "onDestroyView:");
        super.onDestroyView();
        ClearNowSending();
        UltraflyModelApplication.getInstance().bus.unregister(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        }
    }

}