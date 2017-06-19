package android.com.buyhatke.service;

import android.app.Service;
import android.com.buyhatke.ApplyCoupon;
import android.com.buyhatke.R;
import android.com.buyhatke.interfaces.FetchDataListener;
import android.com.buyhatke.tasks.ApplyCouponTask;
import android.com.buyhatke.tasks.FetchCouponCodeTask;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static android.com.buyhatke.activities.WebViewActivity.KEY;

/**
 * Created by shobhit on 17/6/17.
 */

public class FloatingViewService extends Service implements FetchDataListener {

    static final public String SERVICE_RESULT = "com.controlj.copame.backend.COPAService.REQUEST_PROCESSED";
    static final public String SERVICE_MESSAGE = "com.controlj.copame.backend.COPAService.COPA_MSG";
    private final String TAG = "FloatingViewService";
    private WindowManager mWindowManager;
    private View mFloatingView;
    private LinearLayout expandedView;
    private String[] coupons;
    private ArrayList<ApplyCoupon> tasks = new ArrayList<>();
    private ArrayList<TextView> discountedPrices = new ArrayList<>();
    private LocalBroadcastManager broadcaster;

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

        broadcaster = LocalBroadcastManager.getInstance(this);

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

    public void sendResult(String message) {
        Intent intent = new Intent(SERVICE_RESULT);
        if (message != null)
            intent.putExtra(SERVICE_MESSAGE, message);
        broadcaster.sendBroadcast(intent);
    }

    private String getBestDiscount() {
        float price = 0;
        int index = -1;
        for (TextView priceTextView : discountedPrices) {
            if (!priceTextView.getText().equals("")) {
                try {
                    float discountedPrice = Float.parseFloat(priceTextView.getText().toString().trim());

                    if (discountedPrice < price || index == -1) {
                        price = discountedPrice;
                        index = discountedPrices.indexOf(priceTextView);
                    }
                } catch (NumberFormatException e) {
                    //not a integer value, so ignore
                }
            }
        }

        if (index != -1) {
            return coupons[index];
        } else {
            return null;
        }
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
        try {
            if (mFloatingView != null) {
                mWindowManager.removeView(mFloatingView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            ApplyCoupon applyCoupon = new ApplyCoupon(getBaseContext(), coupon);

            View view = applyCoupon.initLayout();
            applyCoupon.initWebView();

            discountedPrices.add(applyCoupon.getPriceView());

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(5, 5, 5, 5);
            expandedView.addView(view, params);

            tasks.add(applyCoupon);
        }

        ApplyCouponTask applyCouponTask = new ApplyCouponTask();
        applyCouponTask.setArgs(tasks);
        applyCouponTask.setListener(new FetchDataListener() {

            @Override
            public void preExecute() {
                Toast.makeText(getBaseContext(), "Starting search...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void postExecute(final String result) {
                Toast.makeText(getBaseContext(), "Finished!", Toast.LENGTH_SHORT).show();

                final Handler seekHandler = new Handler();

                /*
                 * Background Runnable thread
                 */
                Runnable updateTimeTask = new Runnable() {
                    public void run() {
                        if (getDiscounts() < tasks.size()) {
                            seekHandler.postDelayed(this, 500);
                        } else {
                            finishTask();
                        }
                    }
                };

                seekHandler.postDelayed(updateTimeTask, 500);
            }
        });
        applyCouponTask.execute();
    }

    private void finishTask() {
        String discount = getBestDiscount();
        String title;
        String message;

        if (discount != null) {
            title = "Congratulations!!!";
            message = "Discount " + discount + " applied successfully";

        } else {
            title = "Sorry";
            message = "No coupons applicable";
        }

        Toast.makeText(getBaseContext(), title + "\n\n" + message, Toast.LENGTH_LONG).show();

        sendResult(discount);
        this.onDestroy();
    }

    private int getDiscounts() {
        int count = 0;
        for (TextView textView : discountedPrices) {
            if (!textView.getText().toString().equals("")) {
                count++;
            }
        }
        return count;
    }
}
