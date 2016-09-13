package dev.tanguy.ultrasonic;


public class Constants {

    final static String PREFS_NAME = "myPref";
    final static String PREF_FREQUENCY = "frequency";
    final static int FREQUENCY_8 = 8;
    final static int FREQUENCY_12 = 12;
    final static int FREQUENCY_16 = 16;
    final static int FREQUENCY_20 = 20;
    final static int FREQUENCY_22 = 22;
    final static int FREQUENCY_MIN = FREQUENCY_8;
    final static int FREQUENCY_MAX = FREQUENCY_22;
    final static int[] FREQUENCIES = new int[]{8, 12, 16, 20, 22};
    final static String INTENT_PLAYING = "SoundServiceIsPlaying";
    final static String INTENT_VOLUME_CHANGED = "VolumeChanged";
    public final static String ACTION_PLAY_OR_PAUSE = "dev.tanguy.ultrasonic.PLAY";
    public final static String ACTION_MAX_LOUD = "dev.tanguy.ultrasonic.INCREASE";
    public final static String ACTION_MUTE = "dev.tanguy.ultrasonic.DECREASE";
    public final static String RES_PREFIX = "android.resource://dev.tanguy.ultrasonic/";
    public final static int NOTIFICATION_ID = 0;
    public final static boolean USE_WHITE_ICON =
            (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
    public final static int ICON_SMALL = USE_WHITE_ICON ? R.drawable.notif_small_icon_white
                                    : R.drawable.notif_small_icon_black;
    public final static int VOLUME_MIN = 0;
    public final static int VOLUME_MID = 1;
    public final static int VOLUME_MAX = 2;

    public static int getFrequencyAsStringResource(int frequency) {
        switch(frequency){
            case FREQUENCY_8:
                return R.string.FREQUENCY_8;
            case FREQUENCY_12:
                return R.string.FREQUENCY_12;
            case FREQUENCY_16:
                return R.string.FREQUENCY_16;
            case FREQUENCY_20:
                return R.string.FREQUENCY_20;
            case FREQUENCY_22:
                return R.string.FREQUENCY_22;
            default:
                return R.string.FREQUENCY_8;
        }
    }
}
