package interactive1.com.smsgateway.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.telephony.IExtendedNetworkService;

import interactive1.com.smsgateway.MainActivity;
import interactive1.com.smsgateway.util.Utility;

/**
 * Created by dtomic on 09/07/15.
 */
public class ExtendedNetworkService extends Service {

    private static final String TAG = "ExtendedNetworkService";


//    BroadcastReceiver receiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if(intent.getAction().equals(Intent.ACTION_INSERT)){
//                //activity wishes to listen to USSD returns, so activate this
//                mActive = true;
//                Log.d(TAG, "activate ussd listener");
//            }
//            else if(intent.getAction().equals(Intent.ACTION_DELETE)){
//                mActive = false;
//                Log.d(TAG, "deactivate ussd listener");
//            }
//        }
//    };

    IExtendedNetworkService.Stub binder = new IExtendedNetworkService.Stub() {

        public void setMmiString(String number) throws RemoteException {
            Log.i(TAG, "setMmiString:" + number);
        }

        public CharSequence getMmiRunningText() throws RemoteException {

            return "USSD Running";
        }

        public void clearMmiString() throws RemoteException {
            Log.i(TAG, "called clear");
        }


        public CharSequence getUserMessage(CharSequence text)
                throws RemoteException {

            Log.i(TAG, "Message : " + text);

            if(!Utility.readBooleanPreferences(getApplicationContext(), Utility.KEY_CLOSE_DIALOG_FLAG, false)){
                //listener is still inactive, so return whatever we got
                Log.d(TAG, "inactive " + text);
                return text;
            }


            Intent returnIntent = new Intent(MainActivity.BROADCAST_USSD_ACTION);
            returnIntent.putExtra(MainActivity.USSD_TEXT, text);
            sendBroadcast(returnIntent);

            return null;

        }

    };

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "ExtendedNetworkService onBind");


//        IntentFilter filter = new IntentFilter();
//        filter.addAction(Intent.ACTION_INSERT);
//        filter.addAction(Intent.ACTION_DELETE);
//        filter.addDataScheme(getBaseContext().getString(R.string.uri_scheme));
//        filter.addDataAuthority(getBaseContext().getString(R.string.uri_authority), null);
//        filter.addDataPath(getBaseContext().getString(R.string.uri_path), PatternMatcher.PATTERN_LITERAL);
//        registerReceiver(receiver, filter);


        return binder;
    }

    @Override
    public void onCreate() {

        Log.d(TAG, "ExtendedNetworkService Started..");

        super.onCreate();
    }


}
