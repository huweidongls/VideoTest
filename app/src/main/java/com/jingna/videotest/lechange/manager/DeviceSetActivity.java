package com.jingna.videotest.lechange.manager;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.jingna.videotest.R;
import com.jingna.videotest.lechange.business.Business;
import com.jingna.videotest.lechange.common.CommonTitle;
import com.jingna.videotest.lechange.login.SplashActivity;
import com.lechange.common.log.Logger;

public class DeviceSetActivity extends Activity {

	private String tag = "lcopen_demo_DeviceSetActivity";
	private SharedPreferences mSharedPreferences;
	private CommonTitle mCommonTitle;
	private ToggleButton mSwitch; // 报警订阅开关
	private ToggleButton mCloudMealBtn; // 云存储套餐开关
	private Button mModifyPwd; //修改设备密码
	private TextView mUpgrade; // 云升级开关
	private String mChannelUUID = null;
	private int mCloudMealStates; // 云套餐状态
	private int mAlarmStates; // 报警计划状态
	private boolean mCanBeUpgrade; //设备是否可云升级
	private boolean IsClickSwitchBtn; // 是否主动修改动检开关（手势触发）
	private boolean IsClickCloudMealBtn; // 是否主动修改套餐状态 （手势触发）
	private LinearLayout  mCloudStorageLL;
	private int platForm; //平台类型

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_set);

		mSharedPreferences = getSharedPreferences("OpenSDK",
				SplashActivity.MODE_PRIVATE);

		mChannelUUID = getIntent().getStringExtra("UUID"); // 获取通道的UUID

		mSwitch = (ToggleButton) findViewById(R.id.switchPlan);
		mCloudMealBtn = (ToggleButton) findViewById(R.id.cloudMealBtn);
//		mModifyPwd = (Button) findViewById(R.id.modify_device_pwd);
		mUpgrade = (TextView) findViewById(R.id.device_upgrade_icon);
		mCloudStorageLL= (LinearLayout) findViewById(R.id.cloudMeal);
		mSwitch.setClickable(false);
		mCloudMealBtn.setClickable(false);
		mUpgrade.setClickable(false);
		initTitle();
		setListener();
		getOriginStatus();

	}

	@Override
	protected void onDestroy() {

		super.onDestroy();
	}

	/**
	 * 初始化标题栏
	 */
	public void initTitle() {
		// 绘制标题
		mCommonTitle = (CommonTitle) findViewById(R.id.title);
		mCommonTitle.initView(R.drawable.title_btn_back, 0,
				R.string.devices_operation_name);

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
		//海外云存储
//		if(!Business.getInstance().isOversea){
		mCloudStorageLL.setVisibility(View.VISIBLE);
//		}
	}

	/**
	 * 获取初始状态
	 */
	private void getOriginStatus() {
//		 String status = mSharedPreferences.getString("alarmPlan", "");
//		 if (status != null) {
//		 mSwitch.setChecked(status.equals("open") ? true : false);
//		 } else {// 默认没有打开报警订阅
//		 mSwitch.setChecked(false);
//		 mSharedPreferences.edit().putString("alarmPlan", "close").commit();
//		 }
//		 mSwitch.setVisibility(View.VISIBLE);
//		 IsClickSwitchBtn = true;
//		 mSwitch.setClickable(true);

		// 初始化数据
		// TODO: 2019/10/31 初始化动检开关 V7.8.0切换新接口
		Business.getInstance().getAlarmAndCloudStatus(mChannelUUID, new Handler(){
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				Bundle bundle = (Bundle) msg.obj;
				if(msg.what == 0) {
					mAlarmStates = bundle.getInt("alarmStatus");/** [int]报警布撤防状态，0-撤防，1-布防 */
					Logger.e(tag, "server return [getDeviceInfo] mAlarmStates= " + mAlarmStates + ", mCloudMealStates= " + mCloudMealStates);

					if (mAlarmStates == 1) {
						IsClickSwitchBtn = false;
						mSwitch.setChecked(true);
					}
					mSwitch.setVisibility(View.VISIBLE);
				} else {
					Toast.makeText(DeviceSetActivity.this, "获取设备动检开关状态信息失败", Toast.LENGTH_SHORT).show();
				}
				IsClickSwitchBtn = true;
				mSwitch.setClickable(true);
			}
		});
		// TODO: 2019/10/31 初始化设备云存储状态 V7.8.0切新接口
		Business.getInstance().getStorageStrategy(mChannelUUID, new Handler(){
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				Bundle bundle = (Bundle) msg.obj;
				if(msg.what == 0) {
					mCloudMealStates = bundle.getInt("cloudStatus");
					if (mCloudMealStates == 1) {
						IsClickCloudMealBtn = false;
						mCloudMealBtn.setChecked(true);
					}
					mCloudMealBtn.setVisibility(View.VISIBLE);

				} else {
					Toast.makeText(DeviceSetActivity.this, "获取设备云存储开关信息失败", Toast.LENGTH_SHORT).show();
				}
				IsClickCloudMealBtn = true;
				mCloudMealBtn.setClickable(true);
			}
		});
		// TODO: 2019/10/31 初始化设备可否升级按钮 V7.8.0切新接口
		Business.getInstance().getDeviceUpgradeInfo(mChannelUUID, new Handler(){
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				Bundle bundle = (Bundle) msg.obj;
				if(msg.what == 0) {
					mCanBeUpgrade = bundle.getBoolean("canBeUpgrade");
					if (!mCanBeUpgrade) {
						mUpgrade.setVisibility(View.INVISIBLE);
					}

				} else {
					Toast.makeText(DeviceSetActivity.this, "获取设备可升级信息失败", Toast.LENGTH_SHORT).show();
				}
				mUpgrade.setClickable(true);
			}
		});
	}

	/**
	 * 设置监听函数
	 */
	public void setListener() {
		mSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton view, boolean state) {

				// 如果是手势触发，则进行网络请求
				if (IsClickSwitchBtn) {
					modifyAlarmPlan(state);
				}

			}
		});

		mCloudMealBtn.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton view, boolean state) {
				// 如果是手势触发，则进行网络请求
				if (IsClickCloudMealBtn) {
					//暂不支持此功能
//					Toast.makeText(DeviceSetActivity.this, "Not support now", Toast.LENGTH_SHORT).show();
					setStorageStrategy(state);
				}
			}
		});
		//隐藏设备密码修改功能
