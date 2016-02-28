package dev.jch.tools;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

/**
 * Additional tools for adaptive brightness
 * Created by Jonatan Hamberg on 28.2.2016.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * Start a background service which monitors the charging state
         * and adjusts display brightness accordingly.
         */
        startService(new Intent(this, ChargingBrightnessService.class));
    }
}
