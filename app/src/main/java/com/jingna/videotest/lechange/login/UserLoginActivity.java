package com.jingna.videotest.lechange.login;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.jingna.videotest.R;
import com.jingna.videotest.lechange.business.Business;
import com.jingna.videotest.lechange.common.CommonTitle;
import com.jingna.videotest.lechange.listview.DevicelistActivity;

public class UserLoginActivity extends Activity{

	private String tag = "UserLoginActivity";
	private EditText mZoomEdit;
	private EditText mPhoneEdit;
	private Button mBindUserBtn;
	private Button mDeviceListBtn;
	private TextView notice;
	private CommonTitle mCommonTitle;
	private SharedPreferences sp; //固化数据

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_user_login);
		sp = this.getSharedPreferences("OpenSDK", MODE_PRIVATE);
		initView();
		initTitle();
		setListener();
	}

	public void initView()
	{
		mZoomEdit = (EditText) findViewById(R.id.zoneView);
		mPhoneEdit = (EditText) findViewById(R.id.phoneEdit);
		//区分国内与海外
		//if(getResources().getConfiguration().locale.getLanguage().endsWith("zh")) {
		if(!Business.getInstance().isOversea) {
			mZoomEdit.setText(sp.getString("zone", "+86"));
			mPhoneEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
			mPhoneEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(11)});
			mPhoneEdit.setText(sp.getString("userphonenumber", ""));
		}else{
			mZoomEdit.setText(sp.getString("zone", "Email"));
			mZoomEdit.setTextSize(17);
			mPhoneEdit.setTextSize(17);
			mPhoneEdit.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
			mPhoneEdit.setText(sp.getString("userEmailAddress", ""));
		}

		
		mBindUserBtn = (Button) findViewById(R.id.bindUser);
		mDeviceListBtn = (Button) findViewById(R.id.deviceList);
		notice = (TextView) findViewById(R.id.notice);
	}
	public void initTitle()
	{
		//绘制标题
		mCommonTitle = (CommonTitle) findViewById(R.id.title);
		mCommonTitle.initView(R.drawable.title_btn_back, 0, R.string.user_login_name);
		
		mCommonTitle.setOnTitleClickListener(new CommonTitle.OnTitleClickListener() {
			@Override
			public void onCommonTitleClick(int id) {
				// TODO Auto-generated method stub
				switch (id) {
				case CommonTitle.ID_LEFT:
					finish();
				}
			}
		});
	}
	public void setListener()
	{
		mBindUserBtn.setOnClickListener(new OnClickListener() {	
			@Override
			public void onClick(View arg0) {
				userLogin(mBindUserBtn.getId());
			}
		});
		
		mDeviceListBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {			
				userLogin(mDeviceListBtn.getId());
			}
		});
		
		
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	

	/**
	 * 获取usertoken
	 */
	public void userLogin(final int id){
		notice.setVisibility(View.INVISIBLE);
		String zone = mZoomEdit.getText().toString().trim();
		String phoneNumber = mPhoneEdit.getText().toString().trim();

		//if(getResources().getConfiguration().locale.getLanguage().endsWith("zh")) {
		if(!Business.getInstance().isOversea) {
			//国内
			if(phoneNumber.length() != 11)
			{
				notice.setText(R.string.user_no_input);
				notice.setVisibility(View.VISIBLE);
				return;
			}

			mBindUserBtn.setClickable(false);
			mDeviceListBtn.setClickable(false);

			Editor editor=sp.edit();
			editor.putString("userphonenumber", phoneNumber);
			editor.putString("zone", zone);
			editor.commit();
			if(!zone.endsWith("86") && !zone.equals("") ) {
				phoneNumber = zone + phoneNumber;
			}
		} else {
			//海外
//			if(!isEmail(phoneNumber))
//			{
//				notice.setText(R.string.user_no_input_en);
//				notice.setVisibility(View.VISIBLE);
//				return;
//			}
//
//			mBindUserBtn.setClickable(false);
//			mDeviceListBtn.setClickable(false);
//
//			Editor editor=sp.edit();
//			editor.putString("userEmailAddress", phoneNumber);
//			editor.putString("zone", zone);
//			editor.commit();
////			if(!zone.endsWith("86") && !zone.equals("") ) {
////				phoneNumber = zone + phoneNumber;
////			}
		}

		
		
		Business.getInstance().userlogin(phoneNumber, new Handler(){
			@Override
			public void handleMessage(Message msg) {				
				if(0 == msg.what)//绑定成功
				{
					switch(id){
					case R.id.bindUser:
						notice.setText(R.string.user_bind_err);
						notice.setVisibility(View.VISIBLE);
						break;
					case R.id.deviceList:
						String userToken = (String) msg.obj;
						Log.d(tag,"userToken" + userToken);
						Business.getInstance().setToken(userToken);
						startActivity();
						break;
					}							
				}
				else
				{
					switch(id){
					case R.id.bindUser:
						if(1 == msg.what)////手机号与当前应用还未绑定
							startBindUserActivity();
						else{
							String result = (String)msg.obj;
							notice.setText(result);
							notice.setVisibility(View.VISIBLE);
						}
						break;
					case R.id.deviceList:
						if(1 != msg.what){
							String result = (String)msg.obj;
							notice.setText(result);
						}else{
							notice.setText(R.string.user_nobind_err);
						}
						
						notice.setVisibility(View.VISIBLE);
						break;
					}
				}
				mBindUserBtn.setClickable(true);
				mDeviceListBtn.setClickable(true);
			}
			
		});
	}
	
	public void startBindUserActivity()
	{
		Intent intent = new Intent(UserLoginActivity.this, BindUserActivity.class );
		intent.putExtra("phoneNumber", mPhoneEdit.getText().toString());
		startActivity(intent);
	}

	/**
	 * 跳转到主页
	 */
	public void startActivity(){
		Intent mIntent = new Intent(this, DevicelistActivity.class);
		Bundle b = getIntent().getExtras();
		if (b != null) {
			mIntent.putExtras(getIntent().getExtras());
		}
		startActivity(mIntent);
		finish();
	}

	/**
	 * 校验邮箱格式是否正确
	 */
	public static boolean isEmail(String str){
		boolean isEmail = false;
		String expr = "^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})$";
		if(str.matches(expr)) {
			isEmail = true;
		}
		return isEmail;
	}
}
