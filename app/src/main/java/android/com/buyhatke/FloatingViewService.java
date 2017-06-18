package android.com.buyhatke;

import android.app.Service;
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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.util.ArrayList;

import static android.com.buyhatke.WebViewActivity.KEY;

/**
 * Created by shobhit on 17/6/17.
 */

public class FloatingViewService extends Service implements FetchDataListener {

    private final String TAG = "FloatingViewService";
    private WindowManager mWindowManager;
    private View mFloatingView;

    private LinearLayout expandedView;
    private ArrayList<WebView> webViews = new ArrayList<>();
    private ArrayList<TextView> discountedPrices = new ArrayList<>();
    private String[] coupons;

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
        expandedView = (LinearLayout) mFloatingView.findViewById(R.id.expanded_container);

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

    private String getBestDiscount() {
        int price = 0;
        int index = -1;
        for (TextView priceTextView : discountedPrices) {
            if (!priceTextView.getText().equals("")) {
                int discountedPrice = Integer.parseInt(priceTextView.getText().toString().trim());

                if (discountedPrice < price || index == -1) {
                    price = discountedPrice;
                    index = discountedPrices.indexOf(priceTextView);
                }
            }
        }

        return coupons[index];
    }

    private void initWebView(final String coupon, final WebView webView, final UpdatePrice listener) {

        CookieManager cookieManager = CookieManager.getInstance();
        CookieSyncManager.createInstance(this);

        cookieManager.setAcceptCookie(true);
        cookieManager.acceptCookie();
        CookieSyncManager.getInstance().startSync();


            /* An instance of this class will be registered as a JavaScript interface */
        class MyJavaScriptInterface {
            @JavascriptInterface
            @SuppressWarnings("unused")
            public void processHTML(String html) {
                // process the html as needed by the app

                Element content;
                String value = "";
                try {
                    org.jsoup.nodes.Document doc = Jsoup.parse(html, "UTF-8");
                    content = doc.getElementsByClass("rupee").get(0);
                    value = content.text();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                listener.update(value);
            }
        }

        webView.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");

        webView.setWebViewClient(new WebViewClient() {

            //If you will not use this method url links are open in new browser not in webview
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                Log.d(TAG, "onLoadResource: " + url + " : " + coupon);
                super.onLoadResource(view, url);
            }

            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "onPageFinished: " + url + " : " + coupon);

                if (url.contains("cart")) {

                    if (url.contains(".jabong.")) {

                        if (url.contains("m.jabong.com/cart/coupon/")) {
                            Log.d(TAG, coupon);
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

        configWebView(webView);
    }

    private void configWebView(WebView webView) {
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

        for (String coupon : coupons) {

            View view = View.inflate(getBaseContext(), R.layout.item_layout, null);

            TextView couponView = (TextView) view.findViewById(R.id.coupon);
            final TextView priceView = (TextView) view.findViewById(R.id.price);
            final WebView webView = (WebView) view.findViewById(R.id.itemWebView);

//            priceView.setText("");
            couponView.setText(coupon);

            discountedPrices.add(priceView);

            UpdatePrice updatePriceListener = new UpdatePrice() {
                @Override
                public void update(String price) {
                    priceView.setText(price);
                }
            };

            initWebView(coupon, webView, updatePriceListener);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            expandedView.addView(view, params);

            refresh(webView);
            //webViews.add(webView);
        }

        //runTasks();
    }

    private void runTasks() {
        for (WebView webView : webViews) {
            refresh(webView);
        }

//        String discount = getBestDiscount();
//        Toast.makeText(getBaseContext(), discount, Toast.LENGTH_SHORT).show();
    }

    public void refresh(WebView webView) {
        webView.loadUrl("http://m.jabong.com/cart/coupon/");
    }
}
