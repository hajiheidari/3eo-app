package ir.threeeo.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Talks to https://3eo.ir/api/app/* . Callbacks always run on the main thread. */
final class Api {
    static final String BASE = "https://3eo.ir";
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    interface Cb {
        /** json is null when there was no usable answer; err is a Persian message when something failed */
        void done(JSONObject json, String err);
    }

    private Api() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("3eo", Context.MODE_PRIVATE);
    }

    static String token(Context c) {
        return prefs(c).getString("token", null);
    }

    static void setToken(Context c, String t) {
        prefs(c).edit().putString("token", t).apply();
    }

    static void logout(Context c) {
        prefs(c).edit().clear().apply();
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
    }

    static void get(Context c, String path, Cb cb) {
        call(c, "GET", path, null, cb);
    }

    static void post(Context c, String path, JSONObject body, Cb cb) {
        call(c, "POST", path, body == null ? new JSONObject() : body, cb);
    }

    private static void call(Context c, String method, String path, JSONObject body, Cb cb) {
        final Context app = c.getApplicationContext();
        final String tok = token(app);
        POOL.execute(() -> {
            JSONObject out = null;
            String err = null;
            HttpURLConnection h = null;
            try {
                h = (HttpURLConnection) new URL(BASE + path).openConnection();
                h.setRequestMethod(method);
                h.setConnectTimeout(15000);
                h.setReadTimeout(75000);
                h.setRequestProperty("Accept", "application/json");
                h.setRequestProperty("User-Agent", "3eoApp/1.0 (Android)");
                if (tok != null) {
                    h.setRequestProperty("Authorization", "Bearer " + tok);
                }
                if (body != null) {
                    h.setDoOutput(true);
                    h.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    OutputStream os = h.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.close();
                }
                int code = h.getResponseCode();
                InputStream is = code >= 400 ? h.getErrorStream() : h.getInputStream();
                String text = is == null ? "" : read(is);
                try {
                    out = new JSONObject(text);
                } catch (Exception e) {
                    err = "پاسخ نامعتبر از سرور. دوباره امتحان کنید.";
                }
                if (out != null && !out.optBoolean("ok", false)) {
                    err = out.optString("error", "خطایی رخ داد.");
                }
            } catch (Exception e) {
                err = "اتصال به اینترنت برقرار نشد. دوباره امتحان کنید.";
            } finally {
                if (h != null) {
                    h.disconnect();
                }
            }
            final JSONObject fo = out;
            final String fe = err;
            MAIN.post(() -> {
                if (fo != null && "auth".equals(fo.optString("code"))) {
                    logout(app);
                }
                cb.done(fo, fe);
            });
        });
    }

    private static String read(InputStream is) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) {
            bo.write(buf, 0, n);
        }
        is.close();
        return bo.toString("UTF-8");
    }
}
