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
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";

    private ToggleButton togglePlay;

    private SoundService soundService;
    private Intent serviceIntent;
    private boolean serviceBound = false;
    private ServiceConnection serviceConnection;
    private LocalBroadcastManager broadcastManager;
    private BroadcastReceiver broadcastReceiver;
    private SeekBar volumeBar;
    private ImageView volumeIcon;
    private SeekBar frequencyBar;
    private TextView frequencyText;

    private void setUpBroadcastManager() {
        broadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.INTENT_PLAYING);
        intentFilter.addAction(Constants.INTENT_VOLUME_CHANGED);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "receiving intent... "+intent);
                if(Constants.INTENT_PLAYING.equals(intent.getAction())) {
                    updateToggleState();
                }
                else if(Constants.INTENT_VOLUME_CHANGED.equals(intent.getAction())){
                    updateVolumeBar();
                }
            }
        };
        broadcastManager.registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpSoundManager();
        setUpBroadcastManager();
        setUpButtons();
        restorePreferences();
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
        frequencyBar.setProgress(frequency - Constants.FREQUENCY_MIN);
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
                    //initialize values
                    updateToggleState();
                    updateFrequencyText();
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
        volumeIcon = (ImageView) findViewById(R.id.ic_volume);
        volumeBar = (SeekBar) findViewById(R.id.seekBarVolume);
        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        volumeBar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        updateVolumeBar();

        frequencyBar = (SeekBar) findViewById(R.id.seekBarFrequency);
        frequencyBar.setMax(Constants.FREQUENCY_MAX - Constants.FREQUENCY_MIN);
        frequencyBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(soundService != null)
                    soundService.updateFrequency(progress+Constants.FREQUENCY_MIN);
                updateFrequencyText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        frequencyText = (TextView) findViewById(R.id.text_frequency);
    }

    private void updateFrequencyText(){
        if(soundService != null)
            frequencyText.setText(Constants.getFrequencyAsStringResource(soundService.getFrequency()));
    }

    private void updateVolumeBar(){
        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        final int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeBar.setProgress(volume);
        int level = Constants.VOLUME_MID;
        if (volume == audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
            level = Constants.VOLUME_MAX;
        else if (volume == 0)
            level = Constants.VOLUME_MIN;
        volumeIcon.setImageLevel(level);
        Log.d(TAG, "update volume bar, level = " + volume);
    }
}
