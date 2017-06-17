package android.com.buyhatke;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;

/**
 * Created by shobhit on 16/6/17.
 */

public class WebViewActivity extends AppCompatActivity {

    public static final int KEY_JABONG = 1;
    public static final int KEY_MYNTRA = 2;
    public static final String KEY = "key_url";

    private static final String TAG = "WebViewActivity";
    SharedPreferences sharedPreferences;
    private WebView webView;
    private ImageButton button;
    private EditText editText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview_layout);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        String url = getIntent().getExtras().getString("URL");

        webView = (WebView) findViewById(R.id.webView);
        editText = (EditText) findViewById(R.id.editText);
        button = (ImageButton) findViewById(R.id.button);

        editText.setText(url);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startWebView(editText.getText().toString());
            }
        });

        CookieManager cookieManager = CookieManager.getInstance();
        CookieSyncManager.createInstance(this);

        webView = (WebView) findViewById(R.id.webView);
        cookieManager.setAcceptCookie(true);
        cookieManager.acceptCookie();
        CookieSyncManager.getInstance().startSync();


        startWebView(url);
    }

    private void startWebView(final String url) {

        webView.setWebViewClient(new WebViewClient() {

            //If you will not use this method url links are open in new browser not in webview
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            public void onLoadResource(WebView view, String url) {
                Log.d(TAG, "onLoadResource: " + url);

                if (url.contains("cart")) {

                    if (isServiceRunning(FloatingViewService.class)) {
                        return;
                    }

                    int value = 0;
                    if (url.contains(".jabong.")) {
                        value = KEY_JABONG;
                    } else if (url.contains(".myntra.")) {
                        value = KEY_MYNTRA;
                    }

                    if (value != 0) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt(KEY, value);
                        editor.apply();

                        startService(new Intent(WebViewActivity.this, FloatingViewService.class));

                    }
                }
            }

            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "onPageFinished: " + url);
                editText.setText(url);
            }

        });

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setAppCacheEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setDomStorageEnabled(true);
//        webView.getSettings().setLoadsImagesAutomatically(true);
//        webView.getSettings().setLoadWithOverviewMode(true);
//        webView.getSettings().setUseWideViewPort(true);
//        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
//        webView.setScrollbarFadingEnabled(false);
//        webView.getSettings().setBuiltInZoomControls(true);

        webView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
