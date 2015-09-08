package interactive1.com.smsgateway;

import android.app.Application;

import com.splunk.mint.Mint;

/**
 * Created by dtomic on 03/09/15.
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Mint.enableDebug();
        Mint.initAndStartSession(this, "fa902690");

    }
}
