package com.rowiser.android.gps.gpsterminal.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;

import com.rowiser.android.gps.gpsterminal.GPSFeatureControllor;
import com.rowiser.android.gps.gpsterminal.GPSTerminalService;
import com.rowiser.android.gps.gpsterminal.IGpsTerminalFeature;
import com.rowiser.android.gps.gpsterminal.R;
import com.rowiser.android.gps.gpsterminal.bean.LocationInfo;
import com.rowiser.android.gps.gpsterminal.bean.SettingInfo;
import com.rowiser.android.gps.gpsterminal.utils.AbsServiceControllerHelper;
import com.rowiser.android.gps.gpsterminal.utils.JLog;

public class MainActivity extends Activity {

    private final static String TAG = "MainActivity";
    private final static JLog LOG = new JLog(TAG, GPSTerminalService.DEBUG, JLog.TYPE_DEBUG);
    private GPSFeatureControllor mControllor;
    private IGpsTerminalFeature mFeature;

    private SettingsFragment mSettingsFragment;
    private MainFragment mMainFragment;
    private FragmentManager mFragmentManager;
    private static boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent startGpsTerminalService = new Intent(GPSTerminalService.TERMINAL_SERVICE);
        startGpsTerminalService.setPackage("com.rowiser.android.gps.gpsterminal");
        startService(startGpsTerminalService);
        mFragmentManager = getFragmentManager();
        mSettingsFragment = SettingsFragment.newInstance("aa", "bb");
        mMainFragment = MainFragment.newInstance("aa", "bb");
        mControllor = GPSFeatureControllor.getInstance(getApplicationContext());
        mControllor.addServerSettingsListener(listener);
        connect2service();
        replaceFragment(mMainFragment, R.id.main_layout, false);
    }

    public void connect2service(){
        LOG.print("isConnected = " + isConnected);
        if(!isConnected) {
            mControllor.connectService(new AbsServiceControllerHelper.OnServiceConnectSuccessListener() {
                @Override
                public void onServiceConnectSuccess() {
                    isConnected = true;
                    LOG.print("connect service success~~~~~~");
                    mFeature = mControllor.getFeature();
                    if (mFeature != null) {
                        SettingInfo settingsInfo = null;
                        try {
                            settingsInfo = mFeature.getSettings();
                            if (settingsInfo == null) {
                                switch2SettingsFragment();
                            } else {
                                LOG.print(settingsInfo.toString());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        LOG.print("service is null");
                    }
                }
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }


    @Override
    protected void onResume() {
        LOG.print("---onResume");
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        LOG.print("---onDestroy");
        if(mControllor != null){
            isConnected = false;
            mControllor.removeServerSettingsListener(listener);
            mControllor.disconnectService();
        }
        super.onDestroy();
    }

    private GPSFeatureControllor.ServiceListener listener = new GPSFeatureControllor.ServiceListener() {
        @Override
        public void finishActivity() {
            finish();
        }
    };

    private final void replaceFragment(Fragment fragment, int containerViewId, boolean isAddStack)
    {
        try {
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.replace(containerViewId, fragment);
            ft.setTransition(FragmentTransaction.TRANSIT_NONE);
            if (isAddStack) ft.addToBackStack(null);
            ft.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void switch2SettingsFragment(){
        replaceFragment(mSettingsFragment, R.id.main_layout, true);
    }

    public void switch2MainFragment(){
        connect2service();
        replaceFragment(mMainFragment, R.id.main_layout, false);
    }
}
