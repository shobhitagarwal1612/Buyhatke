package android.com.buyhatke;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import static android.com.buyhatke.WebViewActivity.KEY;

/**
 * Created by shobhit on 17/6/17.
 */

public class FloatingViewService extends Service implements FetchDataListener {

    private final String TAG = "FloatingViewService";
    private WindowManager mWindowManager;
    private View mFloatingView;
    private TextView couponsView;
    private WebView webView;
    private String[] coupons;

    private int index = 0;

    public FloatingViewService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        //Initially view will be added to top-left corner
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, params);

        final View collapsedView = mFloatingView.findViewById(R.id.collapse_view);
        final View expandedView = mFloatingView.findViewById(R.id.expanded_container);

        webView = (WebView) mFloatingView.findViewById(R.id.webView);

        initWebView();

        couponsView = (TextView) mFloatingView.findViewById(R.id.coupons);

        ImageView closeButtonCollapsed = (ImageView) mFloatingView.findViewById(R.id.close_btn);
        closeButtonCollapsed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //close the service and remove the from from the window
                stopSelf();
            }
        });

        ImageView closeButton = (ImageView) mFloatingView.findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                collapsedView.setVisibility(View.VISIBLE);
                expandedView.setVisibility(View.GONE);
            }
        });

        mFloatingView.findViewById(R.id.root_container).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        initialX = params.x;
                        initialY = params.y;

                        //get the touch location
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        int Xdiff = (int) (event.getRawX() - initialTouchX);
                        int Ydiff = (int) (event.getRawY() - initialTouchY);

                        //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                        //So that is click event.
                        if (Xdiff < 10 && Ydiff < 10) {
                            if (isViewCollapsed()) {
                                collapsedView.setVisibility(View.GONE);
                                expandedView.setVisibility(View.VISIBLE);
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);

                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(mFloatingView, params);
                        return true;
                }
                return false;
            }
        });

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int value = sharedPreferences.getInt(KEY, 0);

        if (value != 0) {
            FetchCouponCodeTask getCoupons = new FetchCouponCodeTask();
            getCoupons.setArgs(this);
            getCoupons.execute(value);
        }
    }

    private void initWebView() {

        CookieManager cookieManager = CookieManager.getInstance();
        CookieSyncManager.createInstance(this);

        cookieManager.setAcceptCookie(true);
        cookieManager.acceptCookie();
        CookieSyncManager.getInstance().startSync();

        final Context myApp = this;

        /* An instance of this class will be registered as a JavaScript interface */
        class MyJavaScriptInterface {
            @JavascriptInterface
            @SuppressWarnings("unused")
            public void processHTML(String html) {
                // process the html as needed by the app

                Toast.makeText(getBaseContext(), html, Toast.LENGTH_SHORT).show();
                Log.d(TAG, html);

                org.jsoup.nodes.Document doc = Jsoup.parse(html, "UTF-8");

                Element content = doc.getElementsByClass("rupee").get(0);
                Toast.makeText(getBaseContext(), content.text(), Toast.LENGTH_SHORT).show();

                //webView.loadUrl("http://m.jabong.com/cart/coupon/");

            }
        }

        /* JavaScript must be enabled if you want it to work, obviously */
        webView.getSettings().setJavaScriptEnabled(true);

        webView.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");

        webView.setWebViewClient(new WebViewClient() {

            //If you will not use this method url links are open in new browser not in webview
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            public void onLoadResource(WebView view, String url) {
                Log.d(TAG, "onLoadResource: " + url);
            }

            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "onPageFinished: " + url);

                if (index >= coupons.length) {
                    return;
                }

                String coupon = coupons[index];

                if (url.contains("cart")) {

                    if (url.contains(".jabong.")) {

                        if (url.contains("m.jabong.com/cart/coupon/")) {
                            Log.d(TAG, coupon);
                            index++;
                            webView.loadUrl("javascript:(function(){" +
                                    "l=document.getElementById('applyCoupon');" +
                                    "l.value='" + coupon + "';" +
                                    "e=document.createEvent('HTMLEvents');" +
                                    "e.initEvent('click',true,true);" +
                                    "button=document.getElementsByClassName('jbApplyCoupon')[0];" +
                                    "button.dispatchEvent(e);" +
                                    "})()");
                        } else {

                            /* This call inject JavaScript into the page which just finished loading. */
                            webView.loadUrl("javascript:HTMLOUT.processHTML(document.documentElement.outerHTML);");

                        }
                    } else if (url.contains(".myntra.")) {

                        Toast.makeText(getBaseContext(), "Clicking", Toast.LENGTH_SHORT).show();

                        Log.d(TAG, "clicking myntra");
                        webView.loadUrl("javascript:(function(){" +
                                "l=document.getElementsByName('coupon_code')[0];" +
                                "l.value='INDIA10';" +
                                "e=document.createEvent('HTMLEvents');" +
                                "e.initEvent('click',true,true);" +
                                "button=document.getElementsByClassName('btn-apply')[0];" +
                                "button.dispatchEvent(e);" +
                                "})()");
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
    }

    /**
     * Detect if the floating view is collapsed or expanded.
     *
     * @return true if the floating view is collapsed.
     */
    private boolean isViewCollapsed() {
        return mFloatingView == null || mFloatingView.findViewById(R.id.collapse_view).getVisibility() == View.VISIBLE;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
    }

    @Override
    public void preExecute() {
        Toast.makeText(getBaseContext(), "Fetching coupons", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void postExecute(String result) {
        Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG).show();

        coupons = result.split("~");

        StringBuilder builder = new StringBuilder();
        for (String coupon : coupons) {
            builder.append(coupon).append("\n");
            ApplyCouponTask applyCouponTask = new ApplyCouponTask();
            applyCouponTask.setArgs(new FetchDataListener() {
                @Override
                public void preExecute() {

                }

                @Override
                public void postExecute(String result) {

                }
            });
            //applyCouponTask.execute(coupon);
        }

        couponsView.setText(builder.toString());

        webView.loadUrl("http://m.jabong.com/cart/coupon/");
    }
}
