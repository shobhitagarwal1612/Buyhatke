package android.com.buyhatke.tasks;

import android.com.buyhatke.ApplyCoupon;
import android.com.buyhatke.interfaces.FetchDataListener;
import android.os.AsyncTask;

import java.util.ArrayList;

/**
 * Created by shobhit on 18/6/17.
 */

public class ApplyCouponTask extends AsyncTask {

    private ArrayList<ApplyCoupon> tasksList;
    private FetchDataListener listener;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        listener.preExecute();
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        listener.postExecute("");
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
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public void setArgs(ArrayList<ApplyCoupon> tasks) {
        tasksList = tasks;
    }

    public void setListener(FetchDataListener fetchDataListener) {
        listener = fetchDataListener;
    }
}
