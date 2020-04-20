package com.jingna.videotest.lechange.manager;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jingna.videotest.R;
import com.jingna.videotest.lechange.business.Business;
import com.jingna.videotest.lechange.common.CommonTitle;
import com.jingna.videotest.lechange.common.ProgressDialog;
import com.lechange.common.configwifi.LCSmartConfig;
import com.lechange.common.log.Logger;
import com.lechange.opensdk.api.bean.CheckDeviceBindOrNot;
import com.lechange.opensdk.configwifi.LCOpenSDK_ConfigWifi;

import java.io.Serializable;
import java.util.List;

public class DeviceConfigWifiActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "LCOpenSDK-DeviceConfigWifi";
    private CommonTitle mCommonTitle;
    private ProgressDialog mProgressDialog; // 播放加载使用
    private WifiInfo mWifiInfo;
    private TextView mSsidText;
    private EditText mSnText;
    private EditText mPwdText;
    private EditText mSCText; //设备SC码
    private ImageView mWirelessButton;
    private ImageView mWiredButton;
    private ImageView mScanQRCode;
    private static final int CONFIG_WIFI_TIMEOUT_TIME = 120 * 1000;
    private static final int CONFIG_SEARCH_DEVICE_TIME = 120 * 1000;
    String ssid="";
    private String devType="";
    private Runnable progressRun = new Runnable() {
        @Override
        public void run() {
            toast(getResources().getString(R.string.toast_adddevice_config_timeout));
            stopConfig();
        }
    };

    private final int DEVICE_SEARCH_SUCCESS = 0x1B;
    private final int DEVICE_SEARCH_FAILED = 0x1C;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Logger.d(DeviceConfigWifiActivity.TAG, "msg.what  " + msg.what);
            switch (msg.what) {
                case DEVICE_SEARCH_SUCCESS:
                    Logger.d(TAG, "deviceSearchSuccess");
                    stopConfig();
                    mProgressDialog.setStop();
                    Intent intent = new Intent(DeviceConfigWifiActivity.this, InitDeviceActivity.class);
                    intent.putExtra("devSc", mSCText.getText().toString());//携带设备SC码
                    intent.putExtra("devId", mSnText.getText().toString());
                    intent.putExtra("DeviceInitInfo", (Serializable) msg.obj);
                    startActivity(intent);
                    finish();
                    break;
                case DEVICE_SEARCH_FAILED:
                    stopConfig();
                    mProgressDialog.setStop();
                    Logger.d(TAG, "deviceSearchFailed:" + msg.obj);
                    toast("deviceSearchFailed:" + msg.obj);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_wifi_activity);


        // 绘制标题
        mCommonTitle = (CommonTitle) findViewById(R.id.title);
        mCommonTitle.initView(R.drawable.title_btn_back, 0,
                R.string.devices_add_name);

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


        // 绘制画面
        mSnText = (EditText) findViewById(R.id.deviceSN);
        mPwdText = (EditText) findViewById(R.id.wifiPasswd);
        mSsidText = (TextView) findViewById(R.id.wifiName);
        mSCText = (EditText) findViewById(R.id.deviceSC);
        mScanQRCode = (ImageView) findViewById(R.id.scan_qr_code);
        mScanQRCode.setOnClickListener(this);
        mWirelessButton = (ImageView) findViewById(R.id.wirelessAdd);
        mWirelessButton.setOnClickListener(this);
        mWiredButton = (ImageView) findViewById(R.id.wiredAdd);
        mWiredButton.setOnClickListener(this);
        findViewById(R.id.softApAdd).setOnClickListener(this);
        // load组件
        mProgressDialog = (ProgressDialog) this.findViewById(R.id.query_load);
        WifiManager mWifiManager = (WifiManager) getApplicationContext().getSystemService(Activity.WIFI_SERVICE);
        mWifiInfo = mWifiManager.getConnectionInfo();
        if (mWifiInfo != null) {
            mSsidText.setText("SSID:" + mWifiInfo.getSSID().replaceAll("\"", ""));
            ssid=mWifiInfo.getSSID().replaceAll("\"", "");
        }

    }

    //添加方式选择按钮点击:无线、有线、软AP
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.wirelessAdd://无线
                if (TextUtils.isEmpty(mPwdText.getText().toString()) || TextUtils.isEmpty(mSnText.getText().toString())) {
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_adddevice_no_sn_or_psw), Toast.LENGTH_SHORT).show();
                } else {
                    requestStoragePermission();

                }
                break;
            case R.id.wiredAdd://有线
                if (TextUtils.isEmpty(mSnText.getText().toString())) {
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_adddevice_no_sn), Toast.LENGTH_SHORT).show();
                } else {
                    checkOnBindandline();
                }
                break;
            case R.id.scan_qr_code://扫设备二维码
                requestCameraPermission();
                break;
            case R.id.softApAdd://软AP
                if (TextUtils.isEmpty(mSnText.getText().toString()) || TextUtils.isEmpty(mPwdText.getText().toString())) {
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_adddevice_no_sn_or_psw), Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent=new Intent(DeviceConfigWifiActivity.this, SoftAPActivity.class);
                    intent.putExtra("devId",mSnText.getText().toString());
                    intent.putExtra("devType",devType);
                    intent.putExtra("ssid",ssid);
                    intent.putExtra("mPwd", mPwdText.getText().toString());//将要连接的wifi的密码携带过去
                    startActivityForResult(intent, 0);
                }

                break;
            default:
                break;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopConfig();
    }


    /**
     * 无线配对校验
     */
    public void checkOnBindAndRemind() {
        Business.getInstance().checkBindOrNot(mSnText.getText().toString(),
                new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        // TODO Auto-generated method stub
                        super.handleMessage(msg);
                        Business.RetObject retObject = (Business.RetObject) msg.obj;
                        if (msg.what == 0) {
                            CheckDeviceBindOrNot.Response resp = (CheckDeviceBindOrNot.Response) retObject.resp;
                            if (!resp.data.isBind) {
                            showWifiConfig();
                            } else if (resp.data.isBind && resp.data.isMine)
                                toast(getString(R.string.toast_adddevice_already_binded_by_self));
                            else
                                toast(getString(R.string.toast_adddevice_already_binded_by_others));
                        } else {
                            toast(retObject.mMsg);
                        }
                    }
                });
    }


    /**
     * 有线配对校验
     */
    private void checkOnBindandline() {
        Business.getInstance().checkBindOrNot(mSnText.getText().toString(),
                new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        // TODO Auto-generated method stub
                        super.handleMessage(msg);
                        Business.RetObject retObject = (Business.RetObject) msg.obj;
                        if (msg.what == 0) {
                            CheckDeviceBindOrNot.Response resp = (CheckDeviceBindOrNot.Response) retObject.resp;
                            if (!resp.data.isBind) {
                                mProgressDialog.setStart(getString(R.string.search_devices)) ;
                                searchDevice(CONFIG_SEARCH_DEVICE_TIME);
                            } else if (resp.data.isBind && resp.data.isMine)
                                toast(getString(R.string.toast_adddevice_already_binded_by_self));
                            else
                                toast(getString(R.string.toast_adddevice_already_binded_by_others));
                        } else {
                            toast(retObject.mMsg);
                        }
                    }
                });
    }


    private void toast(String content) {
        Toast.makeText(getApplicationContext(), content, Toast.LENGTH_SHORT).show();
    }

    /**
     * 开启无线配网流程（权限检查，配对说明）
     */
    public void showWifiConfig() {
        boolean isMinSDKM = Build.VERSION.SDK_INT < 23;
        boolean isGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (isMinSDKM || isGranted) {

            showPairDescription();
            // 开启无线配对
            return;
        }

        requestLocationPermission();
    }

    /**
     * 显示配对说明
     */
    private void showPairDescription() {
        DialogInterface.OnClickListener dialogOnclicListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case Dialog.BUTTON_POSITIVE:
                        startConfig();
                        break;
                    case Dialog.BUTTON_NEGATIVE:
                        break;
                    case Dialog.BUTTON_NEUTRAL:
                        break;
                }
            }
        };
        // dialog参数设置
        AlertDialog.Builder builder = new AlertDialog.Builder(this); // 先得到构造器
        builder.setTitle(R.string.devices_config_dialog_title); // 设置标题
        builder.setMessage(R.string.devices_config_dialog_message); // 设置内容
        builder.setPositiveButton(R.string.dialog_positive,
                dialogOnclicListener);
        builder.setNegativeButton(R.string.dialog_nagative,
                dialogOnclicListener);
        builder.create().show();
    }


    /**
     * 请求相关权限
     */
    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {

            Log.d("Uriah", "Uriah + shouldShowRequestPermission true");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }
    }

    /**
     * 请求相关权限
     */
    private void requestCameraPermission() {

        boolean isMinSDKM = Build.VERSION.SDK_INT < 23;
        boolean isGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        if (isMinSDKM || isGranted) {
//            startActivityForResult(new Intent(DeviceConfigWifiActivity.this, CaptureActivity.class), 0x11111);
            return;
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            Log.d("Uriah", "Uriah + shouldShowRequestPermission true");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    2);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    2);
        }
    }

    /**
     * 请求存储权限
     *
     */
    private void requestStoragePermission(){
        boolean isMinSDKM = Build.VERSION.SDK_INT < 23;
        boolean isGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (isMinSDKM || isGranted) {
            checkOnBindAndRemind();
            // 存储权限
            return;
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            Logger.d("Uriah", "Uriah + shouldShowRequestPermission true");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 3);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 3);
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // TODO Auto-generated method stub
        if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == 1) {
                showPairDescription();
            } else if (requestCode == 2) {
//                startActivityForResult(new Intent(DeviceConfigWifiActivity.this, CaptureActivity.class), 0x11111);
            }else if(requestCode == 3){
                checkOnBindAndRemind();
            }

        } else {
            if (requestCode == 1) {
                toast(getString(R.string.toast_permission_location_forbidden));
            } else if (requestCode == 2) {
                toast(getString(R.string.toast_permission_camera_forbidden));
            } else if(requestCode == 3){
                toast(getString(R.string.toast_permission_storage_forbidden));
            }

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0x11111 && resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            String result = bundle.getString("result");
            String devSn="";
            if (result.contains(",")) {
                devSn = result.split(",")[0].split(":")[1];
            }else if(result.contains(":")){
                devSn=result.split(":")[0];
                devType=result.split(":")[1];
            }
            if(devSn!=null&&devSn.length()!=15){
               devSn= result.substring(result.indexOf(":")+1,result.indexOf("}"));
            }
            mSnText.setText(devSn);
        }
    }

    /**
     * 启动无线配对
     */
    private void startConfig() {
        // 开启播放加载控件
        mProgressDialog.setStart(getString(R.string.wifi_config_loading)) ;//无线配网搜索设备中...

        String ssid = mWifiInfo.getSSID().replaceAll("\"", "");
        String ssid_pwd = mPwdText.getText().toString();
        String code = mSnText.getText().toString().toUpperCase();

        String mCapabilities = getWifiCapabilities(ssid);
        // 无线超时任务
        mHandler.postDelayed(progressRun, CONFIG_WIFI_TIMEOUT_TIME);
        // 调用接口，开始通过smartConfig匹配 (频率由11000-->17000)
        LCOpenSDK_ConfigWifi.configWifiStart(code, ssid, ssid_pwd, "WPA2", LCSmartConfig.ConfigType.LCConfigWifi_Type_ALL,true,11000,1);
        searchDevice(CONFIG_SEARCH_DEVICE_TIME);//搜索设备及超时时间
    }


    /**
     * 关闭无线配对
     */
    private void stopConfig() {
        mHandler.removeCallbacks(progressRun);
        LCOpenSDK_ConfigWifi.configWifiStop();// 调用smartConfig停止接口
        Business.getInstance().stopSearchDevice();
    }

    /**
     * 获取wifi加密信息
     */
    private String getWifiCapabilities(String ssid) {
        String mCapabilities = null;
        ScanResult mScanResult = null;
        WifiManager mWifiManager = (WifiManager) getApplicationContext().getSystemService(Activity.WIFI_SERVICE);
        if (mWifiManager != null) {
            WifiInfo mWifi = mWifiManager.getConnectionInfo();
            if (mWifi != null) {
                // 判断SSID是否�?��
                if (mWifi.getSSID() != null
                        && mWifi.getSSID().replaceAll("\"", "").equals(ssid)) {
                    List<ScanResult> mList = mWifiManager.getScanResults();
                    if (mList != null) {
                        for (ScanResult s : mList) {
                            if (s.SSID.replaceAll("\"", "").equals(ssid)) {
                                mScanResult = s;
                                break;
                            }
                        }
                    }
                }
            }
        }
        mCapabilities = mScanResult != null ? mScanResult.capabilities : null;
        return mCapabilities;
    }


    private void searchDevice(int timeout) {
        final String deviceId = mSnText.getText().toString();
        Business.getInstance().searchDevice(deviceId, timeout, new Handler() {
            public void handleMessage(final Message msg) {
                if (msg.what < 0) {
                    if (msg.what == -2)
                        mHandler.obtainMessage(DEVICE_SEARCH_FAILED, "device not found").sendToTarget();
                    else
                        mHandler.obtainMessage(DEVICE_SEARCH_FAILED, "StartSearchDevices failed").sendToTarget();
                    return;
                }

                mHandler.obtainMessage(DEVICE_SEARCH_SUCCESS, msg.obj).sendToTarget();
            }
        });
    }

}
