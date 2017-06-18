package android.com.buyhatke.tasks;

import android.com.buyhatke.interfaces.FetchDataListener;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by shobhit on 17/6/17.
 */

public class FetchCouponCodeTask extends AsyncTask<Integer, Void, String> {
    public static final String API = "http://coupons.buyhatke.com/PickCoupon/FreshCoupon/getCoupons.php?pos=";
    private static final String TAG = "FetchCouponTask";
    private FetchDataListener listener;

    public FetchCouponCodeTask() {
        super();
    }

    @Override
    protected String doInBackground(Integer... params) {
        try {
            String urlString = API + params[0];

            Log.d(TAG, urlString);

            URL url = new URL(urlString);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

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


    public void setArgs(FetchDataListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
        Log.i(TAG, "fetching coupons");
        listener.preExecute();
    }

    @Override
    protected void onPostExecute(String result) {
        if (result == null)
            result = "";
        Log.d(TAG, " response : " + result);
        listener.postExecute(result);
    }
}
