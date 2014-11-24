package com.camlab.alexsloyandexdisktest;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends Activity {


    // oauth params
    public static final String CLIENT_ID = "f545d48d683b484a8b385b37e74f2988";
    public static final String CLIENT_SECRET = "9e388abbb65240c5a1165ce5fc2cf814";
    public static final String AUTH_URL = "https://oauth.yandex.ru/authorize?response_type=token&client_id="+CLIENT_ID;

    public static String FRAGMENT_TAG = "list";
    public static String TOKEN = "yandexdisk.token";

    private WebView mWebView;
    Button mExitButton, mDiskButton;
    TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mWebView = (WebView) findViewById(R.id.webView);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String token = preferences.getString(TOKEN, null);

        mExitButton = (Button) findViewById(R.id.exitButton);
        mExitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                System.exit(0);
            }
        });

        mDiskButton = (Button) findViewById(R.id.diskButton);
        mDiskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processToken();
                processYDisk();
            }
        });

        mTextView = (TextView) findViewById(R.id.textView);

        requestToken();

    }

    private void requestToken() {
        mWebView.loadUrl(AUTH_URL);
    }


    private void processToken() {
        String data = mWebView.getUrl();
        Pattern tokenPattern = Pattern.compile("access_token=(.*?)($|&)");
        Matcher tokenMatcher = tokenPattern.matcher(data);
        if (tokenMatcher.find()) {
            String token = tokenMatcher.group(1);
            if (!TextUtils.isEmpty(token)) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor.putString(TOKEN, token);
                editor.commit();
                Log.d("oauth", "Token has been successfully saved");
            } else {
                Log.w("oauth", "Empty token");
            }
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.authError), Toast.LENGTH_LONG).show();
            Log.w("oauth", "Token hasn't been found in return uri");
        }
    }

    private void processYDisk() {
        disableAuthElements();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new ListPhotoframeFragment(), FRAGMENT_TAG)
                .commit();
    }

    private void disableAuthElements () {
        setContentView(R.layout.empty_activity);

    }
    private void enableAuthElements () {
        setContentView(R.layout.main_activity);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



}
