package ir.threeeo.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The WordPress blog read natively through its public REST API (no web page is shown). */
final class Blog {
    interface Cb {
        void done(JSONArray posts, String err);
    }

    static final String BASE = "https://3eo.ir/blog";
    private static final String QS = "per_page=12&_embed=wp:featuredmedia&_fields=id,date,title,excerpt,content,link,_embedded";
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final android.util.LruCache<String, Bitmap> CACHE = new android.util.LruCache<>(24);
    static JSONObject current;

    private static byte[] fetch(String u) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(u).openConnection();
        h.setConnectTimeout(15000);
        h.setReadTimeout(30000);
        h.setRequestProperty("User-Agent", "3eoApp/1.0 (Android)");
        h.setRequestProperty("Accept", "application/json,image/*");
        try {
            if (h.getResponseCode() >= 400) {
                throw new Exception("http");
            }
            InputStream is = h.getInputStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = is.read(b)) > 0) {
                bo.write(b, 0, n);
            }
            return bo.toByteArray();
        } finally {
            h.disconnect();
        }
    }

    static void posts(final int page, final Cb cb) {
        POOL.execute(() -> {
            JSONArray out = null;
            String err = null;
            String[] urls = {BASE + "/wp-json/wp/v2/posts?" + QS + "&page=" + page, BASE + "/?rest_route=/wp/v2/posts&" + QS + "&page=" + page};
            for (String u : urls) {
                try {
                    out = new JSONArray(new String(fetch(u), "UTF-8"));
                    break;
                } catch (Exception e) {
                    err = L.t("دریافت مقاله‌ها ممکن نشد. اتصال اینترنت را بررسی کنید.");
                }
            }
            final JSONArray fo = out;
            final String fe = out == null ? err : null;
            MAIN.post(() -> cb.done(fo, fe));
        });
    }

    static void image(final ImageView v, final String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        Bitmap c = CACHE.get(url);
        if (c != null) {
            v.setImageBitmap(c);
            return;
        }
        POOL.execute(() -> {
            try {
                byte[] d = fetch(url);
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inSampleSize = 2;
                final Bitmap bm = BitmapFactory.decodeByteArray(d, 0, d.length, o);
                if (bm != null) {
                    CACHE.put(url, bm);
                    MAIN.post(() -> v.setImageBitmap(bm));
                }
            } catch (Exception ignored) {
            }
        });
    }

    static String thumb(JSONObject p) {
        try {
            return p.getJSONObject("_embedded").getJSONArray("wp:featuredmedia").getJSONObject(0).getString("source_url");
        } catch (Exception e) {
            return "";
        }
    }

    static String text(String html) {
        String s = html.replaceAll("(?s)<[^>]+>", "");
        s = s.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#8217;", "'").replace("&hellip;", "…");
        Matcher m = Pattern.compile("&#(\\d+);").matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(new String(Character.toChars(Integer.parseInt(m.group(1))))));
        }
        m.appendTail(sb);
        return sb.toString().replaceAll("[ \\t]+", " ").trim();
    }

    /** Each block: {kind: h|p|li|img, text}. */
    static List<String[]> blocks(String html) {
        List<String[]> out = new ArrayList<>();
        Matcher m = Pattern.compile("<(h[1-6]|p|li|blockquote)\\b[^>]*>(.*?)</\\1>|<img\\b[^>]*?src=\"([^\"]+)\"[^>]*>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(html);
        while (m.find()) {
            if (m.group(3) != null) {
                out.add(new String[]{"img", m.group(3)});
                continue;
            }
            String tag = m.group(1).toLowerCase();
            String t = text(m.group(2));
            if (t.isEmpty()) {
                continue;
            }
            out.add(new String[]{tag.startsWith("h") ? "h" : ("li".equals(tag) ? "li" : "p"), t});
        }
        return out;
    }
}
