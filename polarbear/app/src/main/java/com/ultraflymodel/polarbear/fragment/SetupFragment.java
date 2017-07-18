package com.ultraflymodel.polarbear.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.otto.Subscribe;
import com.ultraflymodel.polarbear.R;
import com.ultraflymodel.polarbear.activity.BaseActivity;
import com.ultraflymodel.polarbear.common.Constants;
import com.ultraflymodel.polarbear.common.HILog;
import com.ultraflymodel.polarbear.eventbus.OnBackPresssedEvent;
import com.ultraflymodel.polarbear.ultraflymodel.UltraflyModelApplication;


public class SetupFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = SetupFragment.class.getSimpleName();
    ViewPager mPager;
    SlidePagerAdapter mPagerAdapter;
    static final int NUM_ITEMS = 2;
    private Activity mActivity;
    private MusicListFragment musicListFragment=null;
    private ScanWifiListFragment scanWifiListFragment=null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mainview = inflater.inflate(R.layout.layout_setup, container, false);
        HILog.d(TAG, "onCreateView:");
        mActivity = getActivity();
        musicListFragment = new MusicListFragment();
        scanWifiListFragment = new ScanWifiListFragment();

        mPager = (ViewPager) mainview.findViewById(R.id.pager);
        mPagerAdapter = new SlidePagerAdapter(BaseActivity.fragmentManager);
        mPager.setAdapter(mPagerAdapter);

        UltraflyModelApplication.getInstance().bus.register(this);

        return mainview;
	}

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        }
    }

    @Override
    public void onResume() {
        HILog.d(TAG, "onResume:");
        super.onResume();
    }

    @Subscribe
    public void getOnBackPresssedEvent(OnBackPresssedEvent onBackPresssedEvent) {
        HILog.d(TAG, "Subscribe: SetupFragment: getOnBackPresssedEvent: ");
        mPager.removeAllViews();
        mPagerAdapter.notifyDataSetChanged();
        getActivity().getSupportFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
        getActivity().getSupportFragmentManager().beginTransaction().remove(musicListFragment).commitAllowingStateLoss();
        getActivity().getSupportFragmentManager().beginTransaction().remove(scanWifiListFragment).commitAllowingStateLoss();
    }

    @Override
    public void onDestroyView() {
        HILog.d(TAG, "onDestroyView:");
        PolarbearMainFragment polarbearMainFragment = new PolarbearMainFragment();
        BaseActivity.loadFragment(polarbearMainFragment, Constants.JumpTo.PMAIN.toString(), false);
/*
        if(scanWifiListFragment!=null) {
            getChildFragmentManager().beginTransaction().remove(scanWifiListFragment).commitAllowingStateLoss();
            scanWifiListFragment = null;
        }

        if(musicListFragment!=null){
            getChildFragmentManager().beginTransaction().remove(musicListFragment).commitAllowingStateLoss();
            musicListFragment = null;
        }
*/
        UltraflyModelApplication.getInstance().bus.unregister(this);
        super.onDestroyView();
    }

    /* PagerAdapter class */
    public class SlidePagerAdapter extends FragmentPagerAdapter {
        FragmentManager fragmentManager;
        public SlidePagerAdapter(FragmentManager fm) {
            super(fm);
            HILog.d(TAG, "SlidePagerAdapter:");
            this.fragmentManager = fm;
        }

        @Override
        public Fragment getItem(int position) {
            HILog.d(TAG, "getItem: position = " + position);
			/*
			 * IMPORTANT: This is the point. We create a RootFragment acting as
			 * a container for other fragments
			 */
            if (position == 0)
                return musicListFragment;
            else
                return scanWifiListFragment;
        }

        @Override
        public int getCount() {
            HILog.d(TAG, "getCount:");
            return NUM_ITEMS;
        }

        @Override
        public int getItemPosition(Object object){
            HILog.d(TAG, "getItemPosition:");
            return PagerAdapter.POSITION_NONE;
        }
    }
}