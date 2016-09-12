package dev.tanguy.ultrasonic;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {

    private ToggleButton togglePlay;

    private SoundService soundService;
    private Intent serviceIntent;
    private boolean serviceBound = false;
    private ServiceConnection serviceConnection;
    private LocalBroadcastManager broadcastManager;
    private BroadcastReceiver broadcastReceiver;

    private void setUpBroadcastManager() {
        broadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.INTENT_PLAYING);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(Constants.INTENT_PLAYING)) {
                    updateToggleState();
                }
            }
        };
        broadcastManager.registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpButtons();
        restorePreferences();
        setUpSoundManager();
        setUpBroadcastManager();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound){
            unbindService(serviceConnection);
            serviceConnection = null;
            serviceIntent = null;
            serviceBound = false;
        }
        broadcastManager.unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (serviceIntent == null){
            serviceIntent = new Intent(this, SoundService.class);
            bindService(serviceIntent, getServiceConnection(), Context.BIND_AUTO_CREATE);
            startService(serviceIntent);
        }
    }

    private void restorePreferences() {
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
        int frequency = settings.getInt(Constants.PREF_FREQUENCY, Constants.FREQUENCY_8);
        switch (frequency) {
            case Constants.FREQUENCY_8:
                ((RadioButton) findViewById(R.id.freq_8)).setChecked(true);
                break;
            case Constants.FREQUENCY_12:
                ((RadioButton) findViewById(R.id.freq_12)).setChecked(true);
                break;
            case Constants.FREQUENCY_16:
                ((RadioButton) findViewById(R.id.freq_16)).setChecked(true);
                break;
            case Constants.FREQUENCY_20:
                ((RadioButton) findViewById(R.id.freq_20)).setChecked(true);
                break;
            case Constants.FREQUENCY_22:
                ((RadioButton) findViewById(R.id.freq_22)).setChecked(true);
                break;
        }
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        if (checked && soundService != null && serviceBound) {
            // Check which radio button was clicked
            switch (view.getId()) {
                case R.id.freq_8: soundService.updateFrequency(8);
                        break;
                case R.id.freq_12: soundService.updateFrequency(12);
                        break;
                case R.id.freq_16: soundService.updateFrequency(16);
                    break;
                case R.id.freq_20: soundService.updateFrequency(20);
                    break;
                case R.id.freq_22: soundService.updateFrequency(22);
                    break;
            }
        }
    }

    private boolean isPlaying() {
        boolean isPlaying = false;
        if (soundService != null && serviceBound) {
            isPlaying = soundService.isPlaying();
        }
        return isPlaying;
    }

    private ServiceConnection getServiceConnection() {
        if (serviceConnection == null) {
            serviceConnection = new ServiceConnection(){

                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    SoundService.MyBinder binder = (SoundService.MyBinder)service;
                    soundService = binder.getService();
                    //@TODO give service some data
                    serviceBound = true;
                    updateToggleState();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    serviceBound = false;
                    updateToggleState();
                }
            };
        }
        return serviceConnection;
    }

    private void setUpSoundManager() {
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    private void updateToggleState() {
        if (togglePlay != null){
            togglePlay.setChecked(isPlaying());
        }
    }

    private void play() {
        if (serviceBound && !isPlaying()) {
            soundService.play();
        }
    }

    private void stop() {
        if (isPlaying()) {
            soundService.stop();
        }
    }

    private void setUpButtons() {
        togglePlay = (ToggleButton) findViewById(R.id.bt_toggle_play);
        togglePlay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) play();
                else stop();
            }
        });
    }
}
