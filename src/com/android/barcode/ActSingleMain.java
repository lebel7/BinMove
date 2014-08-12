package com.android.barcode;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.*;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.proper.binmove.R;
import com.proper.data.BarcodeResponse;
import com.proper.data.LogEntry;
import com.proper.data.UserAuthenticator;
import com.proper.data.UserLoginResponse;
import com.proper.data.helpers.ResponseHelper;
import com.proper.logger.LogHelper;
import com.proper.messagequeue.HttpMessageResolver;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Lebel on 10/04/2014.
 * This is used for single scan query { either scan a single product or scan a single bin code  }
 * Related layout view = lyt_singlemain.xml
 */
public class ActSingleMain extends Activity {
    private UserLoginResponse currentUser = null;   //Authentication
    public static final int KEY_SCAN = 111;
    public static final int KEY_F1 = 112;
    public static final int KEY_F2 = 113;
    public static final int KEY_F3 = 114;
    public static final int KEY_YELLOW = 115;
    private int KEY_POSITION = 0;
    private int inputByHand = 0;

    private static final int MSG_QUERY_DONE = 1;
    private static final int  MSG_QUERY_STARTING = 2;
    private String deviceIMEI = "";
    private static final String myMessageType = "BarcodeQuery";
    private static final String ApplicationID = "BinMove";
    private java.util.Date utilDate = java.util.Calendar.getInstance().getTime();
    private java.sql.Timestamp today = null;

    private DeviceControl DevCtrl;
    private SerialPort mSerialPort;
    //private String buff = new String();
    public int fd;
    private WebServiceTask wsTask;
    private ReadThread mReadThread;
    private Button close;
    private Button scan;
    private Button btnEnterBarcode;
    private EditText mReception;
    private Handler handler = null;
    private String scanInput;
    private static final String TAG = "SerialPort";
    private boolean key_start = true;
    private boolean Powered = false;
    private boolean Opened = false;
    private Timer timer = new Timer();
    private Timer retrig_timer = new Timer();
    private SoundPool soundPool;
    private	int soundId;
    private int errorSoundId;
    private Handler t_handler = null;
    private Handler n_handler = null;
    private Handler wsHandler = null;
    private boolean ops = false;
    private int NAV_INSTRUCTION = 0;
    private BarcodeResponse msgResponse = new BarcodeResponse();
    private com.proper.messagequeue.Message thisMessage = null;
    private int wsLineNumber = 0;
    private String originalEAN = "";
    private long startTime;
    private long elapseTime;
    private String backPressedParameter = "";
    private LogHelper logger = new LogHelper();
    private ResponseHelper responseHelper = new ResponseHelper();

    public BarcodeResponse getMsgResponse() {
        return msgResponse;
    }

    public void setMsgResponse(BarcodeResponse msgResponse) {
        this.msgResponse = msgResponse;
    }

    public String getScanInput() {
        return scanInput;
    }

    public void setScanInput(String scanInput) {
        this.scanInput = scanInput;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lyt_singlemain);
        thisMessage = new com.proper.messagequeue.Message();
        TelephonyManager mngr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        deviceIMEI = mngr.getDeviceId();

        mReception = (EditText) findViewById(R.id.etxtScanInput);
        //mReception.setText(""); //Clear the control before we add a listener
        mReception.addTextChangedListener(new TextChanged());
        close = (Button) this.findViewById(R.id.bnExitActSingleMain);
        close.setOnClickListener(new ClickEvent());
        scan = (Button) this.findViewById(R.id.bnScanProduct);
        scan.setOnClickListener(new ClickEvent());
        btnEnterBarcode = (Button) this.findViewById(R.id.bnEnterBarcodeSingleMain);
        btnEnterBarcode.setOnClickListener(new ClickEvent());

        wsLineNumber = 424; // manually set
//        try {
//            ClassPool pool = ClassPool.getDefault();
//            CtClass cc = pool.get("com.android.barcode.ActSingleMain");
//            javassist.CtMethod wsMethod = cc.getDeclaredMethod("afterTextChanged");
//            wsLineNumber = wsMethod.getMethodInfo().getLineNumber(0);
//        } catch (NotFoundException e1) {
//            e1.printStackTrace();
//            Log.d("onCreate", "NotFoundException - Could not find class specifies: com.android.barcode.ActSingleMain");
//            today = new java.sql.Timestamp(utilDate.getTime());
//            LogEntry log = new LogEntry(1L, ApplicationID, "ActSingleMain - onCreate", deviceIMEI, e1.getClass().getSimpleName(), e1.getMessage(), today);
//            logger.Log(log);
//        }

