package com.example.nfcreceiver;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

public class WifiReceiver extends BroadcastReceiver {
    private SpeechSyn aksSpeech;
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        aksSpeech = new SpeechSyn(context);
        NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if(info != null && info.isConnected()) {
            aksSpeech.speaking("无线已经连接");
        }else{
            aksSpeech.speaking("无线配置错误，请重新配置网络信息后初始化按钮");
        }
    }
}
