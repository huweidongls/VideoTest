package com.jingna.videotest.lechange.manager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.jingna.videotest.R;
import com.jingna.videotest.lechange.business.Business;
import com.jingna.videotest.lechange.business.util.TaskPoolHelper;
import com.jingna.videotest.lechange.common.CommonTitle;
import com.jingna.videotest.lechange.common.ProgressDialog;
import com.jingna.videotest.lechange.listview.DevicelistActivity;
import com.lechange.common.log.Logger;
import com.lechange.opensdk.api.bean.DeviceOnline;
import com.lechange.opensdk.media.DeviceInitInfo;


/*****
 *初始化设备
 */

public class InitDeviceActivity extends Activity {

    private String TAG = "LCOpenSDK_Demo_InitDeviceActivity";
    private final int INITMODE_UNICAST = 0;
    private final int INITMODE_MULTICAST = 1;
    private final int ON_LINE_SUCCESS = 0x11;
    private final int ON_LINE_FAILED = 0x12;
    private final int ADD_DEVICE_SUCCESS = 0x13;
    private final int DEVICE_INIT_SUCCESS = 0x18;
    private final int DEVICE_INIT_FAILED = 0x19;
    private final int DEVICE_INIT_BY_IP_FAILED = 0x1A;
    private final int CHECK_DEVICE_ONLINE=0X1B;
    private final int DEVICE_SEARCH_SUCCESS = 0x1C;
    private final int DEVICE_SEARCH_FAILED = 0x1D;
    private final int ON_LINE_TIMEOUT = 0x1E;
    private String devSc = "";
    private String devId = "";
    private String key = "";
    private DeviceInitInfo deviceInitInfo = null;
    private int curInitMode = INITMODE_MULTICAST;
    private  boolean  isCheckOnline=true;
    private static final int CHECK_ONLINE_TIMEOUT=30*1000;
    private boolean isSecondInitByIP=false;


    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DEVICE_INIT_SUCCESS:
                    Logger.d(TAG, "deviceInitSuccess");
                    toast("deviceInitSuccess!");
                    mHandler.obtainMessage(CHECK_DEVICE_ONLINE).sendToTarget();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //30秒后移除在线检查,并且返回失败，正常情况第一次就可以查询到在线的状态
                            mHandler.removeCallbacksAndMessages(CHECK_DEVICE_ONLINE);
                            mHandler.obtainMessage(ON_LINE_TIMEOUT).sendToTarget();
                            isCheckOnline=false;
                            Logger.d(TAG,"check online timeout");
                        }
                    },CHECK_ONLINE_TIMEOUT);
                    mProgressDialog.setStart(getResources().getString(R.string.checking_device_online));
                    break;
                case DEVICE_INIT_FAILED:
                    //组播失败后走单播
                    if(curInitMode==INITMODE_UNICAST){
                        toast("deviceInitFailed: " + (String) msg.obj);
                        return;
                    }
                    curInitMode = INITMODE_UNICAST;
                    if (null != deviceInitInfo) {
                        initDevice(deviceInitInfo, curInitMode);
                    }

                    break;
                case DEVICE_INIT_BY_IP_FAILED:
                    if (isSecondInitByIP) {
                        curInitMode=INITMODE_MULTICAST;
                        isSecondInitByIP=false;
                        toast("deviceInitByIPFailed: " + (String) msg.obj);
                        mProgressDialog.setStop();
                        showTipDialog(getResources().getString(R.string.toast_device_init_device_failed));
                        return;
                    }
                    if (null != deviceInitInfo) {
                        isSecondInitByIP=true;
                        initDevice(deviceInitInfo, curInitMode);
                    }
                    curInitMode = INITMODE_MULTICAST;
                    break;
                case ADD_DEVICE_SUCCESS:
                    mProgressDialog.setStop();
                    Logger.d(TAG,"add_device_success");
                    toast("SuccessAddDevice");
                    startActivity(new Intent(InitDeviceActivity.this, DevicelistActivity.class));
                    finish();
                    break;

                case ON_LINE_SUCCESS:
                    //设备在线
                    Logger.d(TAG,"check_online_success");
                    if(devSc != null && devSc.length() == 8){
                        key = devSc;
                        bindDevice();
                    }else{
                        unBindDeviceInfo();
                    }
                    mProgressDialog.setStart(getResources().getString(R.string.binding_device));//绑定设备中
                    break;
                case ON_LINE_FAILED:
                    Logger.d(TAG,"check_online_failed");
                    toast("check_online_failed");
                    mHandler.removeCallbacksAndMessages(CHECK_DEVICE_ONLINE);
                    mProgressDialog.setStop();
                    showTipDialog(getResources().getString(R.string.toast_adddevice_check_online_failed));
                    break;
                case CHECK_DEVICE_ONLINE:
                    checkOnline();
                    break;
                case ON_LINE_TIMEOUT:
                    Logger.d(TAG,"check_online_timeout");
                    toast("check_online_timeout");
                    mHandler.removeCallbacksAndMessages(CHECK_DEVICE_ONLINE);
                    mProgressDialog.setStop();
                    showTipDialog(getResources().getString(R.string.toast_adddevice_check_online_timeout));
                    break;
            }
        }
    };


    private CommonTitle mCommonTitle;
    private ProgressDialog mProgressDialog; // 播放加载使用


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.init_device_activity);

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
        mProgressDialog = (ProgressDialog) this.findViewById(R.id.query_load);
        devId = getIntent().getStringExtra("devId");
        deviceInitInfo = (DeviceInitInfo) getIntent().getSerializableExtra("DeviceInitInfo");
        //取出SC码并做相应的处理
        devSc = getIntent().getStringExtra("devSc");
        if(devSc != null && devSc.length() == 8) {
            Log.e(TAG, " SC_initDevice, devSc : " + devSc);
            mProgressDialog.setStart(getResources().getString(R.string.checking_device_online));
            checkOnline();
        }else {//SC码小于8位走原来流程
            if (null != deviceInitInfo) {
                Log.d(TAG, "initDevice");
                mProgressDialog.setStart(getResources().getString(R.string.init_devices));
                initDevice(deviceInitInfo, curInitMode);
            }
        }
    }


    public void initDevice(final DeviceInitInfo deviceInitInfo, int initMode) {
        //1.使用组播进行初始化（initMode=INITMODE_MULTICAST）,走else流程
        //2.组播失败后再使用单播（initMode=INITMODE_UNICAST），此时直接使用组播时输入的秘钥进行初始化
        if (initMode == INITMODE_UNICAST) {
            Business.getInstance().initDeviceByIP(deviceInitInfo.mMac, deviceInitInfo.mIp, key, new Handler() {
                public void handleMessage(Message msg) {
                    String message = (String) msg.obj;
                    if (msg.what == 0) {
                        mHandler.obtainMessage(DEVICE_INIT_SUCCESS, message).sendToTarget();
                    } else {
                        mHandler.obtainMessage(DEVICE_INIT_BY_IP_FAILED, message).sendToTarget();
                    }
                }
            });
        } else {

            final EditText et = new EditText(this);
            final String deviceId = devId;

            final int status = deviceInitInfo.mStatus;

            //not support init
            if (status == 0 && !Business.getInstance().isOversea) {
                key = "";
                mHandler.obtainMessage(DEVICE_INIT_SUCCESS, "inner, go bind without key").sendToTarget();
            } else {
                if (status == 1) {
                    et.setHint(getString(R.string.toast_adddevice_input_device_key_to_init));
                } else {
                    et.setHint(getString(R.string.toast_adddevice_input_device_key_after_init));//输入设备初始化后的设备密码
                }

                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.alarm_message_keyinput_dialog_title)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setView(et)
                        .setPositiveButton(R.string.dialog_positive,
                                new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        key = et.getText().toString();
                                        if (status == 0 || status == 2) {
                                            if (Business.getInstance().isOversea)
                                                checkPwdValidity(deviceId, deviceInitInfo.mIp, deviceInitInfo.mPort, key, mHandler);
                                            else
                                                mHandler.obtainMessage(DEVICE_INIT_SUCCESS, "Inner, go bind with key").sendToTarget();
                                        } else if (status == 1) {
                                            Business.getInstance().initDevice(deviceInitInfo.mMac, key, new Handler() {
                                                public void handleMessage(Message msg) {
                                                    String message = (String) msg.obj;
                                                    if (msg.what == 0) {
                                                        mHandler.obtainMessage(DEVICE_INIT_SUCCESS, message).sendToTarget();
                                                    } else {
                                                        mHandler.obtainMessage(DEVICE_INIT_FAILED, message).sendToTarget();
                                                    }
                                                }
                                            });
                                        }
                                    }
                                })
                        .setNegativeButton(R.string.dialog_nagative,
                                new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mHandler.obtainMessage(DEVICE_INIT_FAILED, "Init has been cancelled").sendToTarget();
                                    }
                                })
                        .create();
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
            }
        }
    }


    /****
     * 海外设备需要校验密码
     * @param deviceId
     * @param ip
     * @param port
     * @param key
     * @param handler
     */
    public void checkPwdValidity(final String deviceId, final String ip, final int port, final String key, final Handler handler) {
        TaskPoolHelper.addTask(new TaskPoolHelper.RunnableTask("real") {
            @Override
            public void run() {
                if (0 == Business.getInstance().checkPwdValidity(deviceId, ip, port, key)) {
                    handler.obtainMessage(DEVICE_INIT_SUCCESS, "checkPwdValidity success").sendToTarget();
                } else {
                    handler.obtainMessage(DEVICE_INIT_FAILED, "checkPwdValidity failed").sendToTarget();
                }
            }
        });
    }


    private void toast(String content) {
        Toast.makeText(getApplicationContext(), content, Toast.LENGTH_LONG).show();
    }


    private void unBindDeviceInfo(){
        if(!Business.getInstance().isOversea){
            Business.getInstance().unBindDeviceInfo(devId, new Handler(){
                public void handleMessage(Message msg) {
                    String message = (String) msg.obj;
                    //				Log.d(tag, "unBindDeviceInfo,"+message);
                    if (msg.what == 0) {
                        if(message.contains("Auth")){
                            bindDevice();
                        }
                        else if(message.contains("RegCode")){
                            final EditText et = new EditText(InitDeviceActivity.this);
                            final AlertDialog dialog = new AlertDialog.Builder(InitDeviceActivity.this)
                                    .setTitle(R.string.alarm_message_keyinput_dialog_title)
                                    .setIcon(android.R.drawable.ic_dialog_info)
                                    .setView(et)
                                    .setPositiveButton(R.string.dialog_positive,null)
                                    .setNegativeButton(R.string.dialog_nagative, null)
                                    .create();
                            dialog.setCanceledOnTouchOutside(false);
                            dialog.show();

                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    if(TextUtils.isEmpty(et.getText())){
                                        toast("Input can't be empty");
                                        return;
                                    }
                                    key = et.getText().toString();
                                    bindDevice();
                                    dialog.dismiss();
                                }
                            });
                        }
                        else{
                            key = "";
                            bindDevice();
                        }
                    } else {
                        toast("unBindDeviceInfo failed");
                        Log.d(TAG, message);
                    }
                }
            });
        }
        else{ //oversea
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
                            mHandler.obtainMessage(ADD_DEVICE_SUCCESS).sendToTarget();
                        }else {
                            toast(getResources().getString(R.string.addDevice_failed)+"{ "+retObject.mMsg+" }");
                            Log.d(TAG, retObject.mMsg);
                            //添加失败后返回设备列表界面
                            startActivity(new Intent(InitDeviceActivity.this, DevicelistActivity.class));
                            finish();
                        }
                    }
                });
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
                                    toast("Online");
                                    mHandler.obtainMessage(ON_LINE_SUCCESS).sendToTarget();
                                } else {
                                    if(isCheckOnline) {
                                     Logger.d(TAG,"onLine : "+((DeviceOnline.Response) retObject.resp).data.onLine);
                                     mHandler.obtainMessage(CHECK_DEVICE_ONLINE).sendToTarget();
                                    }
                                }
                                break;
                            case -1000:
                                Logger.e(TAG,"check_online_respond : -1000");
                                mHandler.obtainMessage(ON_LINE_FAILED).sendToTarget();
                                break;
                            default:
                                Logger.e(TAG,"checkonline  :  "+msg.what);
                                mHandler.obtainMessage(ON_LINE_FAILED).sendToTarget();
                                break;
                        }
                    }
                });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mHandler!=null){
            mHandler.removeCallbacksAndMessages(null);
            mHandler=null;
        }
    }

    private  void  showTipDialog(String tip){

        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(tip).setNeutralButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        }).show();
    }
}