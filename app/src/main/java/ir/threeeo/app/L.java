package ir.threeeo.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/** App language (fa/en): auto-detected once from region/timezone, switchable by the user. */
final class L {
    private static String lang = null;
    private static Map<String, String> map;
    private static String[] frags;

    private L() {
    }

    static void init(Context c) {
        if (lang != null) {
            return;
        }
        SharedPreferences sp = c.getApplicationContext().getSharedPreferences("app", Context.MODE_PRIVATE);
        String v = sp.getString("lang", "");
        if (v.equals("fa") || v.equals("en")) {
            lang = v;
            return;
        }
        lang = detect();
    }

    /** Persian only when the device is in Iran (timezone/region) or set to Persian; everyone else gets English. */
    private static String detect() {
        try {
            String tz = TimeZone.getDefault().getID();
            if ("Asia/Tehran".equals(tz) || "Iran".equals(tz)) {
                return "fa";
            }
            Locale l = Locale.getDefault();
            if ("IR".equalsIgnoreCase(l.getCountry()) || "fa".equalsIgnoreCase(l.getLanguage())) {
                return "fa";
            }
        } catch (Exception ignored) {
        }
        return "en";
    }

    static void set(Context c, String code) {
        lang = code.equals("fa") ? "fa" : "en";
        c.getApplicationContext().getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("lang", lang).apply();
    }

    static boolean fa() {
        return !"en".equals(lang);
    }

    static String code() {
        return fa() ? "fa" : "en";
    }

    private static void load() {
        if (map != null) {
            return;
        }
        map = new HashMap<>();
        java.util.ArrayList<String> f = new java.util.ArrayList<>();
        for (int i = 0; i + 1 < EnDict.D.length; i += 2) {
            map.put(EnDict.D[i], EnDict.D[i + 1]);
            if (EnDict.D[i].trim().length() >= 6) {
                f.add(EnDict.D[i]);
            }
        }
        java.util.Collections.sort(f, (a, b) -> b.length() - a.length());
        frags = f.toArray(new String[0]);
    }

    /** translate a Persian source text when the app runs in English */
    static String t(String s) {
        if (fa() || s == null || s.isEmpty()) {
            return s;
        }
        load();
        String e = map.get(s);
        if (e != null) {
            return e;
        }
        String o = s;
        boolean hit = false;
        for (String k : frags) {
            if (o.contains(k)) {
                o = o.replace(k, map.get(k));
                hit = true;
            }
        }
        return hit ? digits(o) : s;
    }

    static String digits(String s) {
        StringBuilder b = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (ch >= '۰' && ch <= '۹') {
                b.append((char) ('0' + (ch - '۰')));
            } else if (ch == '٫') {
                b.append('.');
            } else if (ch == '٬' || ch == '،') {
                b.append(',');
            } else if (ch == '٪') {
                b.append('%');
            } else {
                b.append(ch);
            }
        }
        return b.toString();
    }

    /** Share text: Persian lines are marked right-to-left, English lines (host, links) left-to-right. */
    static String shareText(String title, String host, String url) {
        String head = (fa() ? "گزارش " : "Report ") + title;
        String rtl = fa() ? "\u200F" : "\u200E";
        return rtl + head + "\n\u200E" + host + "\n\u200E" + url;
    }
}
