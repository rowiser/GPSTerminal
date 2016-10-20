package com.rowiser.android.gps.gpsterminal.ui;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.rowiser.android.gps.gpsterminal.GPSFeatureControllor;
import com.rowiser.android.gps.gpsterminal.GPSTerminalService;
import com.rowiser.android.gps.gpsterminal.R;
import com.rowiser.android.gps.gpsterminal.bean.SettingInfo;
import com.rowiser.android.gps.gpsterminal.utils.JLog;

public class SettingsFragment extends Fragment {
    private final static String TAG = "SettingsFragment";
    private final static JLog LOG = new JLog(TAG, GPSTerminalService.DEBUG, JLog.TYPE_DEBUG);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private EditText mHost, mPort, mDevice, mCycle;

    private GPSFeatureControllor mControllor;
    private Toast mToast;
    private Context mContext;

    public SettingsFragment() {
        // Required empty public constructor
    }


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SettingsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SettingsFragment newInstance(String param1, String param2) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity().getApplicationContext();
        mControllor = GPSFeatureControllor.getInstance(mContext);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        mToast = Toast.makeText(mContext, "", Toast.LENGTH_LONG);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        mHost = (EditText) view.findViewById(R.id.host);
        mPort = (EditText) view.findViewById(R.id.port);
        mDevice = (EditText) view.findViewById(R.id.device);
        mCycle = (EditText) view.findViewById(R.id.cycle);
        view.findViewById(R.id.save_settings).setOnClickListener(mClickListener);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        LOG.print("onResume...");
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mControllor.getFeature() != null){
                    SettingInfo settingsInfo = null;
                    try {
                        settingsInfo = mControllor.getFeature().getSettings();
                        if(settingsInfo == null){
                            LOG.print("settings info is null ~~~");
                            String deviceId = mControllor.getFeature().getDeviceId();
                            if(deviceId != null) {
                                mDevice.setText(deviceId);
                            }
                        }else {
                            LOG.print(settingsInfo.toString());
                            setSettingsInfo(settingsInfo);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else{
                    LOG.print("service is null");
                }
            }
        }, 1000);

    }

    private void setSettingsInfo(SettingInfo settingsInfo) {
        mHost.setText(settingsInfo.host);
        mPort.setText(String.valueOf(settingsInfo.port));
        mDevice.setText(settingsInfo.device);
        mCycle.setText(String.valueOf(settingsInfo.uploadCycle));
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.save_settings:
                    boolean isSaved = saveSettings();
                    if(isSaved) {
                        //得到InputMethodManager的实例
                        InputMethodManager imm = (InputMethodManager)mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                        //如果开启
                        if (imm.isActive()) {
                            //关闭软键盘，开启方法相同，这个方法是切换开启与关闭状态的
                            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
                        }
                        ((MainActivity) getActivity()).switch2MainFragment();
                    }
                    break;
            }
        }
    };

    private boolean saveSettings() {
        LOG.print("saveSettings...");
        mToast.cancel();
        if("".equals(mHost.getText().toString())){
            LOG.print("saveSettings...1");
            mHost.requestFocus();
            mToast = Toast.makeText(mContext, mContext.getString(R.string.null_info_tips, mContext.getString(R.string.host)), Toast.LENGTH_LONG);
            mToast.show();
            return false;
        }
        if("".equals(mPort.getText().toString())){
            mPort.requestFocus();
            mToast = Toast.makeText(mContext, mContext.getString(R.string.null_info_tips, mContext.getString(R.string.port)), Toast.LENGTH_LONG);
            mToast.show();
            return false;
        }
        if("".equals(mDevice.getText().toString())){
            mDevice.requestFocus();
            mToast = Toast.makeText(mContext, mContext.getString(R.string.null_info_tips, mContext.getString(R.string.device)), Toast.LENGTH_LONG);
            mToast.show();
            return false;
        }
        if("".equals(mCycle.getText().toString())){
            mToast = Toast.makeText(mContext, mContext.getString(R.string.null_info_tips, mContext.getString(R.string.cycle)), Toast.LENGTH_LONG);
            mToast.show();
            return false;
        }
        SettingInfo settingInfo = new SettingInfo();
        settingInfo.host = mHost.getText().toString();
        settingInfo.port = Integer.valueOf(mPort.getText().toString());
        settingInfo.device = mDevice.getText().toString();
        settingInfo.uploadCycle = Integer.valueOf(mCycle.getText().toString());
        if(mControllor.getFeature() != null){
            try {
                mControllor.getFeature().updateSettings(settingInfo);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return true;
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };
}
