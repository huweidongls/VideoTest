package com.jingna.videotest.app;

import android.app.Application;

import com.lechange.opensdk.utils.LogUtils;

/**
 * Created by Administrator on 2020/4/20.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        /*******Android 4.4 等部分版本需要在此处特殊处理，单独加载so库文件 *****/
        try {
            System.loadLibrary("netsdk");
            System.loadLibrary("configsdk");
            System.loadLibrary("jninetsdk");
            System.loadLibrary("gnustl_shared");
            System.loadLibrary("LechangeSDK");
            System.loadLibrary("SmartConfig");
            //日志开关
            LogUtils.OpenLog();
        } catch (Exception var1) {
            System.err.println("loadLibrary Exception"+var1.toString());
        } catch (Error var2) {
            System.err.println("loadLibrary Exception"+var2.toString());
        }

    }

}
