package utils;

import android.util.Log;

import com.mingbikes.acquisition.Acquisition;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * http connection utils
 */
public class HttpConnectionUtils {

    private static final int CONNECT_TIMEOUT_IN_MILLISECONDS = 30000;
    private static final int READ_TIMEOUT_IN_MILLISECONDS = 30000;

    public static URLConnection urlConnectionForEventData(String serverURL, final String eventData) throws IOException {
        String urlStr = serverURL + "/developer_server/log";
        final URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_IN_MILLISECONDS);
        conn.setReadTimeout(READ_TIMEOUT_IN_MILLISECONDS);
        conn.setUseCaches(false);
        conn.setDoInput(true);

        if (Acquisition.sharedInstance().isLoggingEnabled()) {
            Log.d(Acquisition.TAG, "Using HTTP POST");
        }

        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(eventData);
        writer.flush();
        writer.close();
        os.close();

        return conn;
    }
}
