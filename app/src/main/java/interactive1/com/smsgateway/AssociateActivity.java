package interactive1.com.smsgateway;

import android.app.Activity;
import android.content.Context;
import android.graphics.PointF;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import interactive1.com.smsgateway.task.SendAssociateTask;


public class AssociateActivity extends Activity implements QRCodeReaderView.OnQRCodeReadListener {

    private static final String TAG = "AssociateActivity";
    private QRCodeReaderView mydecoderview;
    private RelativeLayout progress;
    private boolean isSendedAssociateTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_associate);

        progress = (RelativeLayout) findViewById(R.id.rlProgress);

        mydecoderview = (QRCodeReaderView) findViewById(R.id.qrdecoderview);
        mydecoderview.setOnQRCodeReadListener(this);

        final EditText et = (EditText) findViewById(R.id.etAssociate);

        et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))
                        || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    View view = AssociateActivity.this.getCurrentFocus();
                    InputMethodManager inputManager = (InputMethodManager)
                            AssociateActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(view.getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);
                }
                return false;
            }
        });

        Button bSend = (Button) findViewById(R.id.bSendAssociate);
        bSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendAssociateTask(et.getText().toString());

                mydecoderview.getCameraManager().startPreview();
            }
        });
    }

    @Override
    public void onQRCodeRead(String code, PointF[] pointFs) {

        mydecoderview.getCameraManager().stopPreview();

        if (!isSendedAssociateTask) {
            isSendedAssociateTask = true;
            sendAssociateTask(code);
        }


    }

    @Override
    public void cameraNotFound() {

    }

    @Override
    public void QRCodeNotFoundOnCamImage() {

    }

    @Override
    protected void onResume() {
        super.onResume();

        mydecoderview.getCameraManager().startPreview();

    }

    @Override
    protected void onPause() {
        super.onPause();

        mydecoderview.getCameraManager().stopPreview();
    }

    private void sendAssociateTask(String code) {

        // Log.i(TAG, "sendAssociateTask");

        progress.setVisibility(View.VISIBLE);

        String deviceId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

        TelephonyManager telephonyManager = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE));
        String operatorName = telephonyManager.getNetworkOperatorName();

        SendAssociateTask aTask = new SendAssociateTask(AssociateActivity.this);
        aTask.execute(deviceId, code, operatorName, getDeviceName());
    }

    private String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }


    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
}
