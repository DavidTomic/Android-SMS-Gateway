package interactive1.com.smsgateway.service;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import interactive1.com.smsgateway.MainActivity;
import interactive1.com.smsgateway.util.Utility;

/**
 * Created by dtomic on 07/07/15.
 */
@SuppressLint("NewApi")
public class USSDAccessibilityService extends AccessibilityService {

    private static final String TAG = "USSDAccessibilityService";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        AccessibilityNodeInfo localAccessibilityNodeInfo1 = event.getSource();

        if (localAccessibilityNodeInfo1 == null) {
            return;
        }

//        Log.i(TAG, "event " + event.toString());
//        Log.i(TAG, "event text " + event.getText() + "  event class name "
//                + event.getClassName());
//        Log.i(TAG, "localAccessibilityNodeInfo1 " + localAccessibilityNodeInfo1.toString());

        AccessibilityNodeInfo buttonLocalAccessibilityNodeInfo = getDialogNode(
                localAccessibilityNodeInfo1, "Button");

        if (Utility.readBooleanPreferences(getApplicationContext(), Utility.KEY_CLOSE_DIALOG_FLAG, false)) {
            if (buttonLocalAccessibilityNodeInfo != null) {

                String ussdText = getText(localAccessibilityNodeInfo1);
                Log.i(TAG, "ussdText " + ussdText);

                try {
                    if (ussdText != null) {
                        buttonLocalAccessibilityNodeInfo.performAction(16);
                        buttonLocalAccessibilityNodeInfo.recycle();

                        Intent returnIntent = new Intent(MainActivity.BROADCAST_USSD_ACTION);
                        returnIntent.putExtra(MainActivity.USSD_TEXT, ussdText);
                        sendBroadcast(returnIntent);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Intent returnIntent = new Intent(MainActivity.BROADCAST_USSD_ACTION);
                    returnIntent.putExtra(MainActivity.USSD_TEXT, "Error");
                    sendBroadcast(returnIntent);
                }


            }
        }

    }

    private static AccessibilityNodeInfo getDialogNode(
            AccessibilityNodeInfo paramAccessibilityNodeInfo, String paramString) {

        //try find paramString at first level
        for (int i = 0; i < paramAccessibilityNodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo localAccessibilityNodeInfo = paramAccessibilityNodeInfo
                    .getChild(i);
         //   Log.i(TAG, "localAccessibilityNodeInfo child " + localAccessibilityNodeInfo);
            if (localAccessibilityNodeInfo.getClassName().toString()
                    .contains(paramString)) {
                return localAccessibilityNodeInfo;
            }
            localAccessibilityNodeInfo.recycle();
        }

        //try find paramString at second level
        for (int i = 0; i < paramAccessibilityNodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo localAccessibilityNodeInfo = paramAccessibilityNodeInfo
                    .getChild(i);

            for (int j = 0; j < localAccessibilityNodeInfo.getChildCount(); j++) {
                AccessibilityNodeInfo localAccessibilityNodeInfo2 = paramAccessibilityNodeInfo
                        .getChild(j);


          //      Log.i(TAG, "localAccessibilityNodeInfo2 child " + localAccessibilityNodeInfo2);
                if (localAccessibilityNodeInfo2.getClassName().toString()
                        .contains(paramString)) {
                    return localAccessibilityNodeInfo2;
                }
                localAccessibilityNodeInfo2.recycle();
            }

        }


        return null;
    }

    private static String getText(AccessibilityNodeInfo paramAccessibilityNodeInfo) {
        Object localObject = null;

    //    Log.i(TAG, "getChildCount " + paramAccessibilityNodeInfo.getChildCount());

        //try find text at first level
        for (int i = 0; i < paramAccessibilityNodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo localAccessibilityNodeInfo = paramAccessibilityNodeInfo
                    .getChild(i);

            //   Log.i(TAG, "getChildCount second " + localAccessibilityNodeInfo.getChildCount());


//            for (int j = 0; j < localAccessibilityNodeInfo.getChildCount(); j++) {
//                Log.i(TAG, "getChild " + localAccessibilityNodeInfo
//                        .getChild(j));
//            }


//            Log.i(TAG, "getText localAccessibilityNodeInfo " + localAccessibilityNodeInfo);

            if (localAccessibilityNodeInfo.getClassName().toString()
                    .contains("TextView")) {
                CharSequence localCharSequence = localAccessibilityNodeInfo
                        .getText();
                localAccessibilityNodeInfo.recycle();
                if (localCharSequence != null) {
                 //   Log.i(TAG, "HERE 1");
                    String str = localCharSequence.toString();
                    if ((str != null)
                            && ((localObject == null) || (((String) localObject).length() < str
                            .length()))) {
                      //  Log.i(TAG, "HERE 2");
                        localObject = str;
                    }
                }
            }
        }

        if (localObject == null) {
            //try find text at second level
            for (int i = 0; i < paramAccessibilityNodeInfo.getChildCount(); i++) {
                AccessibilityNodeInfo localAccessibilityNodeInfo = paramAccessibilityNodeInfo
                        .getChild(i);

                for (int j = 0; j < localAccessibilityNodeInfo.getChildCount(); j++) {
                    AccessibilityNodeInfo pom = localAccessibilityNodeInfo.getChild(j);

                    if (pom.getClassName().toString()
                            .contains("TextView")) {
                        CharSequence localCharSequence = pom
                                .getText();
                        pom.recycle();
                        if (localCharSequence != null) {
                            String str = localCharSequence.toString();
                            if ((str != null)
                                    && ((localObject == null) || (((String) localObject).length() < str
                                    .length()))) {
                                localObject = str;
                            }
                        }
                    }

                }
            }

        }

        return (String) localObject;
    }

    @Override
    protected void onServiceConnected() {
        // TODO Auto-generated method stub
        super.onServiceConnected();
        Log.i(TAG, "onServiceConnected");
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    public void onInterrupt() {
        // TODO Auto-generated method stub

    }
}
