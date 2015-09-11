package interactive1.com.smsgateway.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.List;

/**
 * Created by dtomic on 07/07/15.
 */
public class Utility {

    private static final String TAG = "Utility";

    //"http://jjurjevic.interactive1.hr:4567/";
    //"http://smsgw-api.interactive1.hr:80/";

    public static final String URL_BASE = "http://smsgw-api.interactive1.hr:80/";
    public static final String URL_ASSOCIATE = "phone/assoc";
    public static final String URL_MESSAGES = "messages";
    public static final String URL_PHONE = "phone";

    public static final String KEY_ACCOUNT_TOKEN = "key_accountToken";

    public static final String KEY_DEVICE_RESTARTED = "key_deviceRestarted";
    public static final String KEY_CLOSE_DIALOG_FLAG = "key_closeDialogFlag";


    public static final String KEY_USSD_CODE = "key_ussdCode";

    public static final String KEY_SMS_NUMBER = "key_sms_number";
    public static final String KEY_SMS_TEXT = "key_sms_text";


    public static void saveStringPreferences(Context context, String key, String value){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static String readStringPreferences(Context context, String key, String defaultValue){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        String res = sp.getString(key, defaultValue);
        if(res==null)res="";
        return res;
    }

    public static void saveBooleanPreferences(Context context, String key, boolean value){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static boolean readBooleanPreferences(Context context, String key, boolean defaultValue){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return sp.getBoolean(key, defaultValue);
    }

    public static <E> List<E> copyListToList(List<E> input, List<E> output) {
        if (input != null && output != null) {
            output.clear();
            for (E o : input) {
                output.add(o);
            }
        }

        return output;
    }
}
