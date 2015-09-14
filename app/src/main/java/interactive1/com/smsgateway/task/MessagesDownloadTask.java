package interactive1.com.smsgateway.task;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import interactive1.com.smsgateway.MainActivity;
import interactive1.com.smsgateway.R;
import interactive1.com.smsgateway.object.Message;
import interactive1.com.smsgateway.object.MessageSource;
import interactive1.com.smsgateway.util.Utility;

/**
 * Created by dtomic on 07/07/15.
 */
public class MessagesDownloadTask extends AsyncTask<Void, Void, Boolean> {

    private static final String URL = Utility.URL_BASE + Utility.URL_MESSAGES;
    private static final String TAG = "MessagesDownloadTask";

    private static final int INCOMPLETE = 0;
    private static final int FREE_SEND = 1;
    private static final int SEND_AMOUNT = 2;
    private static final int SET_FUNDS = 3;
    private static final int CHECK_BALANCE = 4;

    private MainActivity mainActivity;
    private String response = "";
    private int responseCode;
    private String accountToken;
    private MessageSource messageSource;
    private boolean checkUssdOrMessage;

    private boolean refreshTextAndColor;
    private String text = "";
    private int color = R.color.gray;

    public MessagesDownloadTask(MainActivity act) {
        this.mainActivity = act;
        this.accountToken = Utility.readStringPreferences(act, Utility.KEY_ACCOUNT_TOKEN, "");
        this.messageSource = MessageSource.getInstance();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        HttpURLConnection conn = null;
        try {
            java.net.URL url = new URL(URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(10000);
            conn.setRequestProperty("X-Api-Key", accountToken);


            responseCode = conn.getResponseCode();
            Log.i(TAG, "responseCode " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }

                Log.i(TAG, "response " + response);
                parseMessages(response);
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

            if (checkUssdOrMessage) {
                checkUssdOrMessage = false;
                mainActivity.ussdRequested = false;
                mainActivity.checkUssdAndMessageProcess();
            }


            //start sending sms messages
            if (messageSource.getMessageList().size() > 0) {
                messageSource.setLock(true);
                mainActivity.startSendingProccess();
            }

            if (refreshTextAndColor)
                mainActivity.setTextAndColor(text, color);

        } else if (responseCode == 403) {
            mainActivity.logOut();
        }

        mainActivity.pollInProgress = false;
    }

    private void parseMessages(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);

            if (jsonObject.getInt("status") != 0)
                return;


            JSONArray jsonArray = jsonObject.getJSONArray("messages");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                Message m = new Message();
                m.setId(object.getInt("id"));
                m.setPhoneNumber(object.getString("target_number"));
                m.setText(object.getString("message"));
                messageSource.getMessageList().add(m);
            }

            if (!jsonObject.isNull("check")) {
                JSONObject jsonCheckObject = jsonObject.getJSONObject("check");
                checkUssdOrMessage = false;

                if (!jsonCheckObject.isNull("ussd_enabled") && jsonCheckObject.getBoolean("ussd_enabled")) {
                    checkUssdOrMessage = true;
                    Utility.saveStringPreferences(mainActivity, Utility.KEY_USSD_CODE,
                            jsonCheckObject.getString("ussd_no"));
                } else {
                    Utility.saveStringPreferences(mainActivity, Utility.KEY_USSD_CODE,
                            "");
                }

                if (!jsonCheckObject.isNull("sms_enabled") && jsonCheckObject.getBoolean("sms_enabled")) {
                    checkUssdOrMessage = true;
                    Utility.saveStringPreferences(mainActivity, Utility.KEY_SMS_NUMBER,
                            jsonCheckObject.getString("sms_no"));
                    Utility.saveStringPreferences(mainActivity, Utility.KEY_SMS_TEXT,
                            jsonCheckObject.getString("sms_text"));
                } else {
                    Utility.saveStringPreferences(mainActivity, Utility.KEY_SMS_NUMBER,
                            "");
                    Utility.saveStringPreferences(mainActivity, Utility.KEY_SMS_TEXT,
                            "");
                }

            }


            if (!jsonObject.isNull("battery_check")) {
                SendFundsTask task = new SendFundsTask(Utility.
                        readStringPreferences(mainActivity, Utility.KEY_ACCOUNT_TOKEN, ""), mainActivity);
                task.execute("", "");
            }

            if (!jsonObject.isNull("plan")) {
                JSONObject planJO = jsonObject.getJSONObject("plan");

                refreshTextAndColor = true;
                switch (planJO.getInt("type")) {
                    case INCOMPLETE:
                        text = mainActivity.getString(R.string.please_choose_plan);
                        break;
                    case FREE_SEND:
                        text = mainActivity.getString(R.string.free_send);
                        break;
                    case SEND_AMOUNT:
                        text = mainActivity.getString(R.string.sms_left) +
                                ": " + planJO.getString("amount");
                        break;
                    case SET_FUNDS:
                        text = mainActivity.getString(R.string.balance)
                                + ": " + planJO.getString("funds");
                        break;
                    case CHECK_BALANCE:
                        text = Utility.readStringPreferences(mainActivity, Utility.KEY_CHECK_BALANCE_TEXT, "");

                        try {
                            int colorIndex = Integer.parseInt(Utility.readStringPreferences(mainActivity, Utility.KEY_CHECK_BALANCE_COLOR_INDEX, ""));
                            switch (colorIndex) {
                                case 0:
                                    color = R.color.green;
                                    break;
                                case 1:
                                    color = R.color.yellow;
                                    break;
                                case 2:
                                    color = R.color.red;
                                    break;
                                default:
                                    color = R.color.gray;
                                    break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            color = R.color.gray;
                        }

                        break;
                }

                if (!planJO.isNull("color")) {
                    switch (planJO.getString("color")) {
                        case "green":
                            color = R.color.green;
                            break;
                        case "yellow":
                            color = R.color.yellow;
                            break;
                        case "red":
                            color = R.color.red;
                            break;
                    }
                }

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
