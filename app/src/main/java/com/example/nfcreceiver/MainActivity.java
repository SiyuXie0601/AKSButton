package com.example.nfcreceiver;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Environment;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;


import com.example.nfcreceiver.NetPack.ThreadTask;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.util.ResourceUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;


public class MainActivity extends AppCompatActivity {

    NfcAdapter mNfcAdapter;
    private Context context;
    private ToggleButton mToggleButton;
    private Button buyButton;
    String userID;
    String SSID;
    String PWD;
    int category_id;
    SpeechSynthesizer mTts;
    private Toast mToast;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        API.init();
        setContentView(R.layout.activity_main);
        context = this;
        setAppId();
        InitSpeechSynthesizer();
        initView();//初始化控件方法
        readInfo();
        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            mTts.startSpeaking( "请开启NFC功能",mTtsListener );
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
        boolean connected = wifiManager.reconnect();
        if(connected){
            mTts.startSpeaking( "wifi已连接",mTtsListener );
        }
        else{
            mTts.startSpeaking( "wifi设置有误，请重新设置后再初始化按钮",mTtsListener );
        }
    }

    public void storeInfo(JSONObject JsonInfo){
        try {
            File filePath;
            FileWriter fWriter;
            filePath = new File(context.getExternalFilesDir(null), "info.json");
            fWriter = new FileWriter(filePath,false);
            try {
                JsonInfo.put("category_id",1);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            fWriter.write(JsonInfo.toString());
            fWriter.flush();
            mTts.startSpeaking( "按钮初始化成功",mTtsListener );
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
                category_id = (int)JsonObj.get("category_id");
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private boolean checkInfoComplete(JSONObject JsonInfo){
        if((!JsonInfo.has("SSID"))||(!JsonInfo.has("PWD"))){
            mTts.startSpeaking( "请在配置网络环境后再初始化按钮",mTtsListener );
            return false;
        }
        if(!JsonInfo.has("UserID")){
            mTts.startSpeaking( "请在注册后再初始化按钮",mTtsListener );
            return false;
        }
        return true;
    }

    private void initView() {
        mToggleButton = (ToggleButton) findViewById(R.id.togglebutton); //获取到控件
        mToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mTts.startSpeaking( "按钮电源已开启",mTtsListener );
                    readInfo();
                    WIFIConnect();
                } else {
                    mTts.startSpeaking( "按钮电源已关闭",mTtsListener );
                }
            }
        });
        buyButton = (Button) findViewById(R.id.button);
        buyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addOrder(userID,category_id);
            }
        });

    }

    private void addOrder(String userId, int categoryId) {
        ThreadTask task = API.addOrder(userId, categoryId, addButtonListener);
        if (task != null) {
            task.execute();
        }
    }

    ResponseListener addButtonListener = new ResponseListener() {
        @Override
        public void callback(String response) {
            try {
                JSONObject jsonObject = new JSONObject(response);
                if ((int) jsonObject.get("code") != 0) {
                    String msg = (String) jsonObject.get("msg");
                    mTts.startSpeaking( msg,mTtsListener );
                }
                else {
                    mTts.startSpeaking( "下单成功",mTtsListener );
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public void InitSpeechSynthesizer(){
        mTts= SpeechSynthesizer.createSynthesizer(context, mInitListener);
        //mToast=Toast.makeText(context, "", Toast.LENGTH_SHORT);
        //2.合成参数设置
        mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
        mTts.setParameter(SpeechConstant.SPEED, "50");//设置语速
        mTts.setParameter(SpeechConstant.VOLUME, "80");//设置音量，范围 0~100
        mTts.setParameter(ResourceUtil.TTS_RES_PATH, getResourcePath());//设置发音人资源路径
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL); //设置本地
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/xf_yuyin/tts.wav");
    }

    /**
     * 获取本地assets文件夹下的发音人资源路径
     */
    private String getResourcePath() {
        StringBuffer tempBuffer = new StringBuffer();
        //合成通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(context, ResourceUtil.RESOURCE_TYPE.assets, "tts/common.jet"));
        tempBuffer.append(";");
        //发音人资源
        tempBuffer.append(ResourceUtil.generateResourcePath(context, ResourceUtil.RESOURCE_TYPE.assets, "tts/"+ "xiaoyan" +".jet"));
        return tempBuffer.toString();
    }

    /**
     * 初始化监听
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int arg0) {
            // TODO Auto-generated method stub
            //Log.d(TAG, "InitListener init, code=" + arg0);
            if(arg0 != ErrorCode.SUCCESS) {
                toastMessage("初始化失败，错误码：" + arg0);
            } else {

            }
        }
    };

    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            //开始播放
        }

        @Override
        public void onSpeakPaused() {
            //暂停播放
        }

        @Override
        public void onSpeakResumed() {
            //"继续播放")
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            // 合成进度

        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度

        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) { //播放完成

            } else {
                toastMessage("code=" + error.getErrorCode() + ",msg=" + error.getErrorDescription());
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null

        }
    };
    public void toastMessage(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public void setAppId(){
        StringBuffer param = new StringBuffer();
        param.append(SpeechConstant.APPID +"=59672315");
        param.append(",");
        // 设置使用v5+
        param.append(SpeechConstant.ENGINE_MODE+"="+SpeechConstant.MODE_MSC);
        SpeechUtility.createUtility(context, param.toString());
    }

}
