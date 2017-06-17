package android.com.buyhatke;

/**
 * Created by shobhit on 17/6/17.
 */

public interface FetchDataListener {
    void preExecute();

    void postExecute(String result);
}