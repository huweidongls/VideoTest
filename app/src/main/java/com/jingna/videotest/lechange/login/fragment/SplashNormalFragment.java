package com.jingna.videotest.lechange.login.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.jingna.videotest.R;
import com.jingna.videotest.lechange.business.Business;
import com.jingna.videotest.lechange.listview.DevicelistActivity;
import com.jingna.videotest.lechange.login.SplashActivity;
import com.jingna.videotest.lechange.login.UserLoginActivity;
import com.lechange.common.log.Logger;

public class SplashNormalFragment extends Fragment{

    private EditText appId = null;
    private EditText appSecret = null;
    private EditText appUrl = null;
    private ImageView adminBtn = null;
    private ImageView userBtn = null;
    private SharedPreferences sp; //固化数据
    private Activity mActivity;
    private boolean isAdminLogin=false;


    public String getAppId() {
        return appId.getText().toString();
    }



    public String getAppSecret() {
        return appSecret.getText().toString();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        sp =  getActivity().getSharedPreferences("OpenSDK", SplashActivity.MODE_PRIVATE);
        return inflater.inflate(R.layout.fragment_splash_normal, container,false);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initView();
        setListener();
        appId.setText(sp.getString("appid", ""));
        appSecret.setText(sp.getString("appsecret", ""));
        appUrl.setText(sp.getString("appurl", "openapi.lechange.cn:443"));
//		appUrl.setVisibility(View.INVISIBLE);

        
    }


    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    private void initView()
    {
        mActivity = getActivity();

        appId = (EditText) getView().findViewById(R.id.editAppId);
        appSecret = (EditText) getView().findViewById(R.id.editAppSectet);
        appUrl = (EditText) getView().findViewById(R.id.editappurl);
        adminBtn = (ImageView) getView().findViewById(R.id.adminButton);
        userBtn = (ImageView) getView().findViewById(R.id.userButton);
        //英文屏蔽用户登录按钮
//        if(!getResources().getConfiguration().locale.getLanguage().endsWith("zh")){
//            userBtn.setVisibility(View.GONE);
//        }
    }

    private void setListener()
    {
        adminBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                isAdminLogin=true;
                requestLocationPermission();
            }

        });

        userBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                isAdminLogin=false;
                requestLocationPermission();
            }

        });
    }

    private void  adminLogin() {
        if(!validCheck())
        {
            SplashActivity splashActivity = (SplashActivity) getActivity();
            splashActivity.changeFragment();
            return;
        }
        //按钮不能点击，防止连击开启多个activity；也可以修改启动模式
        adminBtn.setClickable(false);
//        ClientEnvironment.setClientSafeCode("");
        //初始化需要的数据
        Business.getInstance().init(appId.getText().toString(), appSecret.getText().toString(), appUrl.getText().toString());
        Editor editor=sp.edit();
        editor.putString("appid", appId.getText().toString());
        editor.putString("appsecret", appSecret.getText().toString());
        editor.putString("appurl", appUrl.getText().toString());
        editor.commit();
        Business.getInstance().adminlogin(new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if(0 == msg.what)
                {
                    String accessToken = (String) msg.obj;
                    Business.getInstance().setToken(accessToken);

                    //恢复可点击
                    adminBtn.setClickable(true);
                    startActivity();
                }
                else{
                    if(1 == msg.what){
                        Toast.makeText(mActivity, "getToken failed", Toast.LENGTH_SHORT).show();
                    }else{
                        String result = (String)msg.obj;
                        Toast.makeText(mActivity, result, Toast.LENGTH_SHORT).show();
                    }
                    //恢复可点击
                    adminBtn.setClickable(true);
                }
            }

        });
    }


    private void UserLogin(){
        if(!validCheck())
        {
            SplashActivity splashActivity = (SplashActivity) getActivity();
            splashActivity.changeFragment();
            return;
        }

        //初始化需要的数据
        Business.getInstance().init(appId.getText().toString(), appSecret.getText().toString(), appUrl.getText().toString());

        Editor editor=sp.edit();
        editor.putString("appid", appId.getText().toString());
        editor.putString("appsecret", appSecret.getText().toString());
        editor.putString("appurl", appUrl.getText().toString());
        editor.commit();

        Intent intent = new Intent(getActivity(),UserLoginActivity.class );
        getActivity().startActivity(intent);
    }

    private void startActivity() {
        Intent mIntent = new Intent(this.getActivity(), DevicelistActivity.class);
        Bundle b = mActivity.getIntent().getExtras();
        if (b != null) {
            mIntent.putExtras(mActivity.getIntent().getExtras());
        }
        startActivity(mIntent);
    }

    private boolean validCheck()
    {

        if(appId.getText().length()==0 || appSecret.getText().length() == 0)
        {
            return false;
        }
        return true;
    }

	/**
	 * 请求定位权限
	 *
	 */
	private void requestLocationPermission(){
		boolean isMinSDKM = Build.VERSION.SDK_INT < 23;

		boolean isGranted = false;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			isGranted = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
		}
		if (isMinSDKM || isGranted) {
            if (isAdminLogin) {
                adminLogin();
            } else {
                UserLogin();
            }
            // 定位权限
			return;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {

                Logger.d("Uriah", "Uriah + shouldShowRequestPermission true");
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }else{
                Logger.d("Uriah", "Uriah + shouldShowRequestPermission false");
//                Toast.makeText(getActivity(),getResources().getString(R.string.toast_permission_location_forbidden),Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
		}

	}


	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		// TODO Auto-generated method stub
		if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			if (requestCode == 1) {
                if (isAdminLogin) {
                    adminLogin();
                } else {
                    UserLogin();
                }
			}

		} else {
			if (requestCode == 1) {
				Toast.makeText(getActivity(),getResources().getString(R.string.toast_permission_location_forbidden),Toast.LENGTH_SHORT).show();
			}

		}

	}

}
