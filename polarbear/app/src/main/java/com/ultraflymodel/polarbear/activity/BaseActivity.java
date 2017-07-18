package com.ultraflymodel.polarbear.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.ultraflymodel.polarbear.R;
import com.ultraflymodel.polarbear.common.Constants;
import com.ultraflymodel.polarbear.common.FragmentController;
import com.ultraflymodel.polarbear.common.HILog;
import com.ultraflymodel.polarbear.eventbus.OnBackPresssedEvent;
import com.ultraflymodel.polarbear.fragment.PolarbearMainFragment;
import com.ultraflymodel.polarbear.ultraflymodel.UltraflyModelApplication;
import com.ultraflymodel.polarbear.utils.StringUtil;

public class BaseActivity extends FragmentActivity implements FragmentController<Fragment>, View.OnClickListener {
    private static final String TAG = BaseActivity.class.getSimpleName();
    public static Context m_ctx;
    public static Fragment mFgContent;
    public static FragmentManager fragmentManager;
    public static FragmentTransaction fragmentTransaction;
    private static FrameLayout fl_container;
    public static boolean mFlagSetting=false, mFlagWakeup=false, mFlageVideo=false, mFlagMusic=false, mFlagSpeak=false;
    private static String popBackName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        HILog.d(TAG, "onCreate:");

        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        // set this if you don't want scroll keyboard
        m_ctx = BaseActivity.this;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        fragmentManager = this.getSupportFragmentManager();
        fl_container = (FrameLayout) findViewById(R.id.fl_container);

    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void clearAndBackToHome() {

    }

    @Override
    public void loadMainPage() {

    }

    @Override
    public void loadFragment(Fragment f, String FragTag) {

    }

    public static void loadFragment(Fragment f, String FragTag, boolean useAdd) {
        HILog.d(TAG, "loadFragment : useAdd = " + useAdd);
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        if (useAdd) {
            fragmentTransaction.add(fl_container.getId(), f, FragTag);
        } else {
            fragmentTransaction.replace(fl_container.getId(), f, FragTag);
        }
        fragmentTransaction.addToBackStack(FragTag);
        fragmentTransaction.commit();

    }

    public String getPopBackName() {
        return popBackName;
    }

    public static void setPopBackName(String name) {
        popBackName = name;
    }

    public void GotStraightToHome() {
        HILog.d(TAG, "GotStraightToHome : ");
        setPopBackName(null);
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStack(Constants.JumpTo.PMAIN.toString(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        PolarbearMainFragment f = new PolarbearMainFragment();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        fragmentTransaction.addToBackStack(Constants.JumpTo.PMAIN.toString());
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void onBackPressed() {
        String popbackname = getPopBackName();
        HILog.d(TAG, "onBackPressed : popbackname = " + popbackname);
        int count = getSupportFragmentManager().getBackStackEntryCount();
        HILog.d(TAG, "onBackPressed : getBackStackEntryCount = " + count);
        if (!StringUtil.isStrNullOrEmpty(popbackname)) {
            if(popbackname.equals(Constants.JumpTo.PMAIN.toString())){
                getSupportFragmentManager().popBackStack(popbackname, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                finish();

            } else if(popbackname.equals(Constants.JumpTo.WIFISETUP.toString())){
                setPopBackName(Constants.JumpTo.PMAIN.toString());
                UltraflyModelApplication.getInstance().bus.post(new OnBackPresssedEvent());
            }
        }
        System.gc();
        super.onBackPressed();
    }


}
