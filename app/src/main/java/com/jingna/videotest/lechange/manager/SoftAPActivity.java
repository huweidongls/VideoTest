package com.jingna.videotest.lechange.manager;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jingna.videotest.R;
import com.jingna.videotest.lechange.Dialog.PasswordDialog;
import com.jingna.videotest.lechange.business.Business;
import com.jingna.videotest.lechange.business.util.WifiManagerUtil;
import com.jingna.videotest.lechange.common.CommonTitle;
import com.jingna.videotest.lechange.common.ProgressDialog;
import com.jingna.videotest.lechange.listener.PasswordSetListener;
import com.jingna.videotest.lechange.listview.DevicelistActivity;
import com.lechange.common.log.Logger;
import com.lechange.opensdk.api.bean.CheckDeviceBindOrNot;
import com.lechange.opensdk.api.bean.DeviceOnline;
import com.lechange.opensdk.softap.LCOpenSDK_SoftAPConfig;
import com.lechange.opensdk.utils.NetWorkUtil;

public class SoftAPActivity extends FragmentActivity implements Handler.Callback, View.OnClickListener, PasswordSetListener, Runnable {

    private static final String TAG = "LCOpenSDK_SoftAPActivity";
    private final int ON_LINE_SUCCESS = 0x11;
    private final int ON_LINE_FAILED = 0x12;
    private final int CHECK_DEVICE_ONLINE = 0X1B;
    private final int ADD_DEVICE_SUCCESS = 0x13;
    private boolean isCheckOnline = true;
    private Handler handler;
    private String devHotSpot = "";
    private String ssid = "";
    private String pwd = "";
    private String devId;
    private String devType;
    private WifiManagerUtil managerUtil;
    private PasswordDialog passwordDialog;
//    MyBroadcastReceiver broadcastReceiver;
    TextView mSSidView;
    CommonTitle mCommonTitle;
    private ProgressDialog mProgressDialog; // 播放加载使用
    public static final String CONNECTIVITY_CHANGE_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    private String key = "";
    Runnable checkRunnable = new Runnable() {
        @Override
        public void run() {
            //30秒后移除在线检查,并且返回失败，正常情况第一次就可以查询到在线的状态
            handler.removeCallbacksAndMessages(CHECK_DEVICE_ONLINE);
            handler.obtainMessage(ON_LINE_FAILED).sendToTarget();
            isCheckOnline = false;
            Logger.d(TAG, "check online timeout");
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.soft_ap_activity);
        // 绘制标题
        mCommonTitle = (CommonTitle) findViewById(R.id.title);
        mCommonTitle.initView(R.drawable.title_btn_back, 0,
                R.string.devices_soft_ap_config);
        mSSidView = (TextView) findViewById(R.id.ssid);
        mCommonTitle.setOnTitleClickListener(new CommonTitle.OnTitleClickListener() {
            @Override
            public void onCommonTitleClick(int id) {
                // TODO Auto-generated method stub
                switch (id) {
                    case CommonTitle.ID_LEFT:
                        finish();
                        break;
                }
            }
        });

        mProgressDialog = (ProgressDialog) this.findViewById(R.id.query_load);
        findViewById(R.id.start_softAP_config).setOnClickListener(this);
        findViewById(R.id.start_check_online).setOnClickListener(this);
        managerUtil = new WifiManagerUtil(getApplicationContext());
        devId = getIntent().getStringExtra("devId");
        devType = getIntent().getStringExtra("devType");
        ssid = getIntent().getStringExtra("ssid");
        pwd = getIntent().getStringExtra("mPwd");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CONNECTIVITY_CHANGE_ACTION);
//        broadcastReceiver=new MyBroadcastReceiver();
//        registerReceiver(broadcastReceiver, intentFilter);
        handler = new Handler(this);
        passwordDialog = new PasswordDialog();
        passwordDialog.setListener(this);
        devHotSpot = devType + "-" + devId.substring(devId.length() - 4);
        mSSidView.setText(getResources().getString(R.string.connect_wifi_name) + " : " + ssid);

    }


    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case LCOpenSDK_SoftAPConfig.MSG_WHAT:
                mProgressDialog.setStop();
                Toast.makeText(getApplicationContext(), String.valueOf(msg.arg1), Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.switch_wifi_and_start_check_online), Toast.LENGTH_LONG).show();
                break;
            case CHECK_DEVICE_ONLINE:
