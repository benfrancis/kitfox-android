package org.mozilla.kitfox;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

public class MainActivity extends Activity {

    private static final String HOME_PAGE = "http://kitfox.tola.me.uk";
    private GeckoView geckoview;
    private GeckoSession session;
    private EditText urlBar;
    private ListView chatView;
    private ChatArrayAdapter chatArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        geckoview = findViewById(R.id.geckoview);
        session = new GeckoSession();
        GeckoRuntime runtime = GeckoRuntime.create(this);

        session.open(runtime);
        geckoview.setSession(session);
        session.loadUri(HOME_PAGE);

        urlBar = findViewById(R.id.urlbar);

        chatView = findViewById(R.id.chat_view);
        chatArrayAdapter = new ChatArrayAdapter(getApplicationContext(), R.layout.incoming_message);
        chatView.setAdapter(chatArrayAdapter);

        /**
         * Search or navigate to URL on submit.
         */
        urlBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String url = urlBar.getText().toString();
                    if (url.length() == 0) {
                        return false;
                    }

                    // Navigate to URL or submit message
                    if (URLUtil.isValidUrl(url) && url.contains(".")) {
                        session.loadUri(url);
                    } else if (URLUtil.isValidUrl("http://" + url) && url.contains(".")) {
                        session.loadUri("http://" + url);
                    } else {
                        addChatMessage();
                        showChatView();
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
     * Navigate to home page.
     *
     * @param View view
     */
    public void goHome(View view) {
        chatView.setVisibility(View.GONE);
        geckoview.setVisibility(View.VISIBLE);
        session.loadUri(HOME_PAGE);
        urlBar.setText("");
    }

    private void showChatView() {
        geckoview.setVisibility(View.GONE);
        chatView.setVisibility(View.VISIBLE);
    }

    private void addChatMessage() {
        chatArrayAdapter.add(new ChatMessage(false, urlBar.getText().toString()));
        urlBar.setText("");
    }
}
