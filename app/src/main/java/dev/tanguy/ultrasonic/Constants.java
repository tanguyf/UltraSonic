package dev.tanguy.ultrasonic;


public class Constants {

    final static String PREFS_NAME = "myPref";
    final static String PREF_FREQUENCY = "frequency";
    final static int FREQUENCY_8 = 8;
    final static int FREQUENCY_12 = 12;
    final static int FREQUENCY_16 = 16;
    final static int FREQUENCY_20 = 20;
    final static int FREQUENCY_22 = 22;
    final static String INTENT_PLAYING = "SoundServiceIsPlaying";
    public final static String ACTION_PLAY = "dev.tanguy.ultrasonic.PLAY";
    public final static String ACTION_STOP = "dev.tanguy.ultrasonic.STOP";
    public final static String ACTION_INCREASE_VOLUME = "dev.tanguy.ultrasonic.INCREASE";
    public final static String ACTION_DECREASE_VOLUME = "dev.tanguy.ultrasonic.DECREASE";
    public final static String RES_PREFIX = "android.resource://dev.tanguy.ultrasonic/";
    public final static int NOTIFICATION_ID = 0;
    public final static boolean USE_WHITE_ICON =
            (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
    public final static int ICON_SMALL = USE_WHITE_ICON ? R.drawable.notif_small_icon_white
                                    : R.drawable.notif_small_icon_black;
    public final static int ICON_INC = USE_WHITE_ICON ? R.drawable.ic_volume_up_black_24dp
                                    : R.drawable.ic_volume_up_white_24dp;
    public final static int ICON_DEC = USE_WHITE_ICON ? R.drawable.ic_volume_down_black_24dp
                                    : R.drawable.ic_volume_down_white_24dp;
    public final static int ICON_PAUSE = USE_WHITE_ICON ? R.drawable.ic_pause_black_24dp
                                                                                                                                            : R.drawable.ic_pause_white_24dp;
    public final static int ICON_PLAY = USE_WHITE_ICON ? R.drawable.ic_play_arrow_black_24dp
                                                                                                                                                                                    : R.drawable.ic_play_arrow_white_24dp;
    public final static int VOLUME_MIN = 0;
    public final static int VOLUME_MID = 1;
    public final static int VOLUME_MAX = 2;
}