//                checkOnline();
                checkOnBindAndRemind();
                break;
            case ON_LINE_FAILED:
                Logger.d(TAG, "check_online_failed");
                Toast.makeText(getApplicationContext(), "check_online_failed", Toast.LENGTH_SHORT).show();
                handler.removeCallbacksAndMessages(CHECK_DEVICE_ONLINE);
                mProgressDialog.setStop();
                break;
            case ON_LINE_SUCCESS:
                //设备在线
                Logger.d(TAG, "check_online_success");
                handler.removeCallbacks(checkRunnable);
                unBindDeviceInfo();
                mProgressDialog.setStart(getResources().getString(R.string.binding_device));
                break;
            case ADD_DEVICE_SUCCESS:
                mProgressDialog.setStop();
                Logger.d(TAG, "add_device_success");
                Toast.makeText(getApplicationContext(), "SuccessAddDevice", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SoftAPActivity.this, DevicelistActivity.class));
                finish();
                break;


        }


        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_softAP_config:
                if (!managerUtil.isWifi(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(), getString(R.string.connect_device_wifi), Toast.LENGTH_SHORT).show();
                    return;
                }
                com.lechange.common.log.Logger.d(SoftAPActivity.this.getClass().getSimpleName(), " devHotSpot : " + devHotSpot);
//                if (isConnectedDevHot()) {
                if (null != passwordDialog && !passwordDialog.isAdded()) {
                    handler.removeCallbacks(SoftAPActivity.this);
                    mProgressDialog.setStart(getString(R.string.search_devices));
                    Business.getInstance().searchDevice(devId, 30 * 1000, new Handler() {
                        public void handleMessage(final Message msg) {
                            if (msg.what == 1) {
                                //需要初始化
                                passwordDialog.setCurrentType(1);
                                passwordDialog.show(getSupportFragmentManager(), "passwordDialog");
                            } else if (msg.what == 2) {
                                //已经初始化
                                passwordDialog.setCurrentType(2);
                                passwordDialog.show(getSupportFragmentManager(), "passwordDialog");
                            } else if (msg.what == 0) {
                                //不需要初始化
                                passwordDialog.setCurrentType(0);
                                passwordDialog.show(getSupportFragmentManager(), "passwordDialog");
                            } else {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.search_devices_timeout), Toast.LENGTH_LONG).show();
                                mProgressDialog.setStop();
                            }
                        }
                    });

                }

