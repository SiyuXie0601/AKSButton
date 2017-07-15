package com.example.nfcreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Environment;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
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
import java.io.IOException;

import static android.net.NetworkInfo.DetailedState.CONNECTED;


public class MainActivity extends AppCompatActivity {

    NfcAdapter mNfcAdapter;
    SpeechSyn aksSpeech;
    InfoIO infoIO;
    WifiConnection wifiConnecter;
    private Context context;
    private ToggleButton mToggleButton;
    private ImageView buyButton;
    String userid;
    String ssid;
    String pwd;
    int categoryid;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        API.init();
        setContentView(R.layout.activity_main);
        this.context = this;
        aksSpeech = new SpeechSyn(context);
        try {
            infoIO = new InfoIO("info.json", context, aksSpeech);
            wifiConnecter = new WifiConnection(context, aksSpeech);
        }catch(IOException e){

        }
        initView();//初始化控件方法
        infoIO.readInfo();
        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            aksSpeech.speaking("请开启NFC功能");
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
                infoIO.storeInfo(JsonObj);
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void initView() {
        mToggleButton = (ToggleButton) findViewById(R.id.togglebutton); //获取到控件
        mToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    loadInfo();
                } else {

                }
            }
        });
        buyButton = (ImageView) findViewById(R.id.button);
        buyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if(activeNetwork.getDetailedState()==CONNECTED){
                    addOrder(userid, categoryid);
                }
                else{
                    aksSpeech.speaking("无线设置有误，请重新设置后再初始化按钮");
                }
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
                    aksSpeech.speaking(msg);
                }
                else {
                    aksSpeech.speaking("下单成功");
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private boolean checkInfoComplete(JSONObject JsonInfo){
        if((!JsonInfo.has("SSID"))||(!JsonInfo.has("PWD"))){
            aksSpeech.speaking("请在配置网络环境后再初始化按钮");
            return false;
        }
        if(!JsonInfo.has("UserID")){
            aksSpeech.speaking("请在注册后再初始化按钮");
            return false;
        }
        return true;
    }

    private void loadInfo(){
        JSONObject jsonObject = infoIO.readInfo();
        if(checkInfoComplete(jsonObject)) {
            try {
                userid = (String) jsonObject.get("UserID");
                ssid = (String) jsonObject.get("SSID");
                pwd = (String) jsonObject.get("PWD");
                categoryid = (int) jsonObject.get("category_id");
                wifiConnecter.WIFIConnect(ssid, pwd);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}


class SpeechSyn{
    private Context context;
    private String app_id = "=59672315";
    private SpeechSynthesizer mTts;
    public static String voicerLocal="xiaoyan";
    private String speed = "50"; //合成语音音速
    private String volume = "100"; //合成语音音量,0-100

    public SpeechSyn(Context context){
        this.context = context;
        setAppId();
        init();
        setParams();
    }

    public void setAppId(){
        StringBuffer param = new StringBuffer();
        param.append(SpeechConstant.APPID + app_id);
        param.append(",");
        param.append(SpeechConstant.ENGINE_MODE+"="+SpeechConstant.MODE_MSC);
        SpeechUtility.createUtility(context, param.toString());
    }

    private void init() {
        mTts = SpeechSynthesizer.createSynthesizer(context, mInitListener);
    }

    //初始化监听
    private InitListener mInitListener = new InitListener() {
        @Override
        public void onInit(int arg0) {
            if(arg0 != ErrorCode.SUCCESS) {
                toastMessage("初始化失败，错误码：" + arg0);
            } else {

            }
        }
    };

    public void toastMessage(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    private void setParams() {
        mTts.setParameter(SpeechConstant.VOICE_NAME, voicerLocal);
        mTts.setParameter(SpeechConstant.SPEED, speed);//设置语速
        mTts.setParameter(SpeechConstant.VOLUME, volume);//设置音量，范围 0~100
        mTts.setParameter(ResourceUtil.TTS_RES_PATH, getResourcePath());//设置发音人资源路径
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL); //设置本地
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/xf_yuyin/tts.wav");
    }

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

    //获取本地assets文件夹下的发音人资源路径
    private String getResourcePath() {
        StringBuffer tempBuffer = new StringBuffer();
        //合成通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(context, ResourceUtil.RESOURCE_TYPE.assets, "tts/common.jet"));
        tempBuffer.append(";");
        //发音人资源
        tempBuffer.append(ResourceUtil.generateResourcePath(context, ResourceUtil.RESOURCE_TYPE.assets, "tts/"+ "xiaoyan" +".jet"));
        return tempBuffer.toString();
    }

    public void speaking(String str){
        mTts.startSpeaking( str,mTtsListener );
    }
}

class InfoIO{
    private File fileLocation;
    private Context context;
    private FileWriter fileWriter;
    private JSONParser parser;
    private SpeechSyn aksSpeech;


    public InfoIO(String fileName, Context context, SpeechSyn aksSpeech) throws IOException {
        this.context = context;
        this.aksSpeech = aksSpeech;
        fileLocation = new File(context.getExternalFilesDir(null), "info.json");
    }

    public void storeInfo(JSONObject JsonInfo){
        try {
            fileWriter = new FileWriter(fileLocation,false);
            JsonInfo.put("category_id",1);
            fileWriter.write(JsonInfo.toString());
            fileWriter.flush();
            aksSpeech.speaking("按钮初始化成功");
        }catch (java.io.IOException e) {
            e.printStackTrace();
        }catch(JSONException e){

        }
    }

    public JSONObject readInfo(){
        try {
            parser = new JSONParser();
            Object obj = parser.parse(new FileReader(fileLocation));
            JSONObject JsonObj = new JSONObject(obj.toString());
            return JsonObj;
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
}

class WifiConnection{
    private Context context;
    private String ssid;
    private String pwd;
    private ConnectivityManager cm;
    private NetworkInfo wifiInfo;
    private SpeechSyn aksSpeech;

    public WifiConnection(Context context, SpeechSyn aksSpeech){
        this.context = context;
        this.aksSpeech = aksSpeech;
        cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public void WIFIConnect(String ssid, String pwd){
        this.ssid = ssid;
        this.pwd = pwd;
        wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if(wifiInfo.getDetailedState()==CONNECTED){
            aksSpeech.speaking("无线已经连接");
        }
        //need to connect to wifi
        else{
            WifiConfiguration wifiConfig = new WifiConfiguration();
            Toast.makeText(context, pwd, Toast.LENGTH_LONG).show();
            Toast.makeText(context, ssid, Toast.LENGTH_LONG).show();
            wifiConfig.SSID = String.format("\"%s\"", ssid);
            wifiConfig.preSharedKey = String.format("\"%s\"", pwd);
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            int netId = wifiManager.addNetwork(wifiConfig);
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            wifiManager.reconnect();
        }
    }
}

