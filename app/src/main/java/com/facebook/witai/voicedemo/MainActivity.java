package com.facebook.witai.voicedemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!checkPermissionsFromDevice()) requestPermissions();
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.RECORD_AUDIO
        }, 1000);
    }

    private boolean checkPermissionsFromDevice() {
        int recordAudioResult = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int internetResult = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);

        return recordAudioResult == PackageManager.PERMISSION_GRANTED
                && internetResult == PackageManager.PERMISSION_GRANTED;
    }
}
