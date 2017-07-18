package com.ultraflymodel.polarbear.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.squareup.otto.Subscribe;
import com.ultraflymodel.polarbear.R;
import com.ultraflymodel.polarbear.activity.BaseActivity;
import com.ultraflymodel.polarbear.common.Constants;
import com.ultraflymodel.polarbear.common.HILog;
import com.ultraflymodel.polarbear.eventbus.OnBackPresssedEvent;
import com.ultraflymodel.polarbear.ultraflymodel.UltraflyModelApplication;


public class AboutFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = AboutFragment.class.getSimpleName();
	private int mTitle;
    private Button mUla, mPrivacy;
    private TextView tv_myname;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mainview = inflater.inflate(R.layout.layout_about, container, false);
        HILog.d(TAG, "onCreateView:");
        UltraflyModelApplication.getInstance().bus.register(this);
        return mainview;
	}

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        }
    }

    @Override
    public void onDestroyView() {
        HILog.d(TAG, "onDestroyView:");
        UltraflyModelApplication.getInstance().bus.unregister(this);
        PolarbearMainFragment polarbearMainFragment = new PolarbearMainFragment();
        BaseActivity.loadFragment(polarbearMainFragment, Constants.JumpTo.PMAIN.toString(), false);
        super.onDestroyView();
    }

    @Subscribe
    public void getOnBackPresssedEvent(OnBackPresssedEvent onBackPresssedEvent) {
        HILog.d(TAG, "Subscribe: AboutFragment: getOnBackPresssedEvent: ");
        getActivity().getSupportFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
    }
}