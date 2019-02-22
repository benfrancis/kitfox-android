package org.mozilla.kitfox;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebRequest;
import org.mozilla.geckoview.GeckoWebExecutor;
import org.mozilla.geckoview.WebResponse;

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
    private EditText urlBar;
    private ListView chatView;
    private ChatArrayAdapter chatArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        geckoview = findViewById(R.id.geckoview);
        session = new GeckoSession();
        runtime = GeckoRuntime.create(this);

        session.open(runtime);
        geckoview.setSession(session);
        session.loadUri(HOME_PAGE);

        urlBar = findViewById(R.id.urlbar);

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
    }

    /**
     * Show web view and navigate to home page.
     *
     * @param View view
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
     * @param WebResponse response Response received.
     */
    private void handleMessageResponse(WebResponse response) {
        // Detect error responses
        if (response.statusCode != 200) {
            Log.e("Kitfox", "Received bad response from server " + response.statusCode);
            return;
        }

        // Parse response
        JSONObject jsonResponse = this.byteBuffertoJsonObject(response.body);

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
        if (text.length() > 0) {
            this.showChatMessage(true, text);
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
     * @param JSONObject object A JSONObject to be converted
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
     * Convert a ByteBuffer to a JSONObject.
     * @param ByteBuffer buffer Buffer to convert.
     * @return JSONObject
     */
    private JSONObject byteBuffertoJsonObject(ByteBuffer buffer) {
        CharBuffer charBuffer = Charset.forName("UTF-8").decode(buffer);
        String string = charBuffer.toString();
        JSONObject object = new JSONObject();

        // Try to parse JSON
        try {
            object = new JSONObject(string);
        } catch(JSONException exception) {
          Log.e("Kitfox", "Invalid JSON");
        }

        return object;
    }
}