//                } else {
//                    Toast.makeText(getApplicationContext(), getString(R.string.connect_device_wifi), Toast.LENGTH_SHORT).show();
//                }
                break;

            case R.id.start_check_online:
                isCheckOnline = true;
                handler.obtainMessage(CHECK_DEVICE_ONLINE).sendToTarget();
                handler.postDelayed(checkRunnable, 30 * 1000);
                mProgressDialog.setStart(getResources().getString(R.string.checking_device_online));

                break;
        }
    }


    //是否已连上设备热点
    public boolean isConnectedDevHot() {
//        return true;
        boolean isWifiConnected = NetWorkUtil.NetworkType.NETWORK_WIFI.equals(NetWorkUtil.getNetworkType(getApplicationContext()));
        WifiInfo wifiInfo = managerUtil.getCurrentWifiInfo();
        if (wifiInfo == null || !isWifiConnected) {
            return false;
        } else {
            String ssid = "\"" + this.devHotSpot + "\"";
            return wifiInfo.getSSID().equals(ssid);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        unregisterReceiver(broadcastReceiver);
        LCOpenSDK_SoftAPConfig.cancel();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            isCheckOnline = false;
            handler = null;
        }

    }


    /**
     * 校验在线
     */
    private void checkOnline() {
        Business.getInstance().checkOnline(devId,
                new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        Business.RetObject retObject = (Business.RetObject) msg.obj;
                        switch (msg.what) {
                            case 0:
                                if (((DeviceOnline.Response) retObject.resp).data.onLine.equals("1")) {
                                    handler.obtainMessage(ON_LINE_SUCCESS).sendToTarget();
                                } else {
                                    if (isCheckOnline) {
                                        Logger.d(TAG, "onLine : " + ((DeviceOnline.Response) retObject.resp).data.onLine);
                                        handler.obtainMessage(CHECK_DEVICE_ONLINE).sendToTarget();
                                    }
                                }
                                break;
                            case -1000:
                                Logger.e(TAG, "check_online_respond : -1000  " + ((Business.RetObject) msg.obj).mMsg);
                                handler.obtainMessage(ON_LINE_FAILED).sendToTarget();
                                Toast.makeText(getApplicationContext(), ((Business.RetObject) msg.obj).mMsg, Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                Logger.e(TAG, "checkonline  :  " + msg.what);
                                handler.obtainMessage(ON_LINE_FAILED).sendToTarget();
                                Toast.makeText(getApplicationContext(), ((Business.RetObject) msg.obj).mMsg, Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                });

    }

    private void unBindDeviceInfo() {
        if (!Business.getInstance().isOversea) {

            Business.getInstance().unBindDeviceInfo(devId, new Handler() {
                public void handleMessage(Message msg) {
                    String message = (String) msg.obj;
                    //				Log.d(tag, "unBindDeviceInfo,"+message);
                    if (msg.what == 0) {
                        if (message.contains("Auth")) {
                            Logger.d(TAG, " Auth  bindDevice(), key = :"+key + ", deviceID = " + devId);
                            bindDevice();
                        } else if (message.contains("RegCode")) {
                            final EditText et = new EditText(SoftAPActivity.this);
                            final AlertDialog dialog = new AlertDialog.Builder(SoftAPActivity.this)
                                    .setTitle(R.string.alarm_message_keyinput_dialog_title)
                                    .setIcon(android.R.drawable.ic_dialog_info)
                                    .setView(et)
                                    .setPositiveButton(R.string.dialog_positive, null)
                                    .setNegativeButton(R.string.dialog_nagative, null)
                                    .create();
                            dialog.setCanceledOnTouchOutside(false);
                            dialog.show();

                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    if (TextUtils.isEmpty(et.getText())) {
                                        Toast.makeText(getApplicationContext(), "Input can't be empty", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    key = et.getText().toString();
                                    bindDevice();
                                    dialog.dismiss();
                                }
                            });
                        } else {
                            key = "";
                            bindDevice();
                        }
                    } else {
                        mProgressDialog.setStop();
                        Toast.makeText(getApplicationContext(), "unBindDeviceInfo failed", Toast.LENGTH_SHORT).show();
                        Logger.d(TAG, message);
                    }
                }
            });
        } else { //oversea
            Logger.d(TAG, " oversea  bindDevice(), key = :"+key + ", deviceID = " + devId);
            bindDevice();
        }

    }


    /**
     * 绑定
     */
    private void bindDevice() {
        //设备绑定
        Business.getInstance().bindDevice(devId, key,
                new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        Business.RetObject retObject = (Business.RetObject) msg.obj;
                        if (msg.what == 0) {
                            handler.obtainMessage(ADD_DEVICE_SUCCESS).sendToTarget();
                        } else {
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.addDevice_failed), Toast.LENGTH_SHORT).show();
                            Logger.d(TAG, retObject.mMsg);
                        }
                    }
                });
    }


    @Override
    public void onSaveSuccess(String psw1) {
        mProgressDialog.setStart(getResources().getString(R.string.soft_ap_config));
//        if(Business.getInstance().isOversea){//海外软AP需携带code过去
            key=psw1;
//        }
        LCOpenSDK_SoftAPConfig.startSoftAPConfig(ssid, pwd, devId, psw1, handler, 30 * 1000);
    }

    @Override
    public void onWifiPassWord(String psw1) {

    }

    @Override
    public void run() {
        //超时操作
        Toast.makeText(getApplicationContext(), getString(R.string.switch_wifi_timeout), Toast.LENGTH_SHORT).show();
    }

    /**
     * 检查是否已被绑定
     */
    public void checkOnBindAndRemind() {
        Business.getInstance().checkBindOrNot(devId,
                new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        // TODO Auto-generated method stub
                        super.handleMessage(msg);
                        Business.RetObject retObject = (Business.RetObject) msg.obj;
                        if (msg.what == 0) {
                            CheckDeviceBindOrNot.Response resp = (CheckDeviceBindOrNot.Response) retObject.resp;
                            if (!resp.data.isBind) {
                                checkOnline();
                            } else if (resp.data.isBind && resp.data.isMine) {
                                Toast.makeText(getApplicationContext(), getString(R.string.toast_adddevice_already_binded_by_self), Toast.LENGTH_SHORT).show();
                                handler.removeCallbacks(checkRunnable);
                                mProgressDialog.setStop();
                            } else {
                                Toast.makeText(getApplicationContext(), getString(R.string.toast_adddevice_already_binded_by_others), Toast.LENGTH_SHORT).show();
                                handler.removeCallbacks(checkRunnable);
                                mProgressDialog.setStop();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), retObject.mMsg, Toast.LENGTH_SHORT).show();
                            handler.removeCallbacks(checkRunnable);
                            mProgressDialog.setStop();
                        }
                    }
                });
    }

