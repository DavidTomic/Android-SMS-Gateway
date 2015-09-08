package interactive1.com.smsgateway.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import interactive1.com.smsgateway.service.ExtendedNetworkService;
import interactive1.com.smsgateway.util.Utility;

/**
 * Created by dtomic on 09/07/15.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Log.i(TAG, "rcvd boot event, launching service");
            Utility.saveBooleanPreferences(context, Utility.KEY_DEVICE_RESTARTED, true);
            context.startService(new Intent(context, ExtendedNetworkService.class));
        }
    }
}
