package dev.jch.tools;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

/**
 * Turns maximum display brightness on when charging
 * Created by Jonatan Hamberg on 28.2.2016.
 */
public class ChargingBrightnessService extends Service {

    private final int AUTOMATIC = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
    private final int MANUAL = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
    private final float POWER_SAVING_LEVEL = 0.15f;
    private final int BRIGHTNESS_GUESS = 128;
    private final int BRIGHTNESS_MAX = 255;

    private int previousBrightness;
    private int previousMode;
    private boolean plugged = false;

    //region Service implementation
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        previousMode = getBrightnessMode();
        previousBrightness = getBrightnessLevel();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(chargingReceiver, filter);
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, "Started brightness service!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(chargingReceiver);
    }
    //endregion

    private final BroadcastReceiver chargingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int batteryStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int batteryPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPercentage = level / (float) scale;

            /**
             * Turn on maximum brightness only when following conditions are met:
             * - Not already plugged
             * - Charging or battery full
             * - Plugged to AC (instead of e.g. USB)
             * - Above battery saving level
             */
            if (!plugged
                    && (batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING
                    || batteryStatus == BatteryManager.BATTERY_STATUS_FULL)
                    && batteryPlugged == BatteryManager.BATTERY_PLUGGED_AC
                    && batteryPercentage > POWER_SAVING_LEVEL) {

                Toast.makeText(context, "Plugged!", Toast.LENGTH_LONG).show();

                // Snapshot brightness settings
                previousBrightness = getBrightnessLevel();
                previousMode = getBrightnessMode();
                plugged = true;

                setBrightnessMode(MANUAL);
                setBrightnessLevel(BRIGHTNESS_MAX);

            } else if (plugged
                    && batteryStatus == BatteryManager.BATTERY_STATUS_DISCHARGING) {
                Toast.makeText(context, "Unplugged!", Toast.LENGTH_LONG).show();
                plugged = false;

                // Restore previous settings if needed
                if (getBrightnessLevel() != previousBrightness) {
                    setBrightnessLevel(previousBrightness);
                }
                if (getBrightnessMode() != previousMode) {
                    setBrightnessMode(previousMode);
                }
            }
        }
    };

    //region System settings access
    private void setBrightnessMode(int mode) {
        Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, mode);
    }

    private void setBrightnessLevel(int level) {
        Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, level);
    }

    private int getBrightnessMode() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
        } catch (Settings.SettingNotFoundException e) {
            Log.d("error", "Failed reading brightness level, defaulting to automatic");
            return AUTOMATIC;
        }
    }

    private int getBrightnessLevel() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            Log.d("error", "Failed reading brightness level, defaulting to " + BRIGHTNESS_GUESS);
            return BRIGHTNESS_GUESS;
        }
    }
    //endregion
}
