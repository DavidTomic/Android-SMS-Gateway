package interactive1.com.smsgateway.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import interactive1.com.smsgateway.MainActivity;
import interactive1.com.smsgateway.task.SendFundsTask;
import interactive1.com.smsgateway.util.Utility;

/**
 * Created by dtomic on 14/07/15.
 */
public class IncomingSmsReceiver extends BroadcastReceiver {

    private static final String TAG = "IncomingSmsReceiver";

    public void onReceive(Context context, Intent intent) {

        // Retrieves a map of extended data from the intent.
        final Bundle bundle = intent.getExtras();

        try {

            if (bundle != null) {

                final Object[] pdusObj = (Object[]) bundle.get("pdus");

                String result = "";
                String senderNum = "";
                String smsNo ="";
                for (int i = 0; i < pdusObj.length; i++) {

                    SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
                    String phoneNumber = currentMessage.getDisplayOriginatingAddress();

                    senderNum = phoneNumber;
                    String message = currentMessage.getDisplayMessageBody();

                    smsNo = Utility.readStringPreferences(context, Utility.KEY_SMS_NUMBER, "");

                    if(!smsNo.contentEquals("") && senderNum.equals(smsNo))
                    {
                        Log.i(TAG, "abortBroadcast");
                        this.abortBroadcast();
                    }

                    result += message;

                } // end for loop

                Log.i(TAG, "senderNum " + senderNum);
                Log.i(TAG, "result " + result);

                if (context instanceof MainActivity && !smsNo.contentEquals("")) {
                    if (!Utility.readStringPreferences(context, Utility.KEY_ACCOUNT_TOKEN, "").contentEquals("")) {
                        SendFundsTask task = new SendFundsTask(Utility.
                                readStringPreferences(context, Utility.KEY_ACCOUNT_TOKEN, ""), (MainActivity) context);
                        task.execute("", result);
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception smsReceiver" + e);

        }
    }
}
