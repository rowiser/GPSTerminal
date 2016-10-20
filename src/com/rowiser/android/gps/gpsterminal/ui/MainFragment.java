package com.rowiser.android.gps.gpsterminal.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.rowiser.android.gps.gpsterminal.GPSFeatureControllor;
import com.rowiser.android.gps.gpsterminal.GPSTerminalService;
import com.rowiser.android.gps.gpsterminal.R;
import com.rowiser.android.gps.gpsterminal.bean.LocationInfo;
import com.rowiser.android.gps.gpsterminal.bean.SettingInfo;
import com.rowiser.android.gps.gpsterminal.utils.JLog;

public class MainFragment extends Fragment {
    private final static String TAG = "MainFragment";
    private final static JLog LOG = new JLog(TAG, GPSTerminalService.DEBUG, JLog.TYPE_DEBUG);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private TextView mLongitude, mLatitude, mDateTime, mModule, mAddress;

    private GPSFeatureControllor mControllor;
    private Toast mToast;
    private Context mContext;
    private LocationInfo mLocationInfo;

    public MainFragment() {
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
    public static MainFragment newInstance(String param1, String param2) {
        MainFragment fragment = new MainFragment();
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
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        mLongitude = (TextView) view.findViewById(R.id.tv_longitude);
        mLatitude = (TextView) view.findViewById(R.id.tv_latitude);
        mDateTime = (TextView) view.findViewById(R.id.tv_datetime);
        mModule = (TextView) view.findViewById(R.id.tv_module);
        mAddress = (TextView) view.findViewById(R.id.tv_address);
        view.findViewById(R.id.btn_back).setOnClickListener(mClickListener);
        view.findViewById(R.id.btn_settings).setOnClickListener(mClickListener);
        view.findViewById(R.id.btn_exit).setOnClickListener(mClickListener);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mControllor.addMainFragmentInfoListener(mListener);
        LOG.print("onResume...");
        if(mLocationInfo != null){
            updateInfo(mLocationInfo);
        }else {
            getLocation();
        }
    }

    private void getLocation(){
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mControllor.getFeature() != null){
                    try {
                        mLocationInfo = mControllor.getFeature().getLocationInfo();
                        if(mLocationInfo != null){
                            updateInfo(mLocationInfo);
                        }else{
                            LOG.print("location is null...");
                            getLocation();
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

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        mControllor.removeMainFragmentInfoListener(mListener);
        super.onPause();
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btn_back:
                    getActivity().finish();
                    break;
                case R.id.btn_exit:
                    dialog();
//                    exit();
//                    getActivity().finish();
                    break;
                case R.id.btn_settings:
                    ((MainActivity)getActivity()).switch2SettingsFragment();
                    break;
            }
        }
    };

    protected void dialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.exit_tips);
        builder.setTitle(R.string.tips_title);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                exit();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    public void exit() {
        if(mControllor.getFeature() != null){
            try {
                mControllor.getFeature().stopUploadData();
                mControllor.stopService();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        getActivity().finish();
    }

    private GPSFeatureControllor.MainFragmentInfoListener mListener = new GPSFeatureControllor.MainFragmentInfoListener() {
        @Override
        public void notifyLocationChanged(final LocationInfo locationInfo) {
            mLocationInfo = locationInfo;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateInfo(locationInfo);
                }
            });
//            locationInfo = li;
//            mHandler.removeMessages(MSG_UPDATE_LOCATION);
//            mHandler.sendEmptyMessage(MSG_UPDATE_LOCATION);
        }
    };

//    private final static int MSG_UPDATE_LOCATION = 1;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
//            switch (msg.what){
//                case MSG_UPDATE_LOCATION:
//
//                    break;
//            }
        }
    };

    private void updateInfo(LocationInfo locationInfo) {
        LOG.print("updateInfo...");
        if(locationInfo == null)return;
        mLongitude.setText(String.valueOf(locationInfo.longitude));
        mLatitude.setText(String.valueOf(locationInfo.latitude));
        mDateTime.setText(locationInfo.dateTime);
        mModule.setText("lbs 精度 " + locationInfo.module + (locationInfo.satelliteCount <= 0 ? "" : "  " + locationInfo.satelliteCount + "颗星"));
        mAddress.setText(locationInfo.address);
    }

}
