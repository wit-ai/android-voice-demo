package com.facebook.witai.voicedemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

public class MainActivity extends AppCompatActivity {
    private Button speakButton;
    private TextView speechTranscription;
    private TextToSpeech textToSpeech;

    private OkHttpClient httpClient;
    private HttpUrl.Builder httpBuilder;
    private Request.Builder httpRequestBuilder;

    private AudioRecord recorder;
    private static final int SAMPLE_RATE = 8000;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, AUDIO_FORMAT) * 10;
    private static final AtomicBoolean recordingInProgress = new AtomicBoolean(false);
    private Thread recordingThread;

    /* Go to your Wit.ai app Management > Settings and obtain the Client Access Token */
    private static final String CLIENT_ACCESS_TOKEN = "<YOUR CLIENT ACCESS TOKEN>";

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

        // Initialize HTTP Client
        initializeHttpClient();

        // Wire up speakButton to an onClickListener
        speakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!recordingInProgress.get()) {
                    startRecording();
                    speakButton.setText("Listening ...");
                    Log.d("speakButton", "Start listening ...");
                }  else {
                    stopRecording();
                    speakButton.setText("Speak");
                    Log.d("speakButton", "Stop listening ...");
                }
            }
        });
    }

    // Instantiate a new AudioRecord and start streaming the recording to the Wit Speech API
    private void startRecording() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL, AUDIO_FORMAT, BUFFER_SIZE);
        recorder.startRecording();
        recordingInProgress.set(true);
        recordingThread = new Thread(new StreamRecordingRunnable(), "Stream Recording Thread");
        recordingThread.start();
    }

    // Release resources for the AudioRecord and Runnable when recording is stopped
    private void stopRecording() {
        if (recorder == null) return;
        recordingInProgress.set(false);
        recorder.stop();
        recorder.release();
        recorder = null;
        recordingThread = null;
    }

    // Define a Runnable to stream the recording data to the Speech API
    // https://wit.ai/docs/http/20200513#post__speech_link
    private class StreamRecordingRunnable implements Runnable {
        @Override
        public void run() {
            final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
            RequestBody requestBody = new RequestBody() {
                @Override
                public MediaType contentType() {
                    return MediaType.parse("audio/raw;encoding=signed-integer;bits=16;rate=8000;endian=little");
                }

                @Override
                public void writeTo(@NotNull BufferedSink bufferedSink) throws IOException {
                    while (recordingInProgress.get()) {
                        int result = recorder.read(buffer, BUFFER_SIZE);
                        if (result < 0) {
                            throw new RuntimeException("Reading of audio buffer failed: " +
                                    getBufferReadFailureReason(result));
                        }
                        bufferedSink.write(buffer);
                        buffer.clear();
                    }
                }
            };

            // Start streaming audio to Wit.ai Speech API
            Request request = httpRequestBuilder.post(requestBody).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    /* To DO: respond to user after receiving a response back from Wit */
                    Log.v("Streaming Response", responseData);
                }
            } catch (IOException e) {
                Log.e("Streaming Response", e.getMessage());
            }
        }

        private String getBufferReadFailureReason(int errorCode) {
            switch (errorCode) {
                case AudioRecord.ERROR_INVALID_OPERATION:
                    return "ERROR_INVALID_OPERATION";
                case AudioRecord.ERROR_BAD_VALUE:
                    return "ERROR_BAD_VALUE";
                case AudioRecord.ERROR_DEAD_OBJECT:
                    return "ERROR_DEAD_OBJECT";
                case AudioRecord.ERROR:
                    return "ERROR";
                default:
                    return "Unknown (" + errorCode + ")";
            }
        }
    }

    // Instantiate a Request.Builder that can be used for all the streaming requests
    // https://square.github.io/okhttp/recipes/#post-streaming-kt-java
    // https://wit.ai/docs/http/20200513#post__speech_link
    private void initializeHttpClient() {
        httpClient = new OkHttpClient();
        httpBuilder = HttpUrl.parse("https://api.wit.ai/speech").newBuilder();
        httpBuilder.addQueryParameter("v", "20200805");
        httpRequestBuilder = new Request.Builder()
                .url(httpBuilder.build())
                .header("Authorization", "Bearer " + CLIENT_ACCESS_TOKEN)
                .header("Content-Type", "audio/raw")
                .header("Transfer-Encoding", "chunked");
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
