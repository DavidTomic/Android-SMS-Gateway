package interactive1.com.smsgateway.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import interactive1.com.smsgateway.object.Message;
import interactive1.com.smsgateway.object.MessageSource;
import interactive1.com.smsgateway.task.SendMessagesIdListTask;
import interactive1.com.smsgateway.util.Utility;

public class SmsSentReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsSentReceiver";
    private int count;
    private boolean allMessagesSent;
    private boolean canSendNext = true;

    public SmsSentReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent arg1) {


        Bundle basket = arg1.getExtras();
        if (basket != null) {
            Log.i(TAG, "phoneNumber " + basket.getString("phoneNumber")
                    + " messageText " + basket.getString("messageText")
                    + " messageId " + basket.getInt("messageId"));
        }

        boolean success = false;

        switch (getResultCode()) {
            case Activity.RESULT_OK:
                //Toast.makeText(context, "SMS Sent", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "SMS Sent");
                success = true;
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                //	Toast.makeText(context, "SMS generic failure", Toast.LENGTH_SHORT)
                //			.show();
                Log.i(TAG, "SMS generic failure");
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                //	Toast.makeText(context, "SMS no service", Toast.LENGTH_SHORT)
                //			.show();
                Log.i(TAG, "SMS no service");
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
                //	Toast.makeText(context, "SMS null PDU", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "SMS null PDU");
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                //	Toast.makeText(context, "SMS radio off", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "SMS radio off");
                break;
        }

        count++;
        Log.i(TAG, "SmsSentReceiver sms RBR: " + count);

        if (count == MessageSource.getInstance().getMessageList().size()) {
            allMessagesSent = true;
            count = 0;
        }

        Log.i(TAG, "allMessagesSent " + allMessagesSent);

        if (success) {
            try {
                List<Integer> list = MessageSource.getInstance().getSuccessMessagesIdList();
                list.add(basket.getInt("messageId"));

                if (list.size() >= 3) {
                    List<Integer> list2 = MessageSource.getInstance().getSuccessMessagesIdListTemp();

                    for (int i = 0; i < list.size(); i++) {
                        list2.add(list.get(i));
                    }

                    list.clear();

                    sendMessagesIdList(context, 3, list2);

                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            try {
                List<Integer> list = MessageSource.getInstance().getUnSuccessMessagesIdList();
                list.add(basket.getInt("messageId"));

                if (list.size() >= 3) {
                    List<Integer> list2 = MessageSource.getInstance().getUnSuccessMessagesIdListTemp();

                    for (int i = 0; i < list.size(); i++) {
                        list2.add(list.get(i));
                    }

                    list.clear();

                    sendMessagesIdList(context, 4, list2);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        setCanSendNext(true);


        if (allMessagesSent) {
            List<Integer> succIdList = MessageSource.getInstance().getSuccessMessagesIdList();
            List<Integer> unSuccIdList = MessageSource.getInstance().getUnSuccessMessagesIdList();

            if (succIdList.size() > 0) {
                List<Integer> newList = new ArrayList<Integer>();
                sendMessagesIdList(context, 3, Utility.copyListToList(succIdList, newList));
            }

            if (unSuccIdList.size() > 0) {
                List<Integer> newList = new ArrayList<Integer>();
                sendMessagesIdList(context, 4, Utility.copyListToList(unSuccIdList, newList));
            }

            MessageSource.getInstance().empty();
            count = 0;
            allMessagesSent = false;
        }
    }

    public void setCanSendNext(boolean value) {
        this.canSendNext = value;
    }

    public boolean isAllowToSendNext() {
        return canSendNext;
    }

    private void sendMessagesIdList(Context context, int status, List<Integer> idList) {
        SendMessagesIdListTask stask = new SendMessagesIdListTask(status,
                Utility.readStringPreferences(context, Utility.KEY_ACCOUNT_TOKEN, ""), idList);
        stask.execute();
    }

}
