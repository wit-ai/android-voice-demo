/* Copyright (c) Facebook, Inc. and its affiliates. */

package com.facebook.witai.voicedemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private Button speakButton;
    private TextView speechTranscription;
    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!checkPermissionsFromDevice()) requestPermissions();

        // Get a reference to the TextView and Button from the UI
        speechTranscription = findViewById(R.id.speechTranscription);
        speakButton = findViewById(R.id.speakButton);

        // Initialize TextToSpeech
        initializeTextToSpeech(this.getApplicationContext());
    }

    // Initialize the Android TextToSpeech
    // https://developer.android.com/reference/android/speech/tts/TextToSpeech
    private void initializeTextToSpeech(Context applicationContext) {
        textToSpeech = new TextToSpeech(applicationContext, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int ttsStatus) {
                // Disable the speakButton and provide the status of app while waiting for TextToSpeech to initialize
                speechTranscription.setHint("Loading app ...");
                speakButton.setEnabled(false);

                // Check the status of the initialization
                if (ttsStatus == TextToSpeech.SUCCESS) {
                    speechTranscription.setHint("Press Speak and say something!");
                    speakButton.setEnabled(true);
                } else {
                    String message = "TextToSpeech initialization failed";
                    speechTranscription.setTextColor(Color.RED);
                    speechTranscription.setText(message);
                    Log.e("TextToSpeech", message);
                }
            }
        });
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
