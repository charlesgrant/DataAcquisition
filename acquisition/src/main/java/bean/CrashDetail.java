package bean;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.List;

import utils.DeviceInfo;

/**
 * crash detail bean
 */

public class CrashDetail {

    private String model;
    private String os;
    private String os_ver;
    private String app_id;
    private String app_ver;
    private String package_name;
    private String sdk_ver;
    private String language;
    private String device_id;
    private String userId;
    private String type;

    private List<Operation> operations;

    public String getCrashData(Context context) {

        final JSONObject json = new JSONObject();

        fillJSONIfValuesNotEmpty(json,
                "_device", DeviceInfo.getDevice(),
                "_os", DeviceInfo.getOS(),
                "_os_version", DeviceInfo.getOSVersion(),
                "_app_version", DeviceInfo.getAppVersion(context)
        );

//        try {
//            json.put("operations", new JSONObject(operations));
//        } catch (JSONException e) {
//
//        }

        String result = json.toString();

        try {
            result = java.net.URLEncoder.encode(result, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            // should never happen because Android guarantees UTF-8 support
        }

        return result;
    }

    /**
     * method to fill JSONObject with supplied objects for supplied keys.
     * Fills json key/value pairs.
     */
    void fillJSONIfValuesNotEmpty(final JSONObject json, final String ... objects) {
        try {
            if (objects.length > 0 && objects.length % 2 == 0) {
                for (int i = 0; i < objects.length; i += 2) {
                    final String key = objects[i];
                    final String value = objects[i + 1];
                    if (value != null && value.length() > 0) {
                        json.put(key, value);
                    }
                }
            }
        } catch (JSONException ignored) {
            // shouldn't ever happen when putting String objects into a JSONObject,
            // it can only happen when putting NaN or INFINITE doubles or floats into it
        }
    }
}
