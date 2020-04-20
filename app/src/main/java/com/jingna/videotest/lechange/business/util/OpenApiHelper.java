package com.jingna.videotest.lechange.business.util;

import android.os.Handler;
import android.util.Log;

import org.json.JSONObject;


public class OpenApiHelper {

    public final static String tag = "OpenApiHelper";


    public static void getAccessToken(final String host,
                                      final String phoneNumber, final String appid,
                                      final String appsecret, final Handler handler) {
        // 拼装url
        String url = "";
        if (host.endsWith(":443")) {
            url = "https://" + host + "/openapi/accessToken";
        } else {
            url = "http://" + host + "/openapi/accessToken";
        }
        Log.d(tag, url);
        getToken(url, phoneNumber, appid, appsecret, handler);
    }

    public static void getUserToken(final String host,
                                    final String phoneNumber, final String appid,
                                    final String appsecret, final Handler handler) {
        // 拼装url
        String url = "";
        if (host.endsWith(":443")) {
            url = "https://" + host + "/openapi/userToken";
        } else {
            url = "http://" + host + "/openapi/userToken";
        }
        Log.d(tag, url);
        getToken(url, phoneNumber, appid, appsecret, handler);
    }
    //
    public static void getUserTokenByAccount(final String host,
                                    final String phoneNumber, final String appid,
                                    final String appsecret, final Handler handler) {
        // 拼装url
        String url = "";
        if (host.endsWith(":443")) {
            url = "https://" + host + "/openapi/userTokenByAccount";
        } else {
            url = "http://" + host + "/openapi/userTokenByAccount";
        }
        Log.d(tag, url);
        userTokenByAccount(url, phoneNumber, appid, appsecret, handler);
    }


    public static void userBindSms(final String host, final String phoneNumber,
                                   final String appid, final String appsecret, final Handler handler) {
        String url = "";
        if (host.endsWith(":443")) {
            url = "https://" + host + "/openapi/userBindSms";
        } else {
            url = "http://" + host + "/openapi/userBindSms";
        }
        Log.d(tag, url);

        int code = -1;
        String result = null;
        try {
            JSONObject body = new JSONObject();
            String data = "{phone: \"" + phoneNumber + "\"}";
            body.put("params", new JSONObject(data));
            body.put("id", "1");// id号 随机值

            body.put(
                    "system",
                    new JSONObject(SignHelper.getSystem(data, appid, appsecret,
                            "1.1")));

            String response = HttpUtils.getInstance().postString(url, body.toString());

            JSONObject res = new JSONObject((String) response);
            if (res.getJSONObject("result").getString("code").equals("0")) {
                code = 0;
                result = res.getJSONObject("result").getString("msg");
            } else {
                code = -1;
                result = res.getJSONObject("result").getString("msg");

            }

        } catch (Exception e) {
            e.printStackTrace();
            result = e.getMessage();
        }

        handler.obtainMessage(code, result).sendToTarget();

    }

    public static void userBind(final String host, final String phoneNumber,
                                final String appId, String appSecret, final String smsCode,
                                final Handler handler) {
        String url = "";
        if (host.endsWith(":443")) {
            url = "https://" + host + "/openapi/userBind";
        } else {
            url = "http://" + host + "/openapi/userBind";
        }
        Log.d(tag, url);


        int code = -1;
        String result = null;
        try {
            JSONObject body = new JSONObject();
            String data = "{phone: \"" + phoneNumber + "\",smsCode:\"" + smsCode
                    + "\"}";
            body.put("params", new JSONObject(data));
            body.put("id", "1");// id号 随机值
            body.put(
                    "system",
                    new JSONObject(SignHelper.getSystem(data, appId, appSecret,
                            "1.1")));


            String response = HttpUtils.getInstance().postString(url, body.toString());
            Log.d("Uriah", "response" + response);
            JSONObject res = new JSONObject((String) response);
            if (res.getJSONObject("result").getString("code").equals("0")) {
                code = 0;
                result = res.getJSONObject("result").getString("msg");
            } else {
                code = -1;
                result = res.getJSONObject("result").getString("msg");

            }

        } catch (Exception e) {
            e.printStackTrace();
            result = e.getMessage();
        }

        handler.obtainMessage(code, result).sendToTarget();
    }

    /**
     * 账号绑定无验证码方式（新接口）
     * @param host
     * @param phoneNumber
     * @param appId
     * @param appSecret
     * @param smsCode
     * @param handler
     */
    public static void userBindNoVerify(final String host, final String phoneNumber,
                                final String appId, String appSecret, final String smsCode,
                                final Handler handler) {
        String url = "";
        if (host.endsWith(":443")) {
            url = "https://" + host + "/openapi/userBindNoVerify";
        } else {
            url = "http://" + host + "/openapi/userBindNoVerify";
        }
        Log.d(tag, url);


        int code = -1;
        String result = null;
        try {


            JSONObject body = new JSONObject();
            String data = "{account: \"" + phoneNumber + "\"}";
            body.put("params", new JSONObject(data));
            body.put("id", "1");// id号 随机值
            body.put(
                    "system",
                    new JSONObject(SignHelper.getSystem(data, appId, appSecret,
                            "1.1")));


            String response = HttpUtils.getInstance().postString(url, body.toString());
            Log.d("Uriah", "response" + response);
            JSONObject res = new JSONObject((String) response);
            if (res.getJSONObject("result").getString("code").equals("0")) {
                code = 0;
                result = res.getJSONObject("result").getString("msg");
            } else {
                code = -1;
                result = res.getJSONObject("result").getString("msg");

            }

        } catch (Exception e) {
            e.printStackTrace();
            result = e.getMessage();
        }

        handler.obtainMessage(code, result).sendToTarget();
    }

