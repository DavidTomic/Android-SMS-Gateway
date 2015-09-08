package interactive1.com.smsgateway.task;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

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

import interactive1.com.smsgateway.util.Utility;

/**
 * Created by dtomic on 08/07/15.
 */
public class SendMessagesIdListTask extends AsyncTask<Void, Void, Boolean> {

    private static final String URL = Utility.URL_BASE + Utility.URL_MESSAGES;
    private static final String TAG = "SendMessagesIdListTask";

    private int status;
    private List<Integer> idList;
    private String accountToken;

    public SendMessagesIdListTask(int status, String accountToken, List<Integer> idList) {
        this.status = status;
        this.accountToken = accountToken;
        this.idList = idList;
    }

    @Override
    protected Boolean doInBackground(Void... params) {

        HttpURLConnection conn = null;
        String response = "";
        try {
            URL url = new URL(URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(10000);
            conn.setRequestMethod("PUT");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("X-Api-Key", accountToken);

            Log.i(TAG, "status " + status + "  ListIds " + idList);

            Uri.Builder builder = new Uri.Builder();


            for (int i = 0; i < idList.size(); i++) {
                builder.appendQueryParameter("id", "" + idList.get(i).intValue());
            }

            builder.appendQueryParameter("status", "" + status);

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
            idList.clear();
        }
    }
}
