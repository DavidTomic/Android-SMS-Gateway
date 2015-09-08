package interactive1.com.smsgateway.task;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import interactive1.com.smsgateway.MainActivity;
import interactive1.com.smsgateway.R;
import interactive1.com.smsgateway.util.Utility;

/**
 * Created by dtomic on 08/07/15.
 */
public class SendFundsTask extends AsyncTask<String, Void, Boolean> {

    private static final String URL = Utility.URL_BASE + Utility.URL_PHONE;
    private static final String TAG = "SendFundsTask";

    private String accountToken;
    private String response = "";
    private MainActivity mainActivity;

    public SendFundsTask(String accountToken, MainActivity mainActivity) {
        this.accountToken = accountToken;
        this.mainActivity = mainActivity;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(10000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("X-Api-Key", accountToken);


            Uri.Builder builder = new Uri.Builder();
            builder.appendQueryParameter("ussd_text", "" + params[0]);
            builder.appendQueryParameter("sms_text", "" + params[1]);
            builder.appendQueryParameter("battery_level", "" + getBatteryLevel());
            String query = builder.build().getEncodedQuery();
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(query);
            writer.flush();
            writer.close();
            os.close();

            int responseCode = conn.getResponseCode();
            Log.i(TAG, "responseCode " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }

                Log.i(TAG, "response " + response);
            } else {
                return false;
            }


        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        Log.i(TAG, "result " + result);
        if (result) {

            try {
                JSONObject json = new JSONObject(response);

                if (json.getInt("status") == 0) {
                    mainActivity.responseToSendFundsTask(json);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(mainActivity, mainActivity.getString(R.string.checkUssd),
                    Toast.LENGTH_LONG).show();
        }
    }


    private int getBatteryLevel() {

        if (mainActivity == null) return 0;

        Intent intent = mainActivity.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int blevel = (level * 100) / scale;
        Log.i(TAG, "blevel " + blevel);
        return blevel;
    }
}
