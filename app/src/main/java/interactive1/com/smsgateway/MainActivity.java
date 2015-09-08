package interactive1.com.smsgateway;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import interactive1.com.smsgateway.object.Message;
import interactive1.com.smsgateway.object.MessageSource;
import interactive1.com.smsgateway.receiver.IncomingSmsReceiver;
import interactive1.com.smsgateway.receiver.SmsSentReceiver;
import interactive1.com.smsgateway.service.ExtendedNetworkService;
import interactive1.com.smsgateway.task.MessagesDownloadTask;
import interactive1.com.smsgateway.task.SendFundsTask;
import interactive1.com.smsgateway.util.Utility;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public static final String BROADCAST_USSD_ACTION = "ussd_action";
    private static final String BROADCAST_SMS_SENT = "SMS_SENT";
    private static final int ACCESSIBILITY_REQUEST_CODE = 3;
    private static final int USSD_REQUEST_CODE = 4;
    private String ACCESSIBILITY_ID;
    public static final String USSD_TEXT = "ussd_text";

    public boolean pollInProgress = false;
    private Timer messageTimer;
    private Handler mPollHandler = new Handler();

    private SmsSentReceiver smsSentBroadcastReceiver = new SmsSentReceiver();
    private IncomingSmsReceiver incomingSmsReceiver = new IncomingSmsReceiver();
    private int countMessage = 0;
    private Button associateButton;

    private TextView tvBalance;
    private RelativeLayout mainLayout;

    public boolean ussdRequested, alertVisible;


    //Activity methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        ACCESSIBILITY_ID = getString(R.string.accessibility_id);

        tvBalance = (TextView) findViewById(R.id.tvState);
        mainLayout = (RelativeLayout) findViewById(R.id.rlMainLayout);

        associateButton = (Button) findViewById(R.id.bAssociate);
        associateButton.setOnClickListener(new View.OnClickListener() {
                                               @Override
                                               public void onClick(View v) {

                                                   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH
                                                           && !isAccessibilityEnabled(MainActivity.this, ACCESSIBILITY_ID)) {

                                                       showAccessibilityAlert();
                                                   } else {

                                                       if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH &&
                                                               !Utility.readBooleanPreferences(MainActivity.this, Utility.KEY_DEVICE_RESTARTED, false)) {
                                                           showRestartAlert();
                                                       } else {
                                                           startActivity(new Intent(MainActivity.this, AssociateActivity.class));
                                                       }
                                                   }
                                               }
                                           }

        );

    }
    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        // TODO Auto-generated method stub


        try {
            unregisterReceiver(broadcastUSSDReceiver);
            unregisterReceiver(smsSentBroadcastReceiver);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Utility.saveBooleanPreferences(MainActivity.this, Utility.KEY_CLOSE_DIALOG_FLAG, false);

        super.onDestroy();
    }
    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        Log.i(TAG, "onResume");
        registerReceiver(broadcastUSSDReceiver, new IntentFilter(BROADCAST_USSD_ACTION));
        registerReceiver(smsSentBroadcastReceiver, new IntentFilter(BROADCAST_SMS_SENT));
        registerReceiver(incomingSmsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
        if (!Utility.readStringPreferences(this, Utility.KEY_ACCOUNT_TOKEN, "").contentEquals("")) {
            callMessagesAsynchronousTask();
            associateButton.setVisibility(View.GONE);


            if (!isMyServiceRunning(ExtendedNetworkService.class) &&
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH &&
                    Utility.readBooleanPreferences(this, Utility.KEY_DEVICE_RESTARTED, false)) {
                Toast.makeText(this, getString(R.string.please_restart_your_device),
                        Toast.LENGTH_SHORT).show();
                Utility.saveBooleanPreferences(this, Utility.KEY_DEVICE_RESTARTED, false);
            }
        }

        Log.i(TAG, "sendingActive " + MessageSource.getInstance().isLock());

        if (MessageSource.getInstance().isLock()) {
            startSendingProccess();
        }

        invalidateOptionsMenu();


    }
    @Override
    protected void onPause() {

        Log.i(TAG, "onPause");
        try {
            unregisterReceiver(incomingSmsReceiver);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        mPollHandler.removeCallbacks(mPollRunnable);

        Log.i(TAG, "sendingActive " + MessageSource.getInstance().isLock());

        if (messageTimer != null) {
            messageTimer.cancel();
            messageTimer.purge();
            messageTimer = null;
        }

        ussdRequested = false;

        super.onPause();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "onActivityResult " + resultCode + " " + requestCode);

        if (resultCode == RESULT_OK) {
            if (requestCode == ACCESSIBILITY_REQUEST_CODE) {
                Log.i(TAG, "isEnabled " + isAccessibilityEnabled(this, ACCESSIBILITY_ID));

                if (isAccessibilityEnabled(this, ACCESSIBILITY_ID)) {

                }
            }
        } else if (resultCode == RESULT_CANCELED) {
            if (requestCode == USSD_REQUEST_CODE) {
                Log.i(TAG, "onActivityResult USSD_REQUEST_CODE");
                ussdRequested = true;
            }
        }
    }
    @Override
    public void onBackPressed() {

        if (MessageSource.getInstance().isLock()) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle(getString(R.string.sendingActive));
            alert.setMessage(getString(R.string.exitAbortSending))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MainActivity.this.finish();
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

            AlertDialog alertDialog = alert.create();
            alertDialog.show();
        } else {
            super.onBackPressed();
        }

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem item = menu.findItem(R.id.logout);
        if (!Utility.readStringPreferences(this, Utility.KEY_ACCOUNT_TOKEN, "").contentEquals("")) {
            item.setVisible(true);
        } else {
            item.setVisible(false);
        }


        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Log.i(TAG, "onOptionsItemSelected");

        int id = item.getItemId();

        if (id == R.id.logout) {
            showLogOutlert();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    //sending sms methods
    public void startSendingProccess() {

        Log.i(TAG, "startSendingProccess");

        final List<Message> messageList = MessageSource.getInstance().getMessageList();

        messageTimer = new Timer();
        messageTimer.schedule(new TimerTask() {

            @Override
            public void run() {


                if (countMessage >= messageList.size()) {
                    countMessage = 0;
                    finishSendingSMS();
                } else {
                    if (smsSentBroadcastReceiver.isAllowToSendNext()) {
                        smsSentBroadcastReceiver.setCanSendNext(false);
                        Message m = messageList.get(countMessage);
                        sendSMS(m.getPhoneNumber(), m.getText(), m.getId());
                        countMessage++;
                    }
                }

                Log.i(TAG, "Poslan sms RBR: " + countMessage);

            }
        }, 500, 3000);


    }
    private void sendSMS(String phoneNumber, String messageText, int messageId) {

        Intent i1 = new Intent(BROADCAST_SMS_SENT);
        i1.putExtra("messageId", messageId);
        i1.putExtra("messageText", messageText);
        i1.putExtra("phoneNumber", phoneNumber);
        PendingIntent sentPI = PendingIntent.getBroadcast(MainActivity.this, 0,
                i1, PendingIntent.FLAG_UPDATE_CURRENT);

        Log.i(TAG, "sendSMS phonenumber " + phoneNumber + " messageText "
                + messageText + " messageId " + messageId);

        try {

            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phoneNumber, null, messageText, sentPI, null);


        } catch (Exception e) {

            e.printStackTrace();
            Log.i(TAG, "SMS sending failed");

        }

    }
    private void finishSendingSMS() {

        Log.i(TAG, "finishSendingSMS " + MessageSource.getInstance().getSuccessMessagesIdList());

        messageTimer.cancel();
        messageTimer.purge();
        messageTimer = null;

    }


    //show messages methods
    private void showAccessibilityAlert() {

        if (alertVisible)
            return;

        alertVisible = true;

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.enableServiceTitle);
        alert.setMessage(getString(R.string.enableServiceText))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.settings), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertVisible = false;
                        Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        startActivityForResult(intent, ACCESSIBILITY_REQUEST_CODE);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        alertVisible = false;
                    }
                });

        AlertDialog alertDialog = alert.create();
        alertDialog.show();
    }
    private void showRestartAlert() {
        if (alertVisible)
            return;

        alertVisible = true;

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getString(R.string.need_restart_device));
        alert.setMessage(getString(R.string.please_restart_your_device))
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertVisible = false;
                        dialog.dismiss();
                    }
                });

        AlertDialog alertDialog = alert.create();
        alertDialog.show();
    }
    private void showLogOutlert() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(getString(R.string.logOut));
        alert.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                logOut();

            }
        }).setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        alert.show();
    }


    //ussd funds methods
    public void checkUssdAndMessageProcess() {

        Log.i(TAG, "checkUssdAndMessageProcess 1");

        if (!Utility.readStringPreferences(this, Utility.KEY_ACCOUNT_TOKEN, "").contentEquals("")) {
            Log.i(TAG, "checkUssdAndMessageProcess 2");

            if (!Utility.readStringPreferences(this, Utility.KEY_USSD_CODE, "").contentEquals("")) {
                Log.i(TAG, "checkUssdAndMessageProcess 3");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH
                        && !isAccessibilityEnabled(MainActivity.this, ACCESSIBILITY_ID)) {
                    Log.i(TAG, "checkUssdAndMessageProcess 4");

                    showAccessibilityAlert();
                } else {
                    Log.i(TAG, "checkUssdAndMessageProcess 5");

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH &&
                            !Utility.readBooleanPreferences(MainActivity.this, Utility.KEY_DEVICE_RESTARTED, false)) {
                        showRestartAlert();
                    } else {
                        Log.i(TAG, "checkUssdAndMessageProcess 6");
                        //     Log.i(TAG, "ussdRequested " + ussdRequested);
                        if (!ussdRequested) {
                            sendUSSDRequest();
                        }

                    }

                }
            }


            if (!Utility.readStringPreferences(this, Utility.KEY_SMS_NUMBER, "").contentEquals("") &&
                    !Utility.readStringPreferences(this, Utility.KEY_SMS_TEXT, "").contentEquals("")) {
                Log.i(TAG, "sendSMS to check message number");
                Log.i(TAG, "number " + Utility.readStringPreferences(this, Utility.KEY_SMS_NUMBER, ""));
                Log.i(TAG, "text " + Utility.readStringPreferences(this, Utility.KEY_SMS_TEXT, ""));


                try {
                    SmsManager sms = SmsManager.getDefault();
                    Log.i(TAG, "SmsManager " + sms);
                    sms.sendTextMessage(Utility.readStringPreferences(this, Utility.KEY_SMS_NUMBER, ""),
                            null, Utility.readStringPreferences(this, Utility.KEY_SMS_TEXT, ""), null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private void sendUSSDRequest() {

        Log.i(TAG, "sendUSSDRequest ");

        Utility.saveBooleanPreferences(MainActivity.this, Utility.KEY_CLOSE_DIALOG_FLAG, true);
        String ussdCode = Utility.readStringPreferences(this, Utility.KEY_USSD_CODE, "");


        if (ussdCode.contains("#")) {
            ussdCode = ussdCode.substring(0, ussdCode.indexOf("#"));
        }

        if (!ussdCode.startsWith("*")) {
            ussdCode = "*" + ussdCode;
        }

        Log.i(TAG, "ussdCode " + ussdCode + "#");

        Intent i = new Intent("android.intent.action.CALL", Uri
                .parse("tel:" + ussdCode + Uri.encode("#")));
        startActivityForResult(i, USSD_REQUEST_CODE);
    }
    private BroadcastReceiver broadcastUSSDReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Utility.saveBooleanPreferences(MainActivity.this, Utility.KEY_CLOSE_DIALOG_FLAG, false);
            String ussedText = intent.getStringExtra(USSD_TEXT);
            Log.i(TAG, "onReceive " + ussedText);
            //    Log.i(TAG, "ACT " + Utility.readStringPreferences(context, Utility.KEY_ACCOUNT_TOKEN, ""));

            if (!Utility.readStringPreferences(context, Utility.KEY_ACCOUNT_TOKEN, "").contentEquals("")) {
                SendFundsTask task = new SendFundsTask(Utility.
                        readStringPreferences(context, Utility.KEY_ACCOUNT_TOKEN, ""), MainActivity.this);
                task.execute(ussedText, "");
            }

        }
    };

    public void responseToSendFundsTask(JSONObject json) {

        try {

            if (!json.isNull("funds"))
                tvBalance.setText(getString(R.string.balance) + ": " + json.getString("funds"));
            else if (!json.isNull("msg_amount"))
                tvBalance.setText(getString(R.string.sms_left) + ": " + json.getString("msg_amount"));
            else return;


            if (!json.isNull("color")) {
                int[] colors = {R.color.green, R.color.yellow, R.color.red};
                int index;
                switch (json.getString("color")) {
                    case "green":
                        index = 0;
                        break;
                    case "yellow":
                        index = 1;
                        break;
                    case "red":
                        index = 2;
                        break;
                    default:
                        index = 0;
                }

                mainLayout.setBackgroundColor(getResources().getColor(colors[index]));
            } else {
                mainLayout.setBackgroundColor(getResources().getColor(R.color.gray));
            }


        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NullPointerException ne) {
            ne.printStackTrace();
        }
    }


    //Others
    public void callMessagesAsynchronousTask() {
        Log.i(TAG, "callMessagesAsynchronousTask");
        mPollHandler.removeCallbacks(mPollRunnable);
        mPollHandler.postDelayed(mPollRunnable, 1000);
    }
    private Runnable mPollRunnable = new Runnable() {

        @Override
        public void run() {
            try {
                if (!pollInProgress && !MessageSource.getInstance().isLock()) {
                    Log.i(TAG, "started new Poll");
                    pollInProgress = true;
                    MessagesDownloadTask mTask = new MessagesDownloadTask(MainActivity.this);
                    mTask.execute();
                } else {
                    Log.i(TAG, "Poll in progress or proccesing messages");
                }

            } catch (Exception e) {
                // TODO Auto-generated catch block
            }
            mPollHandler.postDelayed(mPollRunnable, 10000);
        }
    };


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    @SuppressLint("NewApi")
    public static boolean isAccessibilityEnabled(Context context, String id) {

        AccessibilityManager am = (AccessibilityManager) context
                .getSystemService(Context.ACCESSIBILITY_SERVICE);

        List<AccessibilityServiceInfo> runningServices = am
                .getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);
        for (AccessibilityServiceInfo service : runningServices) {
            if (id.equals(service.getId())) {
                return true;
            }
        }

        return false;
    }
    public void logOut() {
        Utility.saveStringPreferences(MainActivity.this, Utility.KEY_USSD_CODE, "");
        Utility.saveStringPreferences(MainActivity.this, Utility.KEY_ACCOUNT_TOKEN, "");
        Utility.saveStringPreferences(MainActivity.this, Utility.KEY_SMS_NUMBER, "");
        Utility.saveStringPreferences(MainActivity.this, Utility.KEY_SMS_TEXT, "");


        mPollHandler.removeCallbacks(mPollRunnable);

        if (messageTimer != null) {
            messageTimer.cancel();
            messageTimer.purge();
            messageTimer = null;
        }

        pollInProgress = false;
        countMessage = 0;

        associateButton.setVisibility(View.VISIBLE);
        tvBalance.setText("");
        mainLayout.setBackgroundColor(getResources().getColor(R.color.gray));
        MessageSource.getInstance().empty();

        try {
            unregisterReceiver(smsSentBroadcastReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }

        invalidateOptionsMenu();
    }


    public void setTextAndColor(String text, int color){
        tvBalance.setText(text);
        mainLayout.setBackgroundColor(getResources().getColor(color));
    }

}
