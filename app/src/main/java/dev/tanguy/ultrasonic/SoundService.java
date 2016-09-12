package dev.tanguy.ultrasonic;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.IOException;

public class SoundService extends Service implements    AudioManager.OnAudioFocusChangeListener,
                                                        MediaPlayer.OnPreparedListener,
                                                        MediaPlayer.OnErrorListener,
                                                        MediaPlayer.OnCompletionListener{

    private final static String TAG = "SoundService";

    private Notification notification;
    private MediaPlayer player;
    private final IBinder myBinder = new MyBinder();
    private VolumeObserver volumeObserver;
    private int volumeLevel = 0;
    private int frequency = Constants.FREQUENCY_8;
    private PendingIntent playIntent;
    private PendingIntent muteIntent;
    private PendingIntent maxLoudIntent;
    private PendingIntent activityIntent;

    @Override
    public void onCreate() {
        super.onCreate();
        restorePreferences();
        setUpIntents();
        initPlayer();
        setUpVolumeObserver();
        updateVolumeLevel();
    }

    private void restorePreferences() {
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
        frequency = settings.getInt(Constants.PREF_FREQUENCY, Constants.FREQUENCY_8);
    }

    public void updateFrequency(int freq) {

        frequency = freq;

        if(isPlaying()) {
            stop();
            play();
        }

        updateNotification();

        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(Constants.PREF_FREQUENCY, freq);
        editor.apply();
    }

    @Override
    public void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        this.getContentResolver().unregisterContentObserver(volumeObserver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Log.i(TAG, "onStartCommand with Action " + intent.getAction());
            if (Constants.ACTION_PLAY_OR_PAUSE.equals(intent.getAction())) {
                if (isPlaying()) stop(); else play();
            }
            else if (Constants.ACTION_MAX_LOUD.equals(intent.getAction())) adjustVolume(true);
            else if (Constants.ACTION_MUTE.equals(intent.getAction())) adjustVolume(false);
        }
        // We want this service to continue running until it is explicitly stopped, so return sticky.
        return START_STICKY;
    }

    private void adjustVolume(boolean up) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                up ? audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) : 0, 0);
    }

    private void setUpVolumeObserver() {
        volumeObserver = new VolumeObserver(this,new Handler());
        this.getContentResolver()
                .registerContentObserver(android.provider.Settings.System.CONTENT_URI, true,
                        volumeObserver);
    }

    private void setUpIntents() {
        playIntent = PendingIntent.getService(this, 0,
                new Intent(Constants.ACTION_PLAY_OR_PAUSE, null, this, SoundService.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        muteIntent = PendingIntent.getService(this, 0,
                new Intent(Constants.ACTION_MUTE, null, this, SoundService.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        maxLoudIntent = PendingIntent.getService(this, 0,
                new Intent(Constants.ACTION_MAX_LOUD, null, this, SoundService.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        activityIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }


    private void updateNotification() {

        Log.d(TAG, "update notification, volume level = "+volumeLevel);

        notification = new NotificationCompat.Builder(this)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setContentIntent(activityIntent)
                .setSmallIcon(Constants.ICON_SMALL, volumeLevel).build();

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification);
        contentView.setTextViewText(R.id.notif_text_view, getFrequencyAsString());
        int iconId = -1;
        switch(volumeLevel){
            case Constants.VOLUME_MIN: iconId = R.drawable.ic_volume_mute_white_24dp; break;
            case Constants.VOLUME_MID: iconId = R.drawable.ic_volume_down_white_24dp; break;
            case Constants.VOLUME_MAX: iconId = R.drawable.ic_volume_up_white_24dp; break;
        }
        contentView.setImageViewResource(R.id.ic_notification, iconId);
        contentView.setImageViewResource(R.id.ic_play, isPlaying() ?
                R.drawable.ic_pause_black_24dp : R.drawable.ic_play_arrow_black_24dp);
        contentView.setOnClickPendingIntent(R.id.ic_play, playIntent);
        contentView.setOnClickPendingIntent(R.id.ic_mute, muteIntent);
        contentView.setOnClickPendingIntent(R.id.ic_max_loud, maxLoudIntent);

        notification.contentView = contentView;

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Constants.NOTIFICATION_ID, notification);
    }


    private void requestAudioFocus() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // TODO could not get audio focus.
            Log.w(TAG, "WARNING! could not get audio focus");
        }
    }

    private void initPlayer(){
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        requestAudioFocus();
    }

    public void play() {
        Log.i(TAG, "play");
        if(player == null){
            initPlayer();
        }
        if (!isPlaying()) {
            player.reset();
            try {
                player.setDataSource(this, getSoundFileUri());
                player.prepareAsync();
            } catch (IOException e) {
                Log.e(TAG, "setDataSource failed", e);
            }
        }
    }

    private String getFrequencyAsString() {
        switch(frequency){
            case Constants.FREQUENCY_8:
                return getString(R.string.FREQUENCY_8);
            case Constants.FREQUENCY_12:
                return getString(R.string.FREQUENCY_12);
            case Constants.FREQUENCY_16:
                return getString(R.string.FREQUENCY_16);
            case Constants.FREQUENCY_20:
                return getString(R.string.FREQUENCY_20);
            case Constants.FREQUENCY_22:
                return getString(R.string.FREQUENCY_22);
            default:
                return getString(R.string.FREQUENCY_8);
        }
    }

    private Uri getSoundFileUri() {
        switch(frequency){
            case Constants.FREQUENCY_8:
                return Uri.parse(Constants.RES_PREFIX + R.raw.khz8);
            case Constants.FREQUENCY_12:
                return Uri.parse(Constants.RES_PREFIX + R.raw.khz12);
            case Constants.FREQUENCY_16:
                return Uri.parse(Constants.RES_PREFIX + R.raw.khz16);
            case Constants.FREQUENCY_20:
                return Uri.parse(Constants.RES_PREFIX + R.raw.khz20);
            case Constants.FREQUENCY_22:
                return Uri.parse(Constants.RES_PREFIX + R.raw.khz22);
            default:
                return Uri.parse(Constants.RES_PREFIX + R.raw.khz8);
        }
    }

    public void stop() {
        Log.i(TAG, "stop");
        if (isPlaying()) {
            stopForeground(true);
            player.stop();
            updateNotification();
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Constants.INTENT_PLAYING));
        }
    }

    public boolean isPlaying() {
        try{
            return player.isPlaying();
        }
        catch(Exception e){
            return false;
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                Log.d(TAG, "onAudioFocusChange: AUDIOFOCUS_GAIN");
                // resume playback
                if (player == null) initPlayer();
                else if (!player.isPlaying()) player.start();
                player.setVolume(1.0f, 1.0f);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                Log.d(TAG, "onAudioFocusChange: AUDIOFOCUS_LOSS");
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (player.isPlaying()) player.stop();
                player.release();
                player = null;
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Log.d(TAG, "onAudioFocusChange: AUDIOFOCUS_LOSS_TRANSIENT");
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (player.isPlaying()) player.pause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Log.d(TAG, "onAudioFocusChange: AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (player.isPlaying()) player.setVolume(0.1f, 0.1f);
                break;
        }

    }

    public class MyBinder extends Binder {
        SoundService getService() {
            return SoundService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    @Override
    public boolean onUnbind(Intent intent){
        Log.i(TAG, "onUnbind");
        stopForeground(true);
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.i(TAG, "onCompletion");
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        Log.e(TAG, "onError: what " + what + ", extra "+ extra);
        mediaPlayer.reset();
        stopForeground(true);
        updateNotification();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.i(TAG, "onPrepared");
        startForeground(Constants.NOTIFICATION_ID, notification);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
        updateNotification();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Constants.INTENT_PLAYING));
    }

    private void updateVolumeLevel() {
        AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        int volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);

        if (volume == audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) volumeLevel = Constants.VOLUME_MAX;
        else if (volume == 0) volumeLevel = Constants.VOLUME_MIN;
        else volumeLevel = Constants.VOLUME_MID;
        Log.d(TAG, "updateVolumeLevel, volume = "+volume+", level = "+volumeLevel);
        updateNotification();
    }


    public class VolumeObserver extends ContentObserver {
        Context context;

        public VolumeObserver(Context c, Handler handler) {
            super(handler);
            context=c;
        }

        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            updateVolumeLevel();
        }
    }

}
