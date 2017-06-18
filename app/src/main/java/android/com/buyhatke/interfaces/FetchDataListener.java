package android.com.buyhatke.interfaces;

/**
 * Created by shobhit on 17/6/17.
 */

public interface FetchDataListener {
    void preExecute();

    void postExecute(String result);
}