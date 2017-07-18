package com.ultraflymodel.polarbear.fragment;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ultraflymodel.polarbear.R;
import com.ultraflymodel.polarbear.activity.PolarbearMainActivity;
import com.ultraflymodel.polarbear.common.Constants;
import com.ultraflymodel.polarbear.common.HILog;
import com.ultraflymodel.polarbear.eventbus.MusicListClicked;
import com.ultraflymodel.polarbear.eventbus.OnBackPresssedEvent;
import com.ultraflymodel.polarbear.eventbus.StopMp3Event;
import com.ultraflymodel.polarbear.ultraflymodel.UltraflyModelApplication;
import com.ultraflymodel.polarbear.utils.CommonUtils;


public class MusicListFragment extends Fragment implements View.OnClickListener, View.OnLongClickListener {
    private static final String TAG = MusicListFragment.class.getSimpleName();
    private TextView tv_play_list, tv_music_smart_phone;
    private Activity mActivity;
    private static FrameLayout fl_playlist_container, fl_musiclist_container, fl_wakeuplist_container;
    private Button btn_musiclist, btn_back, btn_menu, btn_wake_up_small;
    private SearchMusicListFragment searchMusicListFragment=null;
    private PlayListFragment playListFragment=null;
    private LinearLayout ll_wakeup_list, ll_play_list;
    private boolean mBtnMenuFlag, mBtnWakeupSmallFlag;
    private int mSelected = Constants.NONE;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mainview = inflater.inflate(R.layout.layout_musiclist, container, false);
        HILog.d(TAG, "onCreateView:");
        mActivity = getActivity();
        PolarbearMainActivity.mNotInFront = true;
        tv_music_smart_phone = (TextView) mainview.findViewById(R.id.tv_music_smart_phone);
        tv_music_smart_phone.setTypeface(CommonUtils.getGothicFont(mActivity), Typeface.BOLD);
        tv_play_list = (TextView) mainview.findViewById(R.id.tv_play_list);
        tv_play_list.setTypeface(CommonUtils.getGothicFont(mActivity), Typeface.BOLD);
        btn_musiclist = (Button) mainview.findViewById(R.id.btn_musiclist);
        btn_musiclist.setOnClickListener(this);
        btn_back = (Button) mainview.findViewById(R.id.btn_back);
        btn_back.setOnClickListener(this);
        btn_musiclist.setOnLongClickListener(this);
        btn_menu = (Button) mainview.findViewById(R.id.btn_menu);
        btn_menu.setOnClickListener(this);
        btn_menu.setOnLongClickListener(this);
        btn_wake_up_small = (Button) mainview.findViewById(R.id.btn_wake_up_small);
        btn_wake_up_small.setOnClickListener(this);
        btn_wake_up_small.setOnLongClickListener(this);
        fl_musiclist_container = (FrameLayout) mainview.findViewById(R.id.fl_musiclist_container);
        fl_playlist_container = (FrameLayout) mainview.findViewById(R.id.fl_playlist_container);
        fl_wakeuplist_container = (FrameLayout) mainview.findViewById(R.id.fl_wakeuplist_container);
        ll_wakeup_list = (LinearLayout) mainview.findViewById(R.id.ll_wakeup_list);
        ll_play_list = (LinearLayout) mainview.findViewById(R.id.ll_play_list);
        btn_menu.performClick();
        return mainview;
    }

    @Override
    public void onDestroyView() {
        HILog.d(TAG, "onDestroyView:");
        PolarbearMainActivity.mNotInFront = false;

        if(searchMusicListFragment!=null) {
            getChildFragmentManager().beginTransaction().remove(searchMusicListFragment).commitAllowingStateLoss();
            searchMusicListFragment = null;
        }
        if(playListFragment!=null) {
            getChildFragmentManager().beginTransaction().remove(playListFragment).commitAllowingStateLoss();
            playListFragment = null;
        }

        HILog.d(TAG, "onDestroyView: End.");
        super.onDestroyView();
    }

    private void StopMP3(){
        StopMp3Event stopMp3Event = new StopMp3Event();
        stopMp3Event.mp3path = null;
        stopMp3Event.remote = false;
        UltraflyModelApplication.getInstance().bus.post(stopMp3Event);
    }

    @Override
    public void onClick(View v) {
        Bundle bundle = new Bundle();
        StopMP3();
        switch (v.getId()) {
                case R.id.btn_musiclist:
                HILog.d(TAG, "onClick: btn_musiclist:");
                UltraflyModelApplication.getInstance().bus.post(new MusicListClicked());
                break;
            case R.id.btn_back:
                HILog.d(TAG, "onClick: btn_back:");
                int count = mActivity.getFragmentManager().getBackStackEntryCount();
                HILog.d(TAG, "onClick : btn_back: count = " + count);
//                getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
                UltraflyModelApplication.getInstance().bus.post(new OnBackPresssedEvent());
                break;
            case R.id.btn_menu:
                HILog.d(TAG, "onClick: btn_menu: mBtnMenuFlag = " + mBtnMenuFlag);
                if(mBtnMenuFlag) break;
                mBtnMenuFlag = !mBtnMenuFlag;
                mBtnWakeupSmallFlag = !mBtnMenuFlag;
                tv_play_list.setText(R.string.play_list);
                mSelected = Constants.PLAYLIST;
                bundle.putInt(Constants.BC_BUNDLE_SELECTED, Constants.PLAYLIST);
                RefreshThreeLists(bundle);
                ll_wakeup_list.setVisibility(View.INVISIBLE);
                ll_play_list.setVisibility(View.VISIBLE);
                HILog.d(TAG, "onClick: btn_menu: 2: mBtnMenuFlag = " + mBtnMenuFlag);
                break;
            case R.id.btn_wake_up_small:
                HILog.d(TAG, "onClick: btn_wake_up_small:");
                if(mBtnWakeupSmallFlag) break;
                mBtnWakeupSmallFlag = !mBtnWakeupSmallFlag;
                mBtnMenuFlag = !mBtnWakeupSmallFlag;
                tv_play_list.setText(R.string.baby_wakeup_music);
                mSelected = Constants.WAKEUPLIST;
                bundle.putInt(Constants.BC_BUNDLE_SELECTED, Constants.WAKEUP);
                RefreshThreeLists(bundle);
                ll_wakeup_list.setVisibility(View.VISIBLE);
                ll_play_list.setVisibility(View.INVISIBLE);
                break;
        }
        show2Buttons();
    }

    private void RefreshThreeLists(Bundle bundle){
        HILog.d(TAG, "RefreshThreeLists: mSelected = " + mSelected);
        if(searchMusicListFragment!=null)
            getChildFragmentManager().beginTransaction().remove(searchMusicListFragment).commitAllowingStateLoss();
        searchMusicListFragment = new SearchMusicListFragment();
        searchMusicListFragment.setArguments(bundle);
        getChildFragmentManager().beginTransaction().replace(fl_musiclist_container.getId(), searchMusicListFragment).commit();

        if(playListFragment!=null)
            getChildFragmentManager().beginTransaction().remove(playListFragment).commitAllowingStateLoss();
        playListFragment = new PlayListFragment();
        playListFragment.setArguments(bundle);
        if(mSelected==Constants.PLAYLIST){
            HILog.d(TAG, "RefreshThreeLists: PLAYLIST!");
            getChildFragmentManager().beginTransaction().replace(fl_playlist_container.getId(), playListFragment).commit();
        } else if (mSelected==Constants.WAKEUPLIST){
            HILog.d(TAG, "RefreshThreeLists: WAKEUPLIST!");
            getChildFragmentManager().beginTransaction().replace(fl_wakeuplist_container.getId(), playListFragment).commit();
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

    private void show2Buttons(){
        if(mBtnMenuFlag) btn_menu.setBackgroundResource(R.mipmap.ic_menu_active);
        else btn_menu.setBackgroundResource(R.mipmap.ic_menu_normal);
        if(mBtnWakeupSmallFlag) btn_wake_up_small.setBackgroundResource(R.mipmap.ic_wake_up_active);
        else btn_wake_up_small.setBackgroundResource(R.mipmap.ic_wake_up_normal);
    }
}