//    public class MyBroadcastReceiver extends android.content.BroadcastReceiver {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if (intent.getAction().equals(CONNECTIVITY_CHANGE_ACTION)) {
//                if (isConnectedDevHot()) {
//                    if (null != passwordDialog && !passwordDialog.isAdded()) {
//                        handler.removeCallbacks(SoftAPActivity.this);
//                        mProgressDialog.setStart(getString(R.string.switch_wifi_success));
//                        Business.getInstance().searchDevice(devId, 30 * 1000, new Handler() {
//                            public void handleMessage(final Message msg) {
//                                if (msg.what == 1) {
//                                    passwordDialog.setCurrentType(1);
//                                    passwordDialog.show(getSupportFragmentManager(), "passwordDialog");
//                                } else if (msg.what == 2) {
//                                    Toast.makeText(getApplicationContext(), getString(R.string.please_reset_device), Toast.LENGTH_LONG).show();
//                                    passwordDialog.setCurrentType(1);
//                                    passwordDialog.show(getSupportFragmentManager(), "passwordDialog");
//                                } else if (msg.what == 0) {
//                                    passwordDialog.setCurrentType(1);
//                                    passwordDialog.show(getSupportFragmentManager(), "passwordDialog");
//                                }
//                            }
//                        });
//
//                    }
//
//                }
//            }
//
//        }
//    }


    @Override
    protected void onResume() {
        super.onResume();
//        if (isConnectedDevHot()) {
//            if (null != passwordDialog && !passwordDialog.isAdded()) {
//                handler.removeCallbacks(SoftAPActivity.this);
//                mProgressDialog.setStart(getString(R.string.switch_wifi_success));
//                Business.getInstance().searchDevice(devId, 30 * 1000, new Handler() {
//                    public void handleMessage(final Message msg) {
//                        if (msg.what == 1) {
//                            passwordDialog.setCurrentType(1);
//                            passwordDialog.show(getSupportFragmentManager(), "passwordDialog");
//                        } else if (msg.what == 2) {
//                            Toast.makeText(getApplicationContext(), getString(R.string.please_reset_device), Toast.LENGTH_LONG).show();
//                            mProgressDialog.setStop();
//                        } else if (msg.what == 0) {
//                            passwordDialog.setCurrentType(1);
//                            passwordDialog.show(getSupportFragmentManager(), "passwordDialog");
//                        }
//                    }
//                });
//            }
//        }
    }
}
