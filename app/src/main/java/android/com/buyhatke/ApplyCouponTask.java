package android.com.buyhatke;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by shobhit on 17/6/17.
 */

public class ApplyCouponTask extends AsyncTask<String, Void, String> {
    private final String TAG = "ApplyCouponTask";
    private final String API = "http://www.jabong.com/cart/applycoupon/";
    private String coupon;
    private FetchDataListener listener;

    public ApplyCouponTask() {
        super();
    }

    public void setArgs(FetchDataListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
        Log.i(TAG, "applying coupons");
        listener.preExecute();
    }

    @Override
    protected void onPostExecute(String result) {
        if (result == null)
            result = "";
        Log.d(TAG, " response : " + result);
        listener.postExecute(result);
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            coupon = params[0];
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("couponcode", params[0]);

            String entity = jsonObject.toString();

            URL url = new URL(API);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.connect();

            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(entity);
            writer.close();

            Log.d(TAG, "Entity " + entity);

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                sb.append(line);
            String result = sb.toString();
            connection.disconnect();

            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
