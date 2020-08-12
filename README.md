
# Build an Interactive Voice-Enabled Android App with Wit.ai

## Overview

In this tutorial, we will be creating a voice-enabled Android app that greets the user. The app will be able to process the user's voice response and respond to the user appropriately. The key things we will explore is how to:

*   Design the user interaction
*   Create a Wit app and train its NLP model
*   Integrate Wit with your Android app

## Pre-requisites

*   Create a [Wit.ai](https://wit.ai/) account
*   Download the [Android Wit.ai Voice voice demo](https://github.com/wit-ai/android-voice-demo/tree/base-setup) from GitHub
*   Download and install [Android Studio](https://developer.android.com/studio)
*   Have an Android device/emulator with
    *   Internet access
    *   Microphone access
    *   API Level 26 or greater
    > If you are using a device [enable USB debugging](https://developer.android.com/studio/debug/dev-options)

## Design the User Interaction

When designing applications with voice interactions, it's important to understand the various ways that a user may interact with your app. Some techniques that can help with modeling the conversation is writing a script or creating a flow diagram. For our greeting app, let's write a script to outline it.

Let's consider the following conversation as the happy path:
```
Wit:  "Hi, welcome to the Wit.ai voice demo. I'm Wit. What is your name?"

User: "My name is Pan"

Wit:  "Nice to meet you Pan!"
```

Now let's think about scenarios were the user can deviate:
```
Wit:  "Hi, welcome to the Wit.ai speech demo. I'm Wit. What is your name?"

User: "I want pizza"

Wit:  "Sorry, I didn't get that. What is your name?"

User: "My name is Pan"

Wit:  "Nice to meet you Pan!"
```

There are many other scenarios to consider as well, but for the tutorial let's just focus on these.

## Add an introduction to your Android app

Import the Wit.ai Voice demo into Android Studio and open **app** > **src** > **main** > **java** > **com.facebook.witai.voicedemo** > **MainActivity.java**.

Next update `initializeTextToSpeech` function to include the introduction announcement as follows:

```java
public class MainActivity extends AppCompatActivity {
  /* Truncated variable declarations */

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    /* ... Truncated code */
  }

  private void initializeTextToSpeech(Context applicationContext) {
    textToSpeech = new TextToSpeech(applicationContext, new TextToSpeech.OnInitListener() {
      @Override
      public void onInit(int ttsStatus) {
        speechTranscription.setHint("Loading app ...");
        speakButton.setEnabled(false);

        if (ttsStatus == TextToSpeech.SUCCESS) {
          /*
            * ADD the following to have the app speak the welcome message
          */
          textToSpeech.speak("Hi! Welcome to the Wit a.i. voice demo. My name is Wit. What is your name?", TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
          speechTranscription.setHint("Press Speak and say something!");
          speakButton.setEnabled(true);
        } else {
          displayErrorMessage("TextToSpeech", "TextToSpeech initialization failed");
        }
      }
    });
  }

  /* Truncated code */

}
```

## Training your Wit app to do natural language processing (NLP)

Now that the Android app can speak the introduction, let’s train our Wit app to process the user’s response to the app.

1. Go to [Wit.ai](https://wit.ai/).
2. Create a new Wit.ai app:
    1. Enter a name e.g. _VoiceDemo_
    2. Select **English**
    3. Select **Open** or **Private** for visibility
    4. Click **Create**.
3. Add an utterance:
    1. Make sure you are on the **Train Your App** page by selecting **Understanding** from the left-hand menu.
    1. Type _My name is Pan_ into the **Utterance** text box.
    2. Label an entity in the utterance by highlighting _Pan_ with your mouse and select `wit/contact` as the Entity type.
4. Add an intent
    1. Click on the **Intent** dropdown.
    2. Enter _Greeting_Intent_ as the name of your new Intent.
    3. Click **Create Intent**.
5. Submit your first utterance by clicking **Train and Validate**. The training should start within a few seconds - you can see the training status in the top right.

![Gif to demo steps to train your Wit app for the Greeting Intent](https://github.com/wit-ai/android-voice-demo/blob/master/wit-training-greeting-intent.gif)

You might have heard that the most important part of machine learning is the training data. At this point, we’ve only provided our Wit app with one data point, so let's think of natural variations that a user might respond with and repeat steps #2 through #4.

Here are some variations that can be added as training utterances:

*   I'm **Scott**
*   I am **Nancy**
*   Call me **Julien**
*   My name is **David**
*   **Alice**, nice to meet you

For more information on this, see the [Quick Start](https://wit.ai/docs/quickstart) guide.


### Extend entities with inclusive and diverse data
---

A machine learning model is the product of the data it trains on, so when providing sample utterances make sure to provide a diverse array that is inclusive.

So far, besides my name — _Pan_ — we've only provided euro-centric names for training the model. If there isn't diversity in the names, my name might not be recognized as a name and might be interpreted as a pan that you fry things on.

For example, here are some utterances with more diverse names that we can add:

*   My name is **Lee Jun-fan**
*   I'm **Muhammad Ali**
*   **Ming-Na Wen**
*   **Mahershala** is the name

## Integrate Wit with your Android app

When you download the Android Wit.ai Voice Demo from the [base setup branch](https://github.com/wit-ai/android-voice-demo/tree/base-setup), the app will be capable of doing text to speech. In this part, we will enable voice processing capabilities by streaming the user’s voice audio (or utterance) to the [Wit Speech API](https://wit.ai/docs/http/20200513#post__speech_link) via HTTP. For this tutorial, we will use [OkHttp](https://square.github.io/okhttp/) as the HTTP client for making requests.


### Initialize the HTTP client to communicate with Wit Speech API endpoint
---

Open **app** > **src** > **main** > **java** > **com.facebook.witai.voicedemo** > **MainActivity.java** and make the following updates:


```java
public class MainActivity extends AppCompatActivity {
  /* ... Truncated variable declarations */

  /*
    * ADD the following variable declarations
  */
  private OkHttpClient httpClient;
  private HttpUrl.Builder httpBuilder;
  private Request.Builder httpRequestBuilder;

  /* Go to your Wit.ai app Management > Settings and obtain the Client Access Token */
  private final String CLIENT_ACCESS_TOKEN = "<YOUR CLIENT ACCESS TOKEN>";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    /* ... Truncated code */
  }

  /*
    * ADD the following function to initialize OkHttp for streaming to the Speech API
  */
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

  /* ... Truncated code */
}
```

### Capture and stream the user's voice response to Wit for processing
---

With a configured HTTP client, let's add the Android `AudioRecord` to record the user's voice response and have it streamed to the Wit Speech API for processing.


```java
public class MainActivity extends AppCompatActivity {
  /* ... Truncated variable declarations */

  /*
    * ADD the following variable declartation
  */
  private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, AUDIO_FORMAT) * 10;
  private static final AtomicBoolean recordingInProgress = new AtomicBoolean(false);

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    /* ... Truncated code */
  }

  /*
    * ADD a Runnable to record and stream the voice data to Wit
  */
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

      Request request = httpRequestBuilder.post(requestBody).build();
      try (Response response = httpClient.newCall(request).execute()) {
          if (response.isSuccessful()) {
              String responseData = response.body().string();
              respondToUser(responseData);
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

  /* ... Truncated code */

}
```

### Wireup `speakButton` to start recording and streaming the voice data
---

With a configured HTTP client, let's add the Android `AudioRecord` to capture the voice response and have it streamed to the Wit Speech API for processing.


```java
public class MainActivity extends AppCompatActivity {
  /* ... Truncated variable declarations */

  /*
    * ADD the following variable declarations
  */
  private AudioRecord recorder;
  private static final int SAMPLE_RATE = 8000;
  private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
  private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
  private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, AUDIO_FORMAT) * 10;
  private Thread recordingThread;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    /* ... Truncated code */

    /*
    * UPDATE speakButton OnClickListener to invoke startRecording and stopRecording
    */
    speakButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Log.d("speakButton", "clicked");
        if (!recordingInProgress.get()) {
          startRecording();
          speakButton.setText("Listening ...");
        } else {
          stopRecording();
          speakButton.setText("Speak");
        }
      }
    });
  }

  /*
    * ADD function to instantiate a new instance of AudioRecord and
    * start the Runnable to record and stream to the Wit Speech API
  */
  private void startRecording() {
    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL, AUDIO_FORMAT, BUFFER_SIZE);
    recorder.startRecording();
    recordingInProgress.set(true);
    recordingThread = new Thread(new StreamRecordingRunnable(), "Stream Recording Thread");
    recordingThread.start();
  }

  /*
    * ADD function to stop the recording and release the memory for
    * AudioRecord, Runnable thread, etc
  */
  private void stopRecording() {
    if (recorder == null) return;
    recordingInProgress.set(false);
    recorder.stop();
    recorder.release();
    recorder = null;
    recordingThread = null;
  }

  /* ... Truncated code */

}
```

### Respond to the user based on the Wit results from Speech API
---

When the `StreamRecordingRunnable` is finished recording and streaming the voice data, Wit will return the resolved intents and entities in the response. We will need to extract that information from the JSON and respond to the user appropriately.


```java
public class MainActivity extends AppCompatActivity {
  /*  ... Truncated variable declarations */

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    /* ... Truncated code */
  }

  /*
    * ADD a function to respond to the user based on the response
    * returned from the Wit Speech API.
  */
  private void respondToUser(String response) {
    Log.v("respondToUser", response);
    String intentName = null;
    String speakerName = null;
    String responseText = "";

    try {
      JSONObject data = new JSONObject(response);

      // Update the TextView with the voice transcription
      // Run it on the MainActivity's UI thread since it's the owner
      final String utterance = data.getString("text");
      MainActivity.this.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          speechTranscription.setText(utterance);
        }
      });

      // Get most confident intent
      JSONObject intent = getMostConfident(data.getJSONArray("intents"));
      if (intent == null) {
        textToSpeech.speak("Sorry, I didn't get that. What is your name?", TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
        return;
      }
      intentName = intent.getString("name");
      Log.v("respondToUser", intentName);

      // Parse and get the most confident entity value for the name
      JSONObject nameEntity = getMostConfident((data.getJSONObject("entities")).getJSONArray("wit$contact:contact"));
      speakerName = (String) nameEntity.get("value");
      Log.v("respondToUser", speakerName);
    } catch (JSONException e) {
        e.printStackTrace();
    }

    // Handle intents
    if (intentName.equals("Greeting_Intent")) {
      responseText = speakerName != null ? "Nice to meet you " + speakerName : "Nice to meet you";
      textToSpeech.speak(responseText, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
    } else {
      textToSpeech.speak("What did you say is your name?", TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
    }
  }

  /*
    * ADD helper function to select the most confident intents and entities
    * from the response to be used.
  */
  private JSONObject getMostConfident(JSONArray list) {
    JSONObject confidentObject = null;
    double maxConfidence = 0.0;
    for (int i = 0; i < list.length(); i++) {
      try {
          JSONObject object = list.getJSONObject(i);
          double currConfidence = object.getDouble("confidence");
          if (currConfidence > maxConfidence) {
            maxConfidence = currConfidence;
            confidentObject = object;
          }
      } catch(JSONException e) {
        e.printStackTrace();
      }
    }
    return confidentObject;
  }

  /* ... Truncated code */

}
```

## Review and continue improving your Wit app

As you are testing the app, you might notice that certain utterances are not resolving to the proper intents. To address this, go to [Wit.ai](https://wit.ai/) and on the **Understanding** page you should see utterances that have been sent to the API endpoint. You can review each utterance by expanding one and making sure that the entity is properly identified and resolving to the correct intent. If there are utterances not relevant to your use case (invalid utterances), you can mark them as **Out of Scope**.


## Next Steps

For demonstration purposes, we’ve created a very simple greeting app, but you can create a much more engaging and interactive voice-enabled app. Try sketching out a larger conversation flow with various scenarios and see our [documentation](https://wit.ai/docs) to learn more about other Wit features e.g. other built-in entities, custom entities, and traits (I recommend the [sentiment analysis trait](https://wit.ai/docs/built-in-entities#wit_sentiment)).

We look forward to what you will develop! To stay connected, join the [Wit Hackers Facebook Group](https://www.facebook.com/groups/withackers) and follow us on [Twitter @FBPlatform](https://twitter.com/fbplatform).


## Related Content

* [Wit Speech API](https://wit.ai/docs/http#post__speech_link)
* [OkHttp](https://square.github.io/okhttp/)
* [Android AudioRecord](https://developer.android.com/reference/android/media/AudioRecord)
* [Android TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech)
* [Wit Documentation](https://wit.ai/docs)
* [Wit GitHub](https://github.com/wit-ai)
* [Wit Blog](https://wit.ai/blog)
