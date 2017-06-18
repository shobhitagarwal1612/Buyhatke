package android.com.buyhatke.tasks;

import android.com.buyhatke.ApplyCoupon;
import android.os.AsyncTask;

import java.util.ArrayList;

/**
 * Created by shobhit on 18/6/17.
 */

public class ApplyCouponTask extends AsyncTask {

    private ArrayList<ApplyCoupon> tasksList;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        ((ApplyCoupon) values[0]).runTask();
        super.onProgressUpdate(values);
    }

    @Override
    protected Object doInBackground(Object[] params) {

        for (ApplyCoupon task : tasksList) {
            publishProgress(task);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public void setArgs(ArrayList<ApplyCoupon> tasks) {
        tasksList = tasks;
    }
}
