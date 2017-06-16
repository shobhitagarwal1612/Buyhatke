package android.com.buyhatke;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private CustomTabsClient mClient;
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.button);
        editText = (EditText) findViewById(R.id.editText);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    runChromeCustomTab();
                } catch (Exception e) {
                    Toast.makeText(getBaseContext(), "error", Toast.LENGTH_SHORT).show();
                    runViaWebView();
                }
            }
        });

        CustomTabsServiceConnection mConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName componentName, CustomTabsClient customTabsClient) {
                mClient = customTabsClient;
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mClient = null;
            }
        };

        String packageName = "com.android.chrome1";
        CustomTabsClient.bindCustomTabsService(this, packageName, mConnection);
    }

    private void runViaWebView() {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra("URL", editText.getText().toString().trim());
        startActivity(intent);
    }

    private void runChromeCustomTab() {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder(getSession());
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(getBaseContext(), Uri.parse(editText.getText().toString().trim()));
    }

    private CustomTabsSession getSession() {
        return mClient.newSession(new CustomTabsCallback() {
            @Override
            public void onNavigationEvent(int navigationEvent, Bundle extras) {
                super.onNavigationEvent(navigationEvent, extras);
            }
        });
    }
}