        try {
            DevCtrl = new DeviceControl("/proc/driver/scan");

        } catch (SecurityException e) {
            e.printStackTrace();
            today = new java.sql.Timestamp(utilDate.getTime());
            LogEntry log = new LogEntry(1L, ApplicationID, "ActSingleMain - onCreate", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
            logger.Log(log);
        } catch (IOException e) {
            Log.d(TAG, "AAA");
            today = new java.sql.Timestamp(utilDate.getTime());
            LogEntry log = new LogEntry(1L, ApplicationID, "ActSingleMain - onCreate", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
            logger.Log(log);
            new AlertDialog.Builder(this).setTitle(R.string.DIA_ALERT).setMessage(R.string.DEV_OPEN_ERR).setPositiveButton(R.string.DIA_CHECK, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    finish();
                }
            }).show();
            return;
        }
        ops = true;

        KEY_POSITION = 0; //Set for Yellow button to scan

        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        soundId = soundPool.load(getString(R.string.SOUND_SCAN), 0);
        errorSoundId = soundPool.load(getString(R.string.SOUND_ERROR), 0);

        t_handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.what == 1) {
                    try {
                        DevCtrl.PowerOffDevice();
                    } catch (IOException e) {
                        Log.d(TAG, "BBB");
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        today = new java.sql.Timestamp(utilDate.getTime());
                        LogEntry log = new LogEntry(1L, ApplicationID, "ActSingleMain - onCreate", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
                        logger.Log(log);
                    }//powerSave
                    Powered = false;
                }
            }
        };

        n_handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.what == 1) {
                    try {
                        if(key_start == false)
                        {
                            DevCtrl.TriggerOffDevice();
                            timer = new Timer();				//start a timer, when machine is idle for some time, cut off power to save energy.
                            timer.schedule(new MyTask(), 60000);
                            scan.setEnabled(true);
                            key_start = true;
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        today = new java.sql.Timestamp(utilDate.getTime());
                        LogEntry log = new LogEntry(1L, ApplicationID, "ActSingleMain - onCreate", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
                        logger.Log(log);
                    }
                }
            }
        };

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.what == 1 && key_start == false){
                    //mReception.append(buff);
                    setScanInput(msg.obj.toString());   //*************** set input from scan ***************
                    if (!mReception.getText().toString().equalsIgnoreCase("")) {
                        mReception.setText("");
                        mReception.setText(getScanInput());
                    }
                    else{
                        mReception.setText(getScanInput());
                    }
                    AudioManager audioMgr = (AudioManager) getSystemService(AUDIO_SERVICE);
                    float volumeLow = audioMgr.getStreamVolume(AudioManager.STREAM_SYSTEM);
                    float volumeLevel = volumeLow / audioMgr.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
                    //soundPool.play(soundId, 1, 1, 0, 0, 1);
                    //float volumeLevel = Float.parseFloat("0.3");
                    soundPool.play(soundId, volumeLevel, volumeLevel, 0, 0, 1);
                    key_start = true;
                    scan.setEnabled(true);
                    retrig_timer.cancel();
                }
            }
        };

        //Handle Wifi Connectivity
        ConnectivityManager connManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (!networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
            // setup a wifi configuration to our chosen network
            WifiConfiguration wc = new WifiConfiguration();
            wc.SSID = getResources().getString(R.string.ssid);
            wc.preSharedKey = getResources().getString(R.string.password);
            wc.status = WifiConfiguration.Status.ENABLED;
            wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            // connect to and enable the connection
            int netId = wifiManager.addNetwork(wc);
            wifiManager.enableNetwork(netId, true);
            wifiManager.setWifiEnabled(true);
        }
    }

    class MyTask extends TimerTask
    {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            Message message = new Message();
            message.what = 1;
            t_handler.sendMessage(message);
        }
    }

    class RetrigTask extends TimerTask
    {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            //startTime = System.currentTimeMillis(); // begin long process time elapse count
            Message message = new Message();
            message.what = 1;
            n_handler.sendMessage(message);
        }
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        if(ops == true)
        {
            mReadThread.interrupt();
            timer.cancel();
            retrig_timer.cancel();
            try {
                DevCtrl.PowerOffDevice();
                Thread.sleep(1000);
            } catch (IOException e) {
                Log.d(TAG, "CCC");
                // TODO Auto-generated catch block
                e.printStackTrace();
                today = new java.sql.Timestamp(utilDate.getTime());
                LogEntry log = new LogEntry(1L, ApplicationID, "ActSingleMain - onPause", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
                logger.Log(log);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                today = new java.sql.Timestamp(utilDate.getTime());
                LogEntry log = new LogEntry(1L, ApplicationID, "ActSingleMain - onPause", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
                logger.Log(log);
            }
            Powered = false;
            if(Opened == true)
            {
                mSerialPort.close(fd);
                Opened = false;
            }
        }
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        if(ops == true)
        {
            if(Opened == false) {
                try {
                    //mSerialPort = new SerialPort("/dev/eser1",9600);//3a
                    mSerialPort = new SerialPort("/dev/eser0",9600);//35
                } catch (SecurityException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    today = new java.sql.Timestamp(utilDate.getTime());
                    LogEntry log = new LogEntry(1L, ApplicationID, "ActSingleMain - onResume", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
                    logger.Log(log);
                } catch (IOException e) {
                    Log.d(TAG, "DDD");
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    today = new java.sql.Timestamp(utilDate.getTime());
                    LogEntry log = new LogEntry(1L, ApplicationID, "ActSingleMain - onResume", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
                    logger.Log(log);
                    new AlertDialog.Builder(this).setTitle(R.string.DIA_ALERT).setMessage(R.string.DEV_OPEN_ERR).setPositiveButton(R.string.DIA_CHECK, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub
                            finish();
                        }
                    }).show();
                    ops = false;
                    soundPool.release();
                    try {
                        DevCtrl.DeviceClose();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                        today = new java.sql.Timestamp(utilDate.getTime());
                        LogEntry log1 = new LogEntry(1L, ApplicationID, "ActSingleMain - onResume", deviceIMEI, e1.getClass().getSimpleName(), e1.getMessage(), today);
                        logger.Log(log1);
                    }
                    super.onResume();
                    return;
                }
                fd = mSerialPort.getFd();
                if(fd > 0){
                    Log.d(TAG,"opened");
                    Opened = true;
                }
            }
            mReadThread = new ReadThread();
            mReadThread.setName("MyReadThread_ActSingleMain");
            mReadThread.start();
        }
        Log.d(TAG, "onResume");
        super.onResume();
    }

    public void onDestroy() {
        // TODO Auto-generated method stub
        if(ops == true)
        {
            try {
                soundPool.release();
                DevCtrl.DeviceClose();
            } catch (IOException e) {
                Log.d(TAG, "EEE");
                // TODO Auto-generated catch block
                e.printStackTrace();
                today = new java.sql.Timestamp(utilDate.getTime());
                LogEntry log = new LogEntry(1L, ApplicationID, "ActSingleMain - onDestroy", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
                logger.Log(log);
            }
        }
        super.onDestroy();
        //android.os.Process.killProcess(android.os.Process.myPid()); Since it's not longer main entry then we're not killing app *LEBEL*
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 0) {
            if (wsTask != null && wsTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
                wsTask.cancel(true);
            }
            refreshActivity();
        }
        if (resultCode == RESULT_OK) {
            if (wsTask != null && wsTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
                wsTask.cancel(true);
            }
            refreshActivity();
        }
    }

    class TextChanged implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            // TODO Auto-generated method stub

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s != null && !s.toString().equalsIgnoreCase("")) {
                if (inputByHand == 0) {
                    int acceptable[] = {12,13,14};
                    String eanCode = s.toString().trim();
                    if (eanCode.length() > 0 && !(Arrays.binarySearch(acceptable, eanCode.length()) == -1)) {
                        //Authenticate current user, Build Message only when all these conditions are right then proceed with asyncTask
                        UserAuthenticator auth = new UserAuthenticator(ActSingleMain.this);
                        currentUser = auth.getCurrentUser();   //Gets currently authenticated user
                        if (currentUser != null) {
                            String msg = String.format("{\"UserId\":\"%s\", \"UserCode\":\"%s\",\"Barcode\":\"%s\"}",
                                    currentUser.getUserId(), currentUser.getUserCode(),eanCode);
                            originalEAN = eanCode;
                            today = new java.sql.Timestamp(utilDate.getTime());
                            thisMessage.setSource(deviceIMEI);
                            thisMessage.setMessageType(myMessageType);
                            thisMessage.setIncomingStatus(1); //default value
                            thisMessage.setIncomingMessage(msg);
                            thisMessage.setOutgoingStatus(0);   //default value
                            thisMessage.setOutgoingMessage("");
                            thisMessage.setInsertedTimeStamp(today);
                            thisMessage.setTTL(100);    //default value
                            //ArrayList<String> inputList = new ArrayList<String>();
                            //inputList.add(String.format("%s", R.integer.ACTION_SINGLEMOVE));
                            //inputList.add(eanCode);
                            //wsTask.execute(eanCode);

                            wsTask = new WebServiceTask();
                            wsTask.execute(thisMessage);
                        } else {
                            soundPool.play(errorSoundId, 1, 1, 0, 0, 1);
                            Vibrator vib = (Vibrator) ActSingleMain.this.getSystemService(Context.VIBRATOR_SERVICE);
                            // Vibrate for 500 milliseconds
                            vib.vibrate(2000);
                            String mMsg = "User not Authenticated \nPlease login";
                            AlertDialog.Builder builder = new AlertDialog.Builder(ActSingleMain.this);
                            builder.setMessage(mMsg)
                                    .setPositiveButton(R.string.but_ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            //do nothing
                                        }
                                    });
                            builder.show();
                        }
                    } else {
                        new AlertDialog.Builder(ActSingleMain.this).setTitle(R.string.DIA_ALERT).setMessage(R.string.DEV_EAN_ERR).setPositiveButton(R.string.DIA_CHECK, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Auto-generated method stub
                                refreshActivity();
                            }
                        }).show();
                    }
                }
            }
        }
    }

    class ClickEvent implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (v == close) {
                if (mReadThread != null && mReadThread.isInterrupted() == false) {
                    mReadThread.interrupt();
                }
                Intent resultIntent = new Intent();
                if (backPressedParameter != null && !backPressedParameter.equalsIgnoreCase("")) {
                    setResult(1, resultIntent);
                } else {
                    setResult(RESULT_OK, resultIntent);
                }
                com.android.barcode.ActSingleMain.this.finish();
            }
            else if(v == scan)
            {
                try {
                    if(key_start == true)
                    {
                        mReception.requestFocus();
                        if(Powered == false)
                        {
                            Powered = true;
                            DevCtrl.PowerOnDevice();
                        }
                        timer.cancel();
                        DevCtrl.TriggerOnDevice();
                        scan.setEnabled(false);
                        key_start = false;
                        retrig_timer = new Timer();
                        retrig_timer.schedule(new RetrigTask(), 3500);	//start a timer, if the data is not received within a period of time, stop the scan.
                    }
                } catch (IOException e) {
                    Log.d(TAG, "FFF");
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    today = new java.sql.Timestamp(utilDate.getTime());
                    LogEntry log = new LogEntry(1L, ApplicationID, "ActSingleMain - onClick", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
                    logger.Log(log);
                }
            } else if(v == btnEnterBarcode) {
                if (inputByHand == 0) {
                    turnOnInputByHand();
                    if (!mReception.isEnabled()) mReception.setEnabled(true);
                    mReception.setText("");
                    showSoftKeyboard();
                } else {
                    turnOffInputByHand();
                    //mReception.removeTextChangedListener();
                }
            }
        }
    }

    private void showSoftKeyboard() {
        InputMethodManager imm =(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        //inputMethodManager.toggleSoftInputFromWindow(linearLayout.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
        if (imm != null){
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
        }
    }

    private void turnOnInputByHand(){
        this.inputByHand = 1;    //Turn On Input by Hand
        this.scan.setEnabled(false);
        paintByHandButtons();
    }

    private void turnOffInputByHand(){
        this.inputByHand = 0;    //Turn On Input by Hand
        this.scan.setEnabled(true);  //
        setScanInput(mReception.getText().toString());
        if (!getScanInput().isEmpty()) {
            mReception.setText(getScanInput()); // just to trigger text changed
        }
        paintByHandButtons();
    }

    private void paintByHandButtons() {
        final String byHand = "ByHand";
        final String finish = "Finish";
        if (inputByHand == 0) {
            btnEnterBarcode.setText(byHand);
        } else {
            btnEnterBarcode.setText(finish);
        }
    }

    public static void killApp(boolean killSafely) {
        if (killSafely) {
            /*
             * Alternatively the process that runs the virtual machine could be
             * abruptly killed. This is the quickest way to remove the app from
             * the device but it could cause problems since resources will not
             * be finalized first. For example, all threads running under the
             * process will be abruptly killed when the process is abruptly
             * killed. If one of those threads was making multiple related
             * changes to the database, then it may have committed some of those
             * changes but not all of those changes when it was abruptly killed.
             */
            android.os.Process.killProcess(android.os.Process.myPid());
        }

    }

    public void refreshActivity() {
        if (!mReception.getText().toString().equalsIgnoreCase("")) mReception.setText("");
        if (!scan.isEnabled()) scan.setEnabled(true);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KEY_SCAN:
                if(KEY_POSITION == 0) {
                    try {
                        if(key_start == true)
                        {
                            mReception.requestFocus();
                            if(Powered == false)
                            {
                                Powered = true;
                                DevCtrl.PowerOnDevice();
                            }
                            timer.cancel();
                            DevCtrl.TriggerOnDevice();
                            scan.setEnabled(false);
                            key_start = false;
                            retrig_timer = new Timer();
                            retrig_timer.schedule(new RetrigTask(), 3500);	//start a timer, if the data is not received within a period of time, stop the scan.

                        }
                    } catch (IOException e) {
                        Log.d(TAG, "FFF");
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        today = new java.sql.Timestamp(utilDate.getTime());
                        LogEntry log = new LogEntry(1L, ApplicationID, "ActSingleMain - onKeyLongPress", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
                        logger.Log(log);
                    }
                }
                break;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KEY_SCAN:
                if(KEY_POSITION == 0){
                    try {
                        if(key_start == true)
                        {
                            mReception.requestFocus();
                            if(Powered == false)
                            {
                                Powered = true;
                                DevCtrl.PowerOnDevice();
                            }
                            timer.cancel();
                            DevCtrl.TriggerOnDevice();
                            scan.setEnabled(false);
                            key_start = false;
                            retrig_timer = new Timer();
                            retrig_timer.schedule(new RetrigTask(), 3500);	//start a timer, if the data is not received within a period of time, stop the scan.

                        }
                    } catch (IOException e) {
                        Log.d(TAG, "FFF");
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        today = new java.sql.Timestamp(utilDate.getTime());
                        LogEntry log = new LogEntry(1L, ApplicationID, "ActSingleMain - onKeyDown", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
                        logger.Log(log);
                    }
                }
                break;
            case KEY_YELLOW:
                if (KEY_POSITION == 0) {
                    try {
                        if(key_start == true)
                        {
                            mReception.requestFocus();
                            if(Powered == false)
                            {
                                Powered = true;
                                DevCtrl.PowerOnDevice();
                            }
                            timer.cancel();
                            DevCtrl.TriggerOnDevice();
                            scan.setEnabled(false);
                            key_start = false;
                            retrig_timer = new Timer();
                            retrig_timer.schedule(new RetrigTask(), 3500);	//start a timer, if the data is not received within a period of time, stop the scan.
                        }
                    } catch (IOException e) {
                        Log.d(TAG, "FFF");
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        today = new java.sql.Timestamp(utilDate.getTime());
                        LogEntry log = new LogEntry(1L, ApplicationID, "ActSingleMain - onKeyDown", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
                        logger.Log(log);
                    }
                    //scan.performClick();
                }
                break;
            case KEY_F1:
                if(KEY_POSITION == 1){
                    try {
                        if(key_start == true)
                        {
                            mReception.requestFocus();
                            if(Powered == false)
                            {
                                Powered = true;
                                DevCtrl.PowerOnDevice();
                            }
                            timer.cancel();
                            DevCtrl.TriggerOnDevice();
                            scan.setEnabled(false);
                            key_start = false;
                            retrig_timer = new Timer();
                            retrig_timer.schedule(new RetrigTask(), 3500);	//start a timer, if the data is not received within a period of time, stop the scan.
                        }
                    } catch (IOException e) {
                        Log.d(TAG, "FFF");
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        today = new java.sql.Timestamp(utilDate.getTime());
                        LogEntry log = new LogEntry(1L, ApplicationID, "ActSingleMain - onKeyDown", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
                        logger.Log(log);
                    }
                }
                break;
            case KEY_F2:
                if(KEY_POSITION == 2){
                    try {
                        if(key_start == true)
                        {
                            mReception.requestFocus();
                            if(Powered == false)
                            {
                                Powered = true;
                                DevCtrl.PowerOnDevice();
                            }
                            timer.cancel();
                            DevCtrl.TriggerOnDevice();
                            scan.setEnabled(false);
                            key_start = false;
                            retrig_timer = new Timer();
                            retrig_timer.schedule(new RetrigTask(), 3500);	//start a timer, if the data is not received within a period of time, stop the scan.
                        }
                    } catch (IOException e) {
                        Log.d(TAG, "FFF");
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        today = new java.sql.Timestamp(utilDate.getTime());
                        LogEntry log = new LogEntry(1L, ApplicationID, "ActSingleMain - onKeyDown", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
                        logger.Log(log);
                    }
                }
                break;
            case KEY_F3:
                if(KEY_POSITION == 3){
                    try {
                        if(key_start == true)
                        {
                            mReception.requestFocus();
                            if(Powered == false)
                            {
                                Powered = true;
                                DevCtrl.PowerOnDevice();
                            }
                            timer.cancel();
                            DevCtrl.TriggerOnDevice();
                            scan.setEnabled(false);
                            key_start = false;
                            retrig_timer = new Timer();
                            retrig_timer.schedule(new RetrigTask(), 3500);	//start a timer, if the data is not received within a period of time, stop the scan.
                        }
                    } catch (IOException e) {
                        Log.d(TAG, "FFF");
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        today = new java.sql.Timestamp(utilDate.getTime());
                        LogEntry log = new LogEntry(1L, ApplicationID, "ActSingleMain - onKeyDown", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
                        logger.Log(log);
                    }
                }
                break;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (mReadThread != null && mReadThread.isInterrupted() == false) {
            mReadThread.interrupt();
        }
        Intent resultIntent = new Intent();
        if (backPressedParameter != null && !backPressedParameter.equalsIgnoreCase("")) {
            setResult(1, resultIntent);
        } else {
            setResult(RESULT_OK, resultIntent);
        }
        //super.onBackPressed();
        com.android.barcode.ActSingleMain.this.finish();
    }

    private class ReadThread extends Thread {

        @Override
        public void run() {
            super.run();
            while(!isInterrupted()) {
                try {
                    Log.d(TAG,"read");
                    String buff = new String();
                    buff = mSerialPort.ReadSerial(fd, 1024);
                    Log.d(TAG,"end");
                    if(buff != null){
                        //ImHit = 0;
                        Message msg = new Message();
                        msg.what = 1;
                        msg.obj = buff; // Pass scanned object to ui wrapped in a message
                        handler.sendMessage(msg);
                        timer = new Timer();
                        timer.schedule(new MyTask(), 60000);
                    }else{
                        Message msg = new Message();
                        msg.what = 0;
                        handler.sendMessage(msg);
                    }
                } catch (SecurityException e) {
                    e.printStackTrace();
                    today = new java.sql.Timestamp(utilDate.getTime());
                    LogEntry log = new LogEntry(1L, ApplicationID, "ActSingleMain - ReadThread - Run", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
                    logger.Log(log);
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    today = new java.sql.Timestamp(utilDate.getTime());
                    LogEntry log = new LogEntry(1L, ApplicationID, "ActSingleMain - ReadThread - Run", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
                    logger.Log(log);
                }
            }
        }
    }

    private class WebServiceTask extends AsyncTask<com.proper.messagequeue.Message, Void, BarcodeResponse>{
        protected ProgressDialog xDialog;

        @Override
        protected void onPreExecute() {
            startTime = new Date().getTime(); //get start time
            xDialog = new ProgressDialog(ActSingleMain.this);
            CharSequence message = "Working hard...contacting webservice...";
            CharSequence title = "Please Wait";
            xDialog.setCancelable(true);
            xDialog.setCanceledOnTouchOutside(false);
            xDialog.setMessage(message);
            xDialog.setTitle(title);
            xDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            xDialog.show();
        }

        @Override
        protected BarcodeResponse doInBackground(com.proper.messagequeue.Message... barcode) {
            msgResponse = new BarcodeResponse();
            //String msg = String.format("{\"Barcode\":\"%s\"}", barcode);

            HttpMessageResolver resolver = new HttpMessageResolver();
            String response = resolver.resolveMessageQuery(thisMessage);

            if (response.contains("not recognised")) {
                //manually error trap this error
                String iMsg = "The Response object return null due to msg queue not recognising your improper request.";
                today = new java.sql.Timestamp(utilDate.getTime());
                LogEntry log = new LogEntry(1L, ApplicationID, "ActSingleMain - WebServiceTask - Line:826", deviceIMEI, RuntimeException.class.getSimpleName(), iMsg, today);
                logger.Log(log);
                throw new RuntimeException("The barcode you have scanned have not been recognised. Please check and scan again");
            }else {
                ObjectMapper mapper = new ObjectMapper();
                //mapper.setPropertyNamingStrategy(new MyCustomNamingStrategy());
                //response = responseHelper.refineOutgoingMessage(response);
                response = responseHelper.refineProductResponse(response);
                try {
                    msgResponse = mapper.readValue(response, BarcodeResponse.class);
                } catch (IOException e) {
                    e.printStackTrace();
                    today = new java.sql.Timestamp(utilDate.getTime());
                    LogEntry log = new LogEntry(1L, ApplicationID, "ActSingleMain - doInBackground", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
                    logger.Log(log);
                }
            }
            return msgResponse;
        }

        @Override
        protected void onPostExecute(BarcodeResponse result) {
            if(xDialog != null && xDialog.isShowing() == true){
                xDialog.dismiss();
            }
            long endTime = new Date().getTime(); //get task end time
            elapseTime = endTime - startTime;   //get the difference in milliseconds

            if (result != null && !result.getProducts().isEmpty()) {
                //if (msgResponse.get(0).getProductId() == 0) {
                if (result.getProducts().get(0).getProductId() == 0) {
                    if(xDialog != null && xDialog.isShowing()){ xDialog.dismiss(); }

                    Toast.makeText(ActSingleMain.this, "The device only scanned a partial barcode, please try again", Toast.LENGTH_LONG).show();
                }
                else {
                    if(xDialog != null && xDialog.isShowing()){ xDialog.dismiss(); }

                    Intent i = new Intent(com.android.barcode.ActSingleMain.this, com.proper.binmove.ActSingleDetails.class);
                    //i.putExtra("SCANDATA_EXTRA", productList.get(0).getBarcode());
                    i.putExtra("SCANDATA_EXTRA", originalEAN);
                    i.putExtra("TIME_EXTRA", elapseTime);
                    i.putExtra("ACTION_EXTRA", R.integer.ACTION_SINGLEMOVE);
                    i.putExtra("MESSAGE_RESPONSE_EXTRA", result);
                    startActivityForResult(i, 1);
                }
            }
            else {
                if (wsTask != null && wsTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
                    wsTask.cancel(true);
                }
                wsTask = null;
                if(xDialog != null && xDialog.isShowing() == true){ xDialog.dismiss(); }
                mReception.setText("");
                Toast.makeText(ActSingleMain.this, "The product scanned does not exist in our database", Toast.LENGTH_LONG).show();
            }
            if (mReception.isEnabled()) mReception.setEnabled(false);
        }

        @Override
        protected void onCancelled() {
            mReception.setText("");
        }
    }

}