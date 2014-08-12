package com.proper.binmove;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import com.proper.data.LogEntry;
import com.proper.data.UserAuthenticator;
import com.proper.data.UserLoginResponse;
import com.proper.logger.LogHelper;
import com.proper.utils.DeviceUtils;

/**
 * Created by Lebel on 09/04/2014.
 */
public class ActChooser extends Activity {
    private SharedPreferences prefs = null;
    private String deviceID = "";
    private String deviceIMEI = "";
    private static final String ApplicationID = "BinMove";
    private java.util.Date utilDate = java.util.Calendar.getInstance().getTime();
    private java.sql.Timestamp today = null;
    private Button btnSingleMove;
    private Button btnBinMove;
    private Button btnExit;
    private Button btnQueries;
    private SoundPool soundPool;
    private int errorSoundId = 0;
    private DeviceUtils device = null;
    private UserAuthenticator authenticator = null;
    private UserLoginResponse currentUser;
    private LogHelper logger = new LogHelper();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lyt_chooser);

        //Identify user, load device info
        authenticator = new UserAuthenticator(this);
        device = new DeviceUtils(this);
        currentUser = authenticator.getCurrentUser();
        deviceIMEI = device.getIMEI();
        deviceID = device.getDeviceID();

        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);

        if (deviceID.equalsIgnoreCase(getString(R.string.SmallDevice))) {
            errorSoundId = soundPool.load(getString(R.string.SOUND_ERROR), 1);
        }
        if (deviceID.equalsIgnoreCase(getString(R.string.LargeDevice))) {
            errorSoundId = soundPool.load(this, R.raw.serror, 1);
        }

        this.setTitle("Welcome " + currentUser.getUserFirstName());

        //setupControls();

        btnSingleMove = (Button) this.findViewById(R.id.bnSingleMove);
        btnBinMove = (Button) this.findViewById(R.id.bnBinMove);
        btnQueries = (Button) this.findViewById(R.id.bnQueries);
        btnExit = (Button) this.findViewById(R.id.bnExitActChooser);
        btnSingleMove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnButtonClicked(view);
            }
        });
        btnBinMove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnButtonClicked(view);
            }
        });
        btnQueries.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnButtonClicked(view);
            }
        });
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnButtonClicked(view);
            }
        });

        Animation animFadeIn = AnimationUtils.loadAnimation(this, R.anim.abc_fade_in);

        btnSingleMove.startAnimation(animFadeIn);
        btnBinMove.startAnimation(animFadeIn);
        btnQueries.startAnimation(animFadeIn);
        btnExit.startAnimation(animFadeIn);
    }

    private void OnButtonClicked(View v) {
        //do
        switch(v.getId()) {
            case R.id.bnBinMove:
                if (currentUser != null) {
                    Intent frmMoveChooser = new Intent(com.proper.binmove.ActChooser.this, com.proper.binmove.ActMoveChooser.class);
                    frmMoveChooser.putExtra("INSTRUCTION", 1);
                    startActivityForResult(frmMoveChooser, 1);
                } else {
                    new SoundPool(1, AudioManager.STREAM_MUSIC, 0).play(errorSoundId, 1, 1, 0, 0, 1);
                    Vibrator vib = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
                    vib.vibrate(2000);  // Vibrate for 500 milliseconds
                    String mMsg = "User not Authenticated \nPlease login";
                    AlertDialog.Builder builder = new AlertDialog.Builder(ActChooser.this);
                    builder.setMessage(mMsg)
                            .setPositiveButton(R.string.but_ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    //do nothing
                                }
                            });
                    builder.show();
                }
                break;
            case R.id.bnSingleMove:
                if (currentUser != null) {
                    if (deviceID.equalsIgnoreCase(getString(R.string.SmallDevice))) {
                        Intent frmSingle = new Intent(com.proper.binmove.ActChooser.this, com.android.barcode.ActSingleMain.class);
                        frmSingle.putExtra("INSTRUCTION", 0);
                        startActivityForResult(frmSingle, 0);
                    }
                    if (deviceID.equalsIgnoreCase(getString(R.string.LargeDevice))) {
                        //Intent frmSingle = new Intent(com.proper.binmove.ActChooser.this, com.chainway.ht.ui.ActSingleMain.class);
                        Intent frmSingle = new Intent(com.proper.binmove.ActChooser.this, com.chainway.ht.ui.ActBinProductMain.class);
                        frmSingle.putExtra("INSTRUCTION", 0);
                        startActivityForResult(frmSingle, 0);
                    }

                } else {
                    soundPool.play(errorSoundId, 1, 1, 0, 0, 1);
                    Vibrator vib = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
                    vib.vibrate(2000);  // Vibrate for 500 milliseconds
                    String mMsg = "User not Authenticated \nPlease login";
                    AlertDialog.Builder builder = new AlertDialog.Builder(ActChooser.this);
                    builder.setMessage(mMsg)
                            .setPositiveButton(R.string.but_ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    //do nothing
                                }
                            });
                    builder.show();
                }
                break;
            case R.id.bnExitActChooser:
                finish();
                overridePendingTransition(R.anim.bottom_in, R.anim.top_out);
                break;
            case R.id.bnQueries:
                if (currentUser != null) {
                    Intent frmQueryChooser = new Intent(com.proper.binmove.ActChooser.this, com.proper.binmove.ActQueryChooser.class);
                    frmQueryChooser.putExtra("INSTRUCTION", 0);
                    startActivityForResult(frmQueryChooser, 0);
                } else {
                    soundPool.play(errorSoundId, 1, 1, 0, 0, 1);
                    Vibrator vib = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
                    // Vibrate for 500 milliseconds
                    vib.vibrate(2000);
                    String mMsg = "User not Authenticated \nPlease login";
                    AlertDialog.Builder builder = new AlertDialog.Builder(ActChooser.this);
                    builder.setMessage(mMsg)
                            .setPositiveButton(R.string.but_ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    //do nothing
                                }
                            });
                    builder.show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (currentUser == null) {
            currentUser = authenticator.getCurrentUser();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Animation animFadeIn = AnimationUtils.loadAnimation(this, R.anim.abc_fade_in);
        btnSingleMove.startAnimation(animFadeIn);
        btnBinMove.startAnimation(animFadeIn);
        btnQueries.startAnimation(animFadeIn);
        btnExit.startAnimation(animFadeIn);
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_open_scale,R.anim.activity_close_translate);
    }

    @Override
    protected void onDestroy() {
        try {
            authenticator.logOffUser();
            soundPool.release();
        } catch (Exception e) {
            e.printStackTrace();
            today = new java.sql.Timestamp(utilDate.getTime());
            LogEntry log = new LogEntry(1L, ApplicationID, "ActChooser - Attempting Logout - onDestroy", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
            logger.Log(log);
        }
        super.onDestroy();
        overridePendingTransition(R.anim.bottom_in, R.anim.top_out);
        android.os.Process.killProcess(android.os.Process.myPid()); //kill it! never returns to login screen
    }
}