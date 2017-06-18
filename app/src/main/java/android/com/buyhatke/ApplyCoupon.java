package android.com.buyhatke;

import android.com.buyhatke.interfaces.UpdatePrice;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/**
 * Created by shobhit on 18/6/17.
 */

public class ApplyCoupon implements UpdatePrice {

    private final Context context;
    private final String coupon;
    private final String TAG = "ApplyCoupon";
    private TextView couponView;
    private TextView priceView;
    private WebView webView;
    private UpdatePrice listener;

    public ApplyCoupon(Context context, String coupon) {
        this.context = context;
        this.coupon = coupon;
        listener = this;
    }

    public TextView getPriceView() {
        return priceView;
    }

    public View initLayout() {
        View view = View.inflate(context, R.layout.item_layout, null);

        couponView = (TextView) view.findViewById(R.id.coupon);
        priceView = (TextView) view.findViewById(R.id.price);
        webView = (WebView) view.findViewById(R.id.itemWebView);

        couponView.setText(coupon);

        return view;
    }

    @Override
    public void update(String price) {
        priceView.setText(price);
    }

    public void initWebView() {

        CookieManager cookieManager = CookieManager.getInstance();
        CookieSyncManager.createInstance(context);

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

            public void onPageFinished(WebView view, String url) {
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
                            Log.d(TAG, coupon + " : loaded");
                            webView.loadUrl("javascript:HTMLOUT.processHTML(document.documentElement.outerHTML);");
                        }
                    } else if (url.contains(".myntra.")) {

                        Toast.makeText(context, "Clicking", Toast.LENGTH_SHORT).show();

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

    public void runTask() {
        webView.loadUrl("http://m.jabong.com/cart/coupon/");
    }
}