    private static void getToken(final String host, final String phoneNumber,
                                 final String appid, final String appsecret, final Handler handler) {

        int code = -1;
        String result = null;
        try {
            JSONObject body = new JSONObject();
            String data = "{phone:\"" + phoneNumber + "\"}";
            body.put("params", new JSONObject(data));
            body.put("id", "1");// id号 随机值
            body.put(
                    "system",
                    new JSONObject(SignHelper.getSystem(data, appid, appsecret,
                            "1.1")));

            String response = HttpUtils.getInstance().postString(host, body.toString());

            if(response != null){
	            JSONObject res = new JSONObject((String) response);
	            if (res.getJSONObject("result").getString("code").equals("0")) {
	                code = 0;
	                if (res.getJSONObject("result").getJSONObject("data")
	                        .has("accessToken")) {
	                    result = res.getJSONObject("result")
	                            .getJSONObject("data")
	                            .getString("accessToken");
	                } else {
	                    result = res.getJSONObject("result")
	                            .getJSONObject("data")
	                            .getString("userToken");
	                }
	            } else {
	                code = -1;
	                result = res.getJSONObject("result").getString("msg");
	                // 界面展示和业务需要 特殊处理
	                if (res.getJSONObject("result").getString("code")
	                        .equals("TK1004"))
	                    code = 1;
	                if (res.getJSONObject("result").getString("code")
	                        .equals("TK1006"))
	                    code = 1;
	
	            }
            }else{
            	code = -1;
            	result = "get Token failed,Response is null";
            }

        } catch (Exception e) {
            e.printStackTrace();
            result = e.getMessage();
        }

        handler.obtainMessage(code, result).sendToTarget();

    }

    /**
     * 根据账号获取用户token（新接口）
     * @param host
     * @param phoneNumber
     * @param appid
     * @param appsecret
     * @param handler
     */
    private static void userTokenByAccount(final String host, final String phoneNumber,
                                 final String appid, final String appsecret, final Handler handler) {
        int code = -1;
        String result = null;
        try {
            JSONObject body = new JSONObject();
            String data = "{account:\"" + phoneNumber + "\"}";
            body.put("params", new JSONObject(data));
            body.put("id", "1");// id号 随机值
            body.put(
                    "system",
                    new JSONObject(SignHelper.getSystem(data, appid, appsecret,
                            "1.1")));

            String response = HttpUtils.getInstance().postString(host, body.toString());

            if(response != null){
                JSONObject res = new JSONObject((String) response);
                if (res.getJSONObject("result").getString("code").equals("0")) {
                    code = 0;
                    if (res.getJSONObject("result").getJSONObject("data")
                            .has("accessToken")) {
                        result = res.getJSONObject("result")
                                .getJSONObject("data")
                                .getString("accessToken");
                    } else {
                        result = res.getJSONObject("result")
                                .getJSONObject("data")
                                .getString("userToken");
                    }
                } else {
                    code = -1;
                    result = res.getJSONObject("result").getString("msg");
                    // 界面展示和业务需要 特殊处理
                    if (res.getJSONObject("result").getString("code")
                            .equals("TK1004"))
                        code = 1;
                    if (res.getJSONObject("result").getString("code")
                            .equals("TK1006"))
                        code = 1;

                }
            }else{
                code = -1;
                result = "get Token failed,Response is null";
            }

        } catch (Exception e) {
            e.printStackTrace();
            result = e.getMessage();
        }

        handler.obtainMessage(code, result).sendToTarget();

    }

    private static void getTokenByEmail(final String host, final String email,
                                 final String appid, final String appsecret, final Handler handler) {

        int code = -1;
        String result = null;
        try {
            JSONObject body = new JSONObject();
            String data = "{email:\"" + email + "\"}";
            body.put("params", new JSONObject(data));
            body.put("id", "1");// id号 随机值
            body.put(
                    "system",
                    new JSONObject(SignHelper.getSystem(data, appid, appsecret,
                            "1.1")));

            String response = HttpUtils.getInstance().postString(host, body.toString());

            if(response != null){
                JSONObject res = new JSONObject((String) response);
                if (res.getJSONObject("result").getString("code").equals("0")) {
                    code = 0;
                    if (res.getJSONObject("result").getJSONObject("data")
                            .has("accessToken")) {
                        result = res.getJSONObject("result")
                                .getJSONObject("data")
                                .getString("accessToken");
                    } else {
                        result = res.getJSONObject("result")
                                .getJSONObject("data")
                                .getString("userToken");
                    }
                } else {
                    code = -1;
                    result = res.getJSONObject("result").getString("msg");
                    // 界面展示和业务需要 特殊处理
                    if (res.getJSONObject("result").getString("code")
                            .equals("TK1004"))
                        code = 1;
                    if (res.getJSONObject("result").getString("code")
                            .equals("TK1006"))
                        code = 1;

                }
            }else{
                code = -1;
                result = "get Token failed,Response is null";
            }

        } catch (Exception e) {
            e.printStackTrace();
            result = e.getMessage();
        }

        handler.obtainMessage(code, result).sendToTarget();

    }

}



