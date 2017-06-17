package android.com.buyhatke;

import android.content.Intent;
import android.os.Bundle;
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
import android.widget.Toast;

/**
 * Created by shobhit on 16/6/17.
 */

public class WebViewActivity extends AppCompatActivity {

    private static final String TAG = "WebViewActivity";
    private WebView webView;
    private ImageButton button;
    private EditText editText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview_layout);

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

                if ((url.contains(".myntra.") || url.contains(".jabong.")) && url.contains("/cart/")) {
                    startService(new Intent(WebViewActivity.this, FloatingViewService.class));
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
}
