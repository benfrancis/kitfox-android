package org.mozilla.kitfox;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mozilla.speechlibrary.ISpeechRecognitionListener;
import com.mozilla.speechlibrary.MozillaSpeechService;
import com.mozilla.speechlibrary.STTResult;

import org.json.JSONException;
import org.json.JSONObject;

import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebRequest;
import org.mozilla.geckoview.GeckoWebExecutor;
import org.mozilla.geckoview.WebResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 *  Kitfox main activity.
 */
public class MainActivity extends Activity {
    private static final String HOME_PAGE = "http://kitfox.tola.me.uk";
    private static final String KITFOX_SERVER = "http://kitfox.tola.me.uk";

    private GeckoView geckoview;
    private GeckoSession session;
    private GeckoRuntime runtime;
    private MozillaSpeechService speechService;
    private EditText urlBar;
    private ListView chatView;
    private ChatArrayAdapter chatArrayAdapter;
    private ImageButton speakButton;
    private ProgressBar loadingSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request record audio permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    123);
        }

        // Request storage write permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    124);
        }

        geckoview = findViewById(R.id.geckoview);
        session = new GeckoSession();
        runtime = GeckoRuntime.create(this);

        session.open(runtime);
        geckoview.setSession(session);
        session.loadUri(HOME_PAGE);

        urlBar = findViewById(R.id.urlbar);
        speakButton = findViewById(R.id.speak_button);
        loadingSpinner = findViewById(R.id.loading_spinner);

        chatView = findViewById(R.id.chat_view);
        chatArrayAdapter = new ChatArrayAdapter(getApplicationContext(), R.layout.incoming_message);
        chatView.setAdapter(chatArrayAdapter);

        /**
         * Navigate to URL or send message on submit.
         */
        urlBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String url = urlBar.getText().toString();
                    if (url.length() == 0) {
                        return false;
                    }

                    // Navigate to URL or submit chat message
                    if (URLUtil.isValidUrl(url) && url.contains(".")) {
                        showWebView();
                        session.loadUri(url);
                    } else if (URLUtil.isValidUrl("http://" + url) && url.contains(".")) {
                        showWebView();
                        session.loadUri("http://" + url);
                    } else {
                        String message = urlBar.getText().toString();
                        urlBar.setText("");
                        showChatView();
                        showChatMessage(false, message);
                        sendChatMessage(message);
                    }

                    // Blur URL bar and hide keyboard
                    urlBar.clearFocus();
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        ISpeechRecognitionListener speechListener = new ISpeechRecognitionListener() {
            public void onSpeechStatusChanged(final MozillaSpeechService.SpeechState aState, final Object aPayload){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (aState) {
                            case DECODING:
                                // Handle when the speech object changes to decoding state
                                speakButton.setImageResource(R.drawable.speak_button);
                                speakButton.setVisibility(View.GONE);
                                loadingSpinner.setVisibility(View.VISIBLE);

                                System.out.println("*** Decoding...");
                                break;
                            case MIC_ACTIVITY:
                                // Captures the activity from the microphone
                                double db = (double)aPayload * -1;
                                System.out.println("*** Mic activity: " + Double.toString(db));
                                break;
                            case STT_RESULT:
                                // When the api finished processing and returned a hypothesis
                                loadingSpinner.setVisibility(View.GONE);
                                speakButton.setVisibility(View.VISIBLE);
                                String transcription = ((STTResult)aPayload).mTranscription;
                                float confidence = ((STTResult)aPayload).mConfidence;
                                System.out.println("*** Result: " + transcription);
                                showChatView();
                                showChatMessage(false, transcription);
                                sendChatMessage(transcription);
                                break;
                            case START_LISTEN:
                                // Handle when the api successfully opened the microphone and started listening
                                speakButton.setImageResource(R.drawable.speak_button_active);
                                System.out.println("*** Listening...");
                                break;
                            case NO_VOICE:
                                // Handle when the api didn't detect any voice
                                System.out.println("*** No voice detected.");
                                break;
                            case CANCELED:
                                // Handle when a cancellation was fully executed
                                speakButton.setImageResource(R.drawable.speak_button);
                                loadingSpinner.setVisibility(View.GONE);
                                speakButton.setVisibility(View.VISIBLE);
                                System.out.println("*** Cancelled.");
                                break;
                            case ERROR:
                                // Handle when any error occurred
                                speakButton.setImageResource(R.drawable.speak_button);
                                loadingSpinner.setVisibility(View.GONE);
                                speakButton.setVisibility(View.VISIBLE);
                                //string error = aPayload;
                                System.out.println("*** Error: " + aPayload);
                                break;
                            default:
                                break;
                        }
                    }
                });
            }
        };

        speechService = MozillaSpeechService.getInstance();
        speechService.setLanguage("en-GB");
        speechService.addListener(speechListener);

        speakButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                speechService.start(getApplicationContext());
                System.out.println("*** Speak button clicked");
            }
        });
    }

    /**
     * Show web view and navigate to home page.
     *
     * @param view
     */
    public void goHome(View view) {
        showWebView();
        session.loadUri(HOME_PAGE);
        urlBar.setText("");
    }

    /**
     * Show web view.
     */
    private void showWebView() {
        chatView.setVisibility(View.GONE);
        geckoview.setVisibility(View.VISIBLE);
    }

    /**
     * Show chat view.
     */
    private void showChatView() {
        geckoview.setVisibility(View.GONE);
        chatView.setVisibility(View.VISIBLE);
    }

    /**
     * Show a message in the chat UI.
     */
    private void showChatMessage(boolean direction, String message) {
        chatArrayAdapter.add(new ChatMessage(direction, message));
    }

    /**
     * Send a message to the Kitfox server.
     */
    private void sendChatMessage(String message) {
        // Build URL
        String commandsUrl = this.KITFOX_SERVER + "/commands";

        // Build body
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("text", message);
        JSONObject bodyObject = new JSONObject(properties);
        ByteBuffer body = this.jsonObjectToByteBuffer(bodyObject);

        // Build request
        WebRequest.Builder builder = new WebRequest.Builder(commandsUrl);
        builder.method("POST");
        builder.header("Content-Type", "application/json");
        builder.body(body);
        WebRequest request = builder.build();

        // Send request
        final GeckoWebExecutor executor = new GeckoWebExecutor(this.runtime);
        final GeckoResult<WebResponse> result = executor.fetch(request);

        // Handle response
        result.then(new GeckoResult.OnValueListener<WebResponse, Void>() {
            @Override
            public GeckoResult<Void> onValue(final WebResponse response) {
                handleMessageResponse(response);
                return null;
            }
        }, new GeckoResult.OnExceptionListener<Void>() {
            @Override
            public GeckoResult<Void> onException(final Throwable exception) {
                Log.e("Kitfox", "Exception with response from server");
                return null;
            }
        });
    }

    /**
     * Handle a response from the Kitfox server.
     *
     * @param response WebResponse received.
     */
    private void handleMessageResponse(WebResponse response) {
        // Detect error responses
        if (response.statusCode != 200) {
            Log.e("Kitfox", "Received bad response from server " + response.statusCode);
            return;
        }

        // Parse response
        JSONObject jsonResponse = this.inputStreamToJsonObject(response.body);

        String method = null;
        String url = null;
        String text = null;

        try {
            method = jsonResponse.getString("method");
        } catch(JSONException exception) { }

        try {
            url = jsonResponse.getString("url");
        } catch(JSONException exception) { }

        try {
            text = jsonResponse.getString("text");
        } catch(JSONException exception) { }

        // If a textual response is provided, show it in chat
        if (text != null && text.length() > 0) {
            this.showChatMessage(true, text);
        } else {
            Log.i("Kitfox","No text response provided by server.");
        }

        if (method == null || url == null) {
            Log.i("Kitfox", "No method or URL provided by server.");
            return;
        }

        // If a GET action with a URL is provided, navigate to it
        if (method.toUpperCase().equals("GET") && url.length() > 0) {
            this.session.loadUri(url);

            // Wait a couple of seconds before showing the WebView
            new CountDownTimer(2000, 2000) {
                public void onTick(long m) {}
                public void onFinish() { showWebView(); }
            }.start();
        }

    }

    /**
     * Convert a JSONObject to a ByteBuffer.
     *
     * @param object A JSONObject to be converted
     * @return ByteBuffer A ByteBuffer to be sent via fetch()
     */
    private ByteBuffer jsonObjectToByteBuffer(JSONObject object) {
        String string = object.toString();
        CharBuffer charBuffer = CharBuffer.wrap(string);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(charBuffer.length());
        Charset.forName("UTF-8").newEncoder().encode(charBuffer, byteBuffer, true);
        return byteBuffer;
    }

    /**
     * Convert an InputStream to a JSONObject
     *
     * @param inputStream The InputStream to convert
     * @return JSONObject
     */
    private JSONObject inputStreamToJsonObject(InputStream inputStream) {
        BufferedReader streamReader = null;

        // Try to decode input stream
        try {
            streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        } catch (UnsupportedEncodingException exception) {
            Log.e("Kitfox", "Unsupported encoding from InputStream");
        }
        StringBuilder stringBuilder = new StringBuilder();
        String inputString;
        try {
            while ((inputString = streamReader.readLine()) != null)
                stringBuilder.append(inputString);
        } catch (IOException exception) {
            Log.e("Kitfox", "I/O exception while reading InputStream");
        }


        // Try to parse string as JSON
        String string = stringBuilder.toString();
        JSONObject object = new JSONObject();
        try {
            object = new JSONObject(string);
        } catch(JSONException exception) {
            Log.e("Kitfox", "Invalid JSON");
        }

        return object;
    }
}