//		mModifyPwd.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				modifyDevicePassword();
//			}
//		});

		mUpgrade.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (!mCanBeUpgrade) {
					Toast.makeText(DeviceSetActivity.this, "No upgrade", Toast.LENGTH_SHORT).show();
					return;
				}
				Logger.d(tag, " start call UpgradeDevice()...");
				UpgradeDevice();
			}
		});
	}

	/**
	 * 设置云套餐状态
	 * 
	 * @param states
	 *            开启关闭状态
	 */
	public void setStorageStrategy(final boolean state) {

		Logger.e(tag, "====setStorageStrategy ,state="+state);
		mCloudMealBtn.setClickable(false);
		String states = null;
		if (state) {
			states = "on";
		} else {
			states = "off";
		}
		Business.getInstance().setStorageStartegy(states, mChannelUUID,
				new Handler() {

					@Override
					public void handleMessage(Message msg) {
						if (0 == msg.what) {
							Toast.makeText(
									DeviceSetActivity.this,
									R.string.toast_storagestrategy_update_success,
									Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(
									DeviceSetActivity.this,
									R.string.toast_storagestrategy_update_failed,
									Toast.LENGTH_SHORT).show();
							IsClickCloudMealBtn = false;
							mCloudMealBtn.setChecked(!state);
						}
						IsClickCloudMealBtn = true;
						mCloudMealBtn.setClickable(true);
					}
				});
	}

	/**
	 * 修改动检计划状态
	 * 
	 * @param enable
	 *            动检计划开启与否
	 */
	public void modifyAlarmPlan(final boolean enable) {

		Logger.e(tag, "modifyAlarmPlan ,state="+enable);
		mSwitch.setClickable(false);
		Business.getInstance().modifyAlarmStatus(enable, mChannelUUID,
				new Handler() {

					@Override
					public void handleMessage(Message msg) {
						if (0 == msg.what) {
							Toast.makeText(DeviceSetActivity.this,
									R.string.toast_alarmplan_modifyalarmstatus_success,
									Toast.LENGTH_LONG).show();
							
							mSharedPreferences
									.edit()
									.putString("alarmPlan",
											enable ? "open" : "close").commit();
						} else {
							Toast.makeText(DeviceSetActivity.this,
									R.string.toast_alarmplan_modifyalarmstatus_failed,
									Toast.LENGTH_LONG).show();
							IsClickSwitchBtn = false;
							mSwitch.setChecked(!enable);
						}
						IsClickSwitchBtn = true;
						mSwitch.setClickable(true);
					}

				});
	}


	/**
	 * 修改设备密码
	 */
//	public void modifyDevicePassword() {
//		LinearLayout layout = (LinearLayout) LinearLayout.inflate(this, R.layout.dialog_modify_device_password, null);
//		final EditText oldPwd = (EditText) layout.findViewById(R.id.old_pwd);
//		final EditText newPwd = (EditText) layout.findViewById(R.id.new_pwd);
//		new AlertDialog.Builder(DeviceSetActivity.this)
//				.setTitle(R.string.alarm_message_keyinput_dialog_title)
//				.setIcon(android.R.drawable.ic_dialog_info)
//				.setView(layout)
//				.setPositiveButton(R.string.dialog_positive,
//						new android.content.DialogInterface.OnClickListener() {
//							@Override
//							public void onClick(DialogInterface dialog, int which) {
//								// TODO Auto-generated method stub
//								String oldKey = oldPwd.getText().toString();
//								String newKey = newPwd.getText().toString();
//								String deviceId = Business.getInstance().getChannel(mChannelUUID).getDeviceCode();
//
//								Business.getInstance().modifyDevicePwd(deviceId, oldKey, newKey, new Handler(){
//									@Override
//									public void handleMessage(Message msg) {
//										RetObject retObject = (RetObject) msg.obj;
//										if (msg.what == 0) {
//											Toast.makeText(DeviceSetActivity.this, "modigyDevicePwd success", Toast.LENGTH_SHORT).show();
//										} else {
//											Toast.makeText(DeviceSetActivity.this, retObject.mMsg, Toast.LENGTH_SHORT).show();
//										}
//									}
//								});
//							}
//				}).setNegativeButton(R.string.dialog_nagative, null).show();
//	}
	
	/**
	 * 设备云升级
	 */
	public void UpgradeDevice() {

		mUpgrade.setClickable(false);
		Business.getInstance().upgradeDevice(mChannelUUID, new Handler() {

			@Override
			public void handleMessage(Message msg) {
				if (0 == msg.what) {
					Toast.makeText(DeviceSetActivity.this,
							R.string.toast_cloudupdate_success,
							Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(DeviceSetActivity.this,
							R.string.toast_cloudupdate_failed,
							Toast.LENGTH_LONG).show();
				}
				mUpgrade.setClickable(true);
			}

		});
	}
}
