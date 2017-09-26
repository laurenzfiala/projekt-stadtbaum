package com.laurenzfiala.stadtbaum;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

/**
 * The intro screen showing sponsors, app name & icon.
 * Also checks the app permissions.
 */
public class IntroActivity extends AppCompatActivity {

    /**
     * Period in milliseconds to show the intro screen.
     */
    private static final int INTRO_PERIOD = 1000;

    /**
     * Request code for requesting new permissions.
     */
    public static final int        REQUEST_PERMISSIONS = 2;

    /**
     * Permissions needed to be granted for the ap to work.
     */
    public static final String[]    NEEDED_PERMISSIONS = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION};

    /**
     * Whether to wait for the permission dialog with continuing to the next activity or not.
     */
    private boolean isShowPermissionDialog = false;

    /**
     * Shows this activity for the duration given in {@link #INTRO_PERIOD}
     * containing title and sponsorships etc.
     * @param savedInstanceState (not used)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        requestPermissions();
        if(!this.isShowPermissionDialog) {
            startIntroTimer();
        }

    }

    /**
     * TODO jdoc
     */
    private void startIntroTimer() {

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {

                Intent mainActivityIntent = new Intent(IntroActivity.this, MainActivity.class);
                startActivity(mainActivityIntent);

                finish(); // prevent user form returning to title screen

            }

        }, INTRO_PERIOD);

    }

    /**
     * Runtime request for "dangerous" coarse location permission.
     * Only request if not already requested.
     */
    private void requestPermissions() {
        if(Build.VERSION.SDK_INT >= 23) {
            int granted = 0;
            for (String perm : NEEDED_PERMISSIONS) {
                if (this.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED) {
                    granted++;
                }
            }
            if (granted < NEEDED_PERMISSIONS.length) {
                this.isShowPermissionDialog = true;
                Dialogs.permissionsDialog(this);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        int granted = 0;
        if(requestCode == REQUEST_PERMISSIONS) {
            for (int i = 0; i < permissions.length; i++) {
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    granted++;
                }
            }
        }

        if(granted < permissions.length) {
            requestPermissions();
        } else {
            startIntroTimer();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
