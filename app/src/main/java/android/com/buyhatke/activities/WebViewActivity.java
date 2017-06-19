package android.com.buyhatke.activities;

import android.app.ActivityManager;
import android.com.buyhatke.R;
import android.com.buyhatke.service.FloatingViewService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
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

import static android.com.buyhatke.service.FloatingViewService.SERVICE_MESSAGE;
import static android.com.buyhatke.service.FloatingViewService.SERVICE_RESULT;

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
    private EditText editText;
    private BroadcastReceiver receiver;
    private String coupon;
    private boolean couponApplied = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview_layout);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        String url = getIntent().getExtras().getString("URL");

        webView = (WebView) findViewById(R.id.webView);
        editText = (EditText) findViewById(R.id.editText);
        ImageButton button = (ImageButton) findViewById(R.id.button);

        editText.setText(url);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startWebView(editText.getText().toString());
            }
        });

        CookieManager cookieManager = CookieManager.getInstance();
        CookieSyncManager.createInstance(this);

        cookieManager.setAcceptCookie(true);
        cookieManager.acceptCookie();
        CookieSyncManager.getInstance().startSync();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra(SERVICE_MESSAGE)) {
                    coupon = intent.getStringExtra(SERVICE_MESSAGE);

                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                    int value = sharedPreferences.getInt(KEY, 0);

                    if (value != 0) {
                        couponApplied = true;

                        if (value == KEY_JABONG) {
                            webView.loadUrl("javascript:(function(){" +
                                    "l=document.getElementById('applyCoupon');" +
                                    "l.value='" + coupon + "';" +
                                    "e=document.createEvent('HTMLEvents');" +
                                    "e.initEvent('click',true,true);" +
                                    "button=document.getElementsByClassName('jbApplyCoupon')[0];" +
                                    "button.dispatchEvent(e);" +
                                    "})()");
                        } else if (value == KEY_MYNTRA) {
                            webView.loadUrl("javascript:(function(){" +
                                    "l=document.getElementsByName('coupon_code')[0];" +
                                    "l.value='" + coupon + "';" +
                                    "e=document.createEvent('HTMLEvents');" +
                                    "e.initEvent('click',true,true);" +
                                    "button=document.getElementsByClassName('btn-apply')[0];" +
                                    "button.dispatchEvent(e);" +
                                    "})()");
                        }
                    }
                }
            }
        };

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

                if (url.contains("cart")) {

                    if (url.contains(".jabong.")) {
                        Log.d(TAG, "jabong cart");

                        if (!couponApplied) {
                            webView.loadUrl("http://m.jabong.com/cart/coupon/");
                        } else if (url.contains("m.jabong.com/cart/coupon/")) {
                            webView.loadUrl("http://m.jabong.com/cart/");
                        }
                    } else if (url.contains(".myntra.")) {
                        Log.d(TAG, "myntra cart");

                        /*if (couponApplied) {
                            webView.loadUrl("http://m.jabong.com/cart/coupon/");
                        } else if (url.contains("m.jabong.com/cart/coupon/")) {
                            webView.loadUrl("http://m.jabong.com/cart/");
                        }*/

                        /*webView.loadUrl("javascript:(function(){" +
                                "l=document.getElementsByName('coupon_code')[0];" +
                                "l.value='INDIA10';" +
                                "e=document.createEvent('HTMLEvents');" +
                                "e.initEvent('click',true,true);" +
                                "button=document.getElementsByClassName('btn-apply')[0];" +
                                "button.dispatchEvent(e);" +
                                "})()");*/
                    }
                }

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
        settings.setLoadsImagesAutomatically(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);

        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);
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

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver),
                new IntentFilter(SERVICE_RESULT)
        );
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }
}
