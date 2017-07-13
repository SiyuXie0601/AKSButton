package com.example.nfcreceiver;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;


import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;


public class MainActivity extends AppCompatActivity {

    NfcAdapter mNfcAdapter;
    Context context;
    private ToggleButton mToggleButton;
    String userID;
    String SSID;
    String PWD;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        initView();//初始化控件方法
        readInfo();
        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }


    //Parses the NDEF Message from the intent, connect wifi and store the info file
    public void processIntent(Intent intent) {
        String wholeJson;
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        wholeJson = new String(msg.getRecords()[0].getPayload());
        //retrieve info from wholeJson String
        try {
            //store info
            JSONObject JsonObj = new JSONObject(wholeJson);
            if(checkInfoComplete(JsonObj)){
                storeInfo(JsonObj);
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void WIFIConnect(){
        WifiConfiguration wifiConfig = new WifiConfiguration();
        Toast.makeText(this, PWD, Toast.LENGTH_LONG).show();
        Toast.makeText(this, SSID, Toast.LENGTH_LONG).show();
        Toast.makeText(this, userID, Toast.LENGTH_LONG).show();
        wifiConfig.SSID = String.format("\"%s\"", SSID);
        wifiConfig.preSharedKey = String.format("\"%s\"", PWD);
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        int netId = wifiManager.addNetwork(wifiConfig);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
        Toast.makeText(this, "试图连接wifi", Toast.LENGTH_LONG).show();
    }

    public void storeInfo(JSONObject JsonInfo){
        try {
            File filePath;
            FileWriter fWriter;
            filePath = new File(context.getExternalFilesDir(null), "info.json");
            fWriter = new FileWriter(filePath,false);
            fWriter.write(JsonInfo.toString());
            fWriter.flush();
        }catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public void readInfo(){
        try {
            //read
            File filePath;
            filePath = new File(context.getExternalFilesDir(null), "info.json");
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(new FileReader(filePath));
            JSONObject JsonObj = new JSONObject(obj.toString());
            if(checkInfoComplete(JsonObj)){
                userID = (String)JsonObj.get("UserID");
                SSID = (String)JsonObj.get("SSID");
                PWD = (String)JsonObj.get("PWD");
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private boolean checkInfoComplete(JSONObject JsonInfo){
        if((!JsonInfo.has("SSID"))||(!JsonInfo.has("PWD"))){
            Toast.makeText(this, "请在配置网络环境后再初始化按钮", Toast.LENGTH_LONG).show();
            return false;
        }
        if(!JsonInfo.has("UserID")){
            Toast.makeText(this, "请在注册后再初始化按钮", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void initView() {
        mToggleButton = (ToggleButton) findViewById(R.id.togglebutton); //获取到控件
        mToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    readInfo();
                    WIFIConnect();
                } else {
                    // The toggle is disabled
                }
            }
        });
    }
}
