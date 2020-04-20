package com.jingna.videotest.lechange.business.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class WifiManagerUtil {
    private WifiManager mWifiManager;
    private WifiInfo mWifiInfo;
    private List<ScanResult> mWifiList;
    private List<WifiConfiguration> mWifiConfiguration;
    private WifiManager.WifiLock mWifiLock;
    private static int WIFI_TYPE_NO_PASSWORD = 1;
    private static int WIFI_TYPE_WEP = 2;
    private static int WIFI_TYPE_WPA = 3;

    @SuppressLint("MissingPermission")
    public WifiManagerUtil(Context context) {
        this.mWifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        this.mWifiInfo = this.mWifiManager.getConnectionInfo();
    }

    @SuppressLint("MissingPermission")
    public void openWifi() {
        if (!this.mWifiManager.isWifiEnabled()) {
            this.mWifiManager.setWifiEnabled(true);
        }

    }

    @SuppressLint("MissingPermission")
    public void closeWifi() {
        if (this.mWifiManager.isWifiEnabled()) {
            this.mWifiManager.setWifiEnabled(false);
        }

    }

    public boolean isWifi5G(Context context) {
        int freq = 0;
        if (Build.VERSION.SDK_INT > 21) {
            freq = this.mWifiInfo.getFrequency();
        } else {
            String ssid = this.mWifiInfo.getSSID();
            if (ssid != null && ssid.length() > 2) {
                String ssidTemp = ssid.substring(1, ssid.length() - 1);
                @SuppressLint("MissingPermission")
                List<ScanResult> scanResults = this.mWifiManager.getScanResults();
                Iterator var6 = scanResults.iterator();

                while(var6.hasNext()) {
                    ScanResult scanResult = (ScanResult)var6.next();
                    if (scanResult.SSID.equals(ssidTemp)) {
                        freq = scanResult.frequency;
                        break;
                    }
                }
            }
        }

        return freq > 4900 && freq < 5900;
    }

    @SuppressLint("MissingPermission")
    public int checkState() {
        return this.mWifiManager.getWifiState();
    }

    public void acquireWifiLock() {
        this.mWifiLock.acquire();
    }

    public void releaseWifiLock() {
        if (this.mWifiLock.isHeld()) {
            this.mWifiLock.acquire();
        }

    }

    public void creatWifiLock() {
        this.mWifiLock = this.mWifiManager.createWifiLock("Test");
    }

    public List<WifiConfiguration> getConfiguration() {
        return this.mWifiConfiguration;
    }

    @SuppressLint("MissingPermission")
    public void connectConfiguration(int index) {
        if (index <= this.mWifiConfiguration.size()) {
            this.mWifiManager.enableNetwork(((WifiConfiguration)this.mWifiConfiguration.get(index)).networkId, true);
        }
    }

    @SuppressLint("MissingPermission")
    public void startScan() {
        this.mWifiManager.startScan();
        this.mWifiList = this.mWifiManager.getScanResults();
        this.mWifiConfiguration = this.mWifiManager.getConfiguredNetworks();
    }

    public List<ScanResult> getWifiList() {
        return this.mWifiList;
    }

    public StringBuilder lookUpScan() {
        StringBuilder stringBuilder = new StringBuilder();

        for(int i = 0; i < this.mWifiList.size(); ++i) {
            stringBuilder.append("Index_" + (new Integer(i + 1)).toString() + ":");
            stringBuilder.append(((ScanResult)this.mWifiList.get(i)).toString());
            stringBuilder.append("/n");
        }

        return stringBuilder;
    }

    public String getMacAddress() {
        return this.mWifiInfo == null ? "NULL" : this.mWifiInfo.getMacAddress();
    }

    public String getBSSID() {
        return this.mWifiInfo == null ? "NULL" : this.mWifiInfo.getBSSID();
    }

    public int getIPAddress() {
        return this.mWifiInfo == null ? 0 : this.mWifiInfo.getIpAddress();
    }

    public int getNetworkId() {
        return this.mWifiInfo == null ? 0 : this.mWifiInfo.getNetworkId();
    }

    public String getWifiInfo() {
        return this.mWifiInfo == null ? "NULL" : this.mWifiInfo.toString();
    }

    @SuppressLint("MissingPermission")
    public boolean addNetwork(WifiConfiguration wcg) {
        int wcgID = this.mWifiManager.addNetwork(wcg);
        return this.mWifiManager.enableNetwork(wcgID, true);
    }

    @SuppressLint("MissingPermission")
    public void disconnectWifi(int netId) {
        this.mWifiManager.disableNetwork(netId);
        this.mWifiManager.disconnect();
    }

    @SuppressLint("MissingPermission")
    public WifiConfiguration createWifiInfo(String SSID, String Password, int Type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";
        WifiConfiguration tempConfig = this.IsExsits(SSID);
        if (tempConfig != null) {
            this.mWifiManager.removeNetwork(tempConfig.networkId);
        }

        if (Type == WIFI_TYPE_NO_PASSWORD) {
            config.wepKeys[0] = "\"\"";
            config.allowedKeyManagement.set(0);
            config.wepTxKeyIndex = 0;
        }

        if (Type == WIFI_TYPE_WEP) {
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + Password + "\"";
            config.allowedAuthAlgorithms.set(1);
            config.allowedGroupCiphers.set(3);
            config.allowedGroupCiphers.set(2);
            config.allowedGroupCiphers.set(0);
            config.allowedGroupCiphers.set(1);
            config.allowedKeyManagement.set(0);
            config.wepTxKeyIndex = 0;
        }

        if (Type == WIFI_TYPE_WPA) {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(0);
            config.allowedGroupCiphers.set(2);
            config.allowedKeyManagement.set(1);
            config.allowedPairwiseCiphers.set(1);
            config.allowedGroupCiphers.set(3);
            config.allowedPairwiseCiphers.set(2);
            config.status = 2;
        }

        return config;
    }

    @SuppressLint("MissingPermission")
    private WifiConfiguration IsExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = this.mWifiManager.getConfiguredNetworks();
        if (existingConfigs == null) {
            return null;
        } else {
            Iterator var3 = existingConfigs.iterator();

            WifiConfiguration existingConfig;
            do {
                if (!var3.hasNext()) {
                    return null;
                }

                existingConfig = (WifiConfiguration)var3.next();
            } while(!existingConfig.SSID.equals("\"" + SSID + "\""));

            return existingConfig;
        }
    }
    @SuppressLint("MissingPermission")
    public boolean connectWifi(String SSID, String passWord) {
        if (Build.VERSION.SDK_INT >= 23) {
            WifiConfiguration tempConfig = this.IsExsits(SSID);
            if (tempConfig != null) {
                return this.mWifiManager.enableNetwork(tempConfig.networkId, true);
            }
        }

        return this.addNetwork(this.createWifiInfo(SSID, passWord, WIFI_TYPE_NO_PASSWORD));
    }
    @SuppressLint("MissingPermission")
    public String getGatewayIp() {
        DhcpInfo dhcpInfo = this.mWifiManager.getDhcpInfo();
        if (dhcpInfo != null) {
            long gatewayIps = (long)dhcpInfo.gateway;
            return this.long2ip(gatewayIps);
        } else {
            return "";
        }
    }

    public String long2ip(long ip) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(String.valueOf((int)(ip & 255L)));
        stringBuffer.append('.');
        stringBuffer.append(String.valueOf((int)(ip >> 8 & 255L)));
        stringBuffer.append('.');
        stringBuffer.append(String.valueOf((int)(ip >> 16 & 255L)));
        stringBuffer.append('.');
        stringBuffer.append(String.valueOf((int)(ip >> 24 & 255L)));
        return stringBuffer.toString();
    }

    @SuppressLint("MissingPermission")
    public WifiInfo getCurrentWifiInfo() {
        this.mWifiInfo = this.mWifiManager.getConnectionInfo();
        return this.mWifiInfo;
    }
    @SuppressLint("MissingPermission")
    public  boolean isWifi(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        } else {
            NetworkInfo curNetwork = connectivity.getActiveNetworkInfo();
            if (curNetwork != null && curNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return true;
            }
        }
        return false;
    }

    public int getWifiEncryption(String capabilities) {
        int encryption = 255;
        String cap = capabilities.toUpperCase(Locale.US);
        if (cap.indexOf("WPA2") != -1) {
            if (cap.indexOf("WPA2-PSK-TKIP") != -1) {
                encryption = 6;
            } else if (cap.indexOf("WPA2-PSK-AES") != -1) {
                encryption = 7;
            } else if (cap.indexOf("WPA2-TKIP") != -1) {
                encryption = 10;
            } else if (cap.indexOf("WPA2-AES") != -1) {
                encryption = 11;
            } else if (cap.indexOf("WPA2-PSK-CCMP") != -1) {
                encryption = 12;
            }
        } else if (cap.indexOf("WPA") != -1) {
            if (cap.indexOf("WPA-PSK-TKIP") != -1) {
                encryption = 4;
            } else if (cap.indexOf("WPA-PSK-CCMP") != -1) {
                encryption = 5;
            } else if (cap.indexOf("WPA-TKIP") != -1) {
                encryption = 8;
            } else if (cap.indexOf("WPA-CCMP") != -1) {
                encryption = 9;
            }
        } else if (cap.indexOf("WEP") != -1) {
            if (cap.indexOf("WEP_Open") != -1) {
                encryption = 2;
            } else if (cap.indexOf("WEP_Shared") != -1) {
                encryption = 3;
            }
        } else {
            encryption = 255;
        }

        return encryption;
    }

    public ScanResult getScanResult() {
        ScanResult scanResult = null;
        if (this.mWifiManager == null) {
            return null;
        } else {
            this.getCurrentWifiInfo();
            if (this.mWifiInfo.getSSID() != null) {
                try {
                    if (this.mWifiList == null) {
                        this.startScan();
                    }

                    if (this.mWifiList == null) {
                        return null;
                    }

                    Iterator var2 = this.mWifiList.iterator();

                    while(var2.hasNext()) {
                        ScanResult s = (ScanResult)var2.next();
                        if (s.SSID.replaceAll("\"", "").equals(this.mWifiInfo.getSSID().replaceAll("\"", ""))) {
                            scanResult = s;
                            break;
                        }
                    }
                } catch (Exception var4) {
                    return null;
                }
            }

            return scanResult;
        }
    }

    public boolean isNoPasswordWifi() {
        ScanResult scanResult = this.getScanResult();
        if (scanResult == null) {
            return false;
        } else {
            int encypt = this.getWifiEncryption(scanResult.capabilities);
            return encypt == 2 || encypt == 255;
        }
    }

    public String getDoorbellSSID(String deviceSn) {
        return "Doorbell-" + deviceSn;
    }

    public boolean isConnectedDoorbellHot(String deviceSn) {
        WifiInfo wifiInfo = this.getCurrentWifiInfo();
        if (wifiInfo == null) {
            return false;
        } else {
            String ssid = this.getDoorbellSSID(deviceSn);
            ssid = "\"" + ssid + "\"";
            return wifiInfo.getSSID().equals(ssid);
        }
    }


}
