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
    private static final String QS = "per_page=12&_embed=wp:featuredmedia";
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

    /** Safe image URL: https, Persian/space characters percent-encoded. */
    static String fixUrl(String u) {
        if (u == null) {
            return "";
        }
        u = u.trim().replace("&amp;", "&");
        if (u.startsWith("//")) {
            u = "https:" + u;
        } else if (u.startsWith("/")) {
            u = "https://3eo.ir" + u;
        }
        if (u.startsWith("http://") && u.contains("3eo.ir")) {
            u = "https://" + u.substring(7);
        }
        StringBuilder sb = new StringBuilder();
        for (char c : u.toCharArray()) {
            if (c > 127 || c == ' ') {
                try {
                    sb.append(java.net.URLEncoder.encode(String.valueOf(c), "UTF-8").replace("+", "%20"));
                } catch (Exception e) {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    static String thumb(JSONObject p) {
        try {
            JSONObject m = p.getJSONObject("_embedded").getJSONArray("wp:featuredmedia").getJSONObject(0);
            JSONObject sz = m.optJSONObject("media_details") == null ? null : m.optJSONObject("media_details").optJSONObject("sizes");
            for (String k : new String[]{"medium_large", "large", "medium", "full"}) {
                if (sz != null && sz.optJSONObject(k) != null && !sz.optJSONObject(k).optString("source_url").isEmpty()) {
                    return fixUrl(sz.optJSONObject(k).optString("source_url"));
                }
            }
            if (!m.optString("source_url").isEmpty()) {
                return fixUrl(m.optString("source_url"));
            }
        } catch (Exception ignored) {
        }
        // no featured image: first picture inside the article
        try {
            String html = p.optJSONObject("content") == null ? "" : p.optJSONObject("content").optString("rendered");
            Matcher m = Pattern.compile("<img\\b[^>]*?(?:data-src|src)=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
            while (m.find()) {
                String u = m.group(1);
                if (!u.startsWith("data:")) {
                    return fixUrl(u);
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    /** Summary: the excerpt, or the first paragraph of the article when the excerpt is empty. */
    static String summary(JSONObject p) {
        String ex = text(p.optJSONObject("excerpt") == null ? "" : p.optJSONObject("excerpt").optString("rendered"));
        if (!ex.isEmpty()) {
            return ex;
        }
        for (String[] b : blocks(p.optJSONObject("content") == null ? "" : p.optJSONObject("content").optString("rendered"))) {
            if ("p".equals(b[0]) && b[1].length() > 20) {
                return b[1];
            }
        }
        return "";
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
                out.add(new String[]{"img", fixUrl(m.group(3))});
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
