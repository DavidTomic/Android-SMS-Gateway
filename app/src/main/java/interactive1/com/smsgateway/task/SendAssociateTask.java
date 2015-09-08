package interactive1.com.smsgateway.task;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

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

import interactive1.com.smsgateway.AssociateActivity;
import interactive1.com.smsgateway.MainActivity;
import interactive1.com.smsgateway.util.Utility;

/**
 * Created by dtomic on 07/07/15.
 */
public class SendAssociateTask extends AsyncTask<String, Void, Boolean> {

    private static final String TAG = "SendAssociateTask";
    private static final String URL = Utility.URL_BASE + Utility.URL_ASSOCIATE;
    private AssociateActivity associateActivity;
    private String response = "";

    public SendAssociateTask(AssociateActivity act) {
        this.associateActivity = act;
    }

    @Override
    protected Boolean doInBackground(String... params) {

        String device_id = params[0];
        String association_code = params[1];
        String operatorName = params[2];
        String deviceName = params[3];

        HttpURLConnection conn = null;
        try {
            URL url = new URL(URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(10000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);


            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("device_token", device_id)
                    .appendQueryParameter("association_code", association_code)
                    .appendQueryParameter("operator", operatorName)
                    .appendQueryParameter("name", deviceName)
                    .appendQueryParameter("platform", "android")
                    .appendQueryParameter("version", Build.VERSION.RELEASE);
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
            if (associateActivity != null && !associateActivity.isFinishing()) {

                try {
                    JSONObject json = new JSONObject(response);

                    if (json.getInt("status") == 0) {
                        Utility.saveStringPreferences(associateActivity, Utility.KEY_ACCOUNT_TOKEN,
                                json.getString("token"));
                        associateActivity.finish();
                    }else {
                        showErrorAlert();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    showErrorAlert();
                }

            }
        }else {
            showErrorAlert();
        }
    }

    private void showErrorAlert(){
        AlertDialog.Builder alert = new AlertDialog.Builder(associateActivity);
        alert.setTitle("Error");
        alert.setMessage("Please try again!")
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        associateActivity.finish();
                        dialog.dismiss();
                    }
                });

        AlertDialog alertDialog = alert.create();
        alertDialog.show();
    }



}
