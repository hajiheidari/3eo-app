package ir.threeeo.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Draws the server's result "blocks" natively (score, counts, tables, lists, chips, copyable text, checks). */
final class Blocks {
    private Blocks() {
    }

    static void render(Context c, LinearLayout into, JSONArray blocks) {
        for (int i = 0; blocks != null && i < blocks.length(); i++) {
            JSONObject b = blocks.optJSONObject(i);
            View v = one(c, b);
            if (v != null) {
                into.addView(v, UI.wrap(c, 12));
            }
        }
    }

    private static View one(Context c, JSONObject b) {
        switch (b.optString("t")) {
            case "score": return score(c, b);
            case "counts": return counts(c, b);
            case "kv": return kv(c, b);
            case "list": return list(c, b);
            case "chips": return chips(c, b);
            case "copy": return copy(c, b);
            case "bars": return bars(c, b);
            case "checks": return checks(c, b);
            case "p": return UI.text(c, b.optString("text"), 13, b.optBoolean("muted") ? UI.MUTED : UI.INK, false);
            case "h": return UI.text(c, b.optString("text"), 16, UI.INK, true);
            default: return null;
        }
    }

    private static View title(Context c, String t) {
        return UI.text(c, t, 16, UI.INK, true);
    }

    private static View score(Context c, JSONObject b) {
        LinearLayout card = UI.card(c);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(new ScoreView(c, b.optInt("score")), new LinearLayout.LayoutParams(UI.dp(c, 180), UI.dp(c, 180)));
        card.addView(UI.text(c, b.optString("label"), 13, UI.MUTED, true));
        return card;
    }

    private static View counts(Context c, JSONObject b) {
        LinearLayout row = new LinearLayout(c);
        UI.rtl(row);
        row.setGravity(Gravity.CENTER);
        String[] k = {"pass", "warn", "fail"};
        String[] n = {L.t("موفق"), L.t("هشدار"), L.t("خطا")};
        int[] col = {UI.GREEN, UI.AMBER, UI.RED};
        for (int i = 0; i < 3; i++) {
            LinearLayout p = UI.column(c);
            p.setGravity(Gravity.CENTER);
            p.setBackground(UI.box(c, UI.CARD, 14, UI.LINE));
            p.setPadding(0, UI.dp(c, 10), 0, UI.dp(c, 10));
            TextView num = UI.text(c, UI.fa(String.valueOf(b.optInt(k[i]))), 22, col[i], true);
            num.setGravity(Gravity.CENTER);
            TextView lb = UI.text(c, n[i], 12, UI.MUTED, false);
            lb.setGravity(Gravity.CENTER);
            p.addView(num);
            p.addView(lb);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(UI.dp(c, 4), 0, UI.dp(c, 4), 0);
            row.addView(p, lp);
        }
        return row;
    }

    private static View kv(Context c, JSONObject b) {
        LinearLayout card = UI.card(c);
        JSONArray rows = b.optJSONArray("rows");
        for (int i = 0; rows != null && i < rows.length(); i++) {
            JSONArray r = rows.optJSONArray(i);
            LinearLayout row = new LinearLayout(c);
            UI.rtl(row);
            row.setPadding(0, UI.dp(c, i == 0 ? 0 : 9), 0, 0);
            row.addView(UI.text(c, r.optString(0), 13, UI.MUTED, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.9f));
            TextView v = UI.text(c, UI.fa(r.optString(1)), 14, UI.INK, true);
            row.addView(v, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.1f));
            card.addView(row);
            if (i < rows.length() - 1) {
                View l = new View(c);
                l.setBackgroundColor(UI.LINE);
                card.addView(l, UI.lp(c, ViewGroup.LayoutParams.MATCH_PARENT, 1, 9));
            }
        }
        return card;
    }

    private static int tone(String t) {
        return "ok".equals(t) ? UI.GREEN : ("warn".equals(t) ? UI.AMBER : ("bad".equals(t) ? UI.RED : UI.MUTED));
    }

    private static View list(Context c, JSONObject b) {
        LinearLayout box = UI.column(c);
        if (!b.optString("title").isEmpty()) {
            box.addView(title(c, b.optString("title")));
        }
        JSONArray items = b.optJSONArray("items");
        for (int i = 0; items != null && i < items.length(); i++) {
            JSONObject it = items.optJSONObject(i);
            LinearLayout row = new LinearLayout(c);
            UI.rtl(row);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(UI.box(c, UI.CARD, 14, UI.LINE));
            int p = UI.dp(c, 12);
            row.setPadding(p, p, p, p);
            LinearLayout txt = UI.column(c);
            TextView t = UI.text(c, it.optString("title"), 14, UI.INK, true);
            if (it.optBoolean("ltr")) {
                t.setTextDirection(View.TEXT_DIRECTION_LTR);
                t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            }
            txt.addView(t);
            String sub = it.optString("sub");
            if (!sub.isEmpty()) {
                txt.addView(UI.text(c, sub, 12, UI.MUTED, false), UI.wrap(c, 3));
            }
            row.addView(txt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            String tag = it.optString("tag");
            if (!tag.isEmpty()) {
                int col = tone(it.optString("tone"));
                TextView tg = UI.text(c, UI.fa(tag), 12, col, true);
                tg.setBackground(UI.box(c, (col & 0x00FFFFFF) | 0x1A000000, 10, 0));
                tg.setPadding(UI.dp(c, 8), UI.dp(c, 3), UI.dp(c, 8), UI.dp(c, 3));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMarginStart(UI.dp(c, 8));
                row.addView(tg, lp);
            }
            box.addView(row, UI.wrap(c, 8));
        }
        return box;
    }

    private static View chips(final Context c, JSONObject b) {
        LinearLayout box = UI.column(c);
        if (!b.optString("title").isEmpty()) {
            box.addView(title(c, b.optString("title")));
        }
        LinearLayout card = UI.card(c);
        JSONArray items = b.optJSONArray("items");
        LinearLayout row = null;
        int used = 0;
        int max = c.getResources().getDisplayMetrics().widthPixels - UI.dp(c, 32 + 32);
        for (int i = 0; items != null && i < items.length(); i++) {
            TextView t = UI.text(c, items.optString(i), 13, UI.GREEN_DARK, false);
            t.setBackground(UI.box(c, UI.GREEN_SOFT, 16, 0));
            t.setPadding(UI.dp(c, 10), UI.dp(c, 5), UI.dp(c, 10), UI.dp(c, 5));
            t.setMaxWidth(max);
            t.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int w = t.getMeasuredWidth() + UI.dp(c, 6);
            if (row == null || used + w > max) {
                row = new LinearLayout(c);
                UI.rtl(row);
                card.addView(row, UI.wrap(c, row == null ? 0 : 6));
                used = 0;
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(UI.dp(c, 6));
            row.addView(t, lp);
            used += w;
        }
        box.addView(card, UI.wrap(c, 8));
        return box;
    }

    private static View copy(final Context c, JSONObject b) {
        final String text = b.optString("text");
        LinearLayout card = UI.card(c);
        card.addView(UI.text(c, b.optString("title"), 14, UI.INK, true));
        TextView t = UI.text(c, text, 13, UI.INK, false);
        t.setTextIsSelectable(true);
        if (b.optBoolean("ltr")) {
            t.setTextDirection(View.TEXT_DIRECTION_LTR);
        }
        t.setBackground(UI.box(c, 0xFFF8FAFC, 10, UI.LINE));
        int p = UI.dp(c, 10);
        t.setPadding(p, p, p, p);
        card.addView(t, UI.wrap(c, 8));
        Button cp = UI.ghost(c, L.t("کپی"), UI.GREEN);
        cp.setOnClickListener(v -> {
            ((ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("3eo", text));
            Toast.makeText(c, L.t("کپی شد ✓"), Toast.LENGTH_SHORT).show();
        });
        card.addView(cp, UI.wrap(c, 8));
        return card;
    }

    private static View bars(Context c, JSONObject b) {
        LinearLayout box = UI.column(c);
        if (!b.optString("title").isEmpty()) {
            box.addView(title(c, b.optString("title")));
        }
        LinearLayout card = UI.card(c);
        JSONArray items = b.optJSONArray("items");
        for (int i = 0; items != null && i < items.length(); i++) {
            JSONObject it = items.optJSONObject(i);
            LinearLayout head = new LinearLayout(c);
            UI.rtl(head);
            head.addView(UI.text(c, it.optString("label"), 13, UI.INK, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            head.addView(UI.text(c, UI.fa(it.optString("text")), 13, UI.MUTED, true));
            card.addView(head, UI.wrap(c, i == 0 ? 0 : 10));
            double max = Math.max(1, it.optDouble("max", 100));
            float frac = (float) Math.min(1, it.optDouble("value") / max);
            LinearLayout track = new LinearLayout(c);
            UI.rtl(track);
            track.setBackground(UI.box(c, UI.LINE, 4, 0));
            View fill = new View(c);
            int col = it.optDouble("max", 100) == 100 ? UI.scoreColor((int) it.optDouble("value")) : UI.GREEN;
            fill.setBackground(UI.box(c, col, 4, 0));
            track.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, Math.max(frac, 0.02f)));
            track.addView(new View(c), new LinearLayout.LayoutParams(0, 1, 1f - Math.max(frac, 0.02f)));
            card.addView(track, UI.lp(c, ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(c, 8), 4));
        }
        box.addView(card, UI.wrap(c, 8));
        return box;
    }

    private static View checks(Context c, JSONObject b) {
        LinearLayout box = UI.column(c);
        JSONArray cats = b.optJSONArray("categories");
        List<JSONObject> issues = new ArrayList<>();
        for (int i = 0; cats != null && i < cats.length(); i++) {
            JSONArray cs = cats.optJSONObject(i).optJSONArray("checks");
            for (int k = 0; cs != null && k < cs.length(); k++) {
                JSONObject ck = cs.optJSONObject(k);
                if (!"pass".equals(ck.optString("status"))) {
                    issues.add(ck);
                }
            }
        }
        Collections.sort(issues, (x, y) -> Double.compare(y.optDouble("gain"), x.optDouble("gain")));
        if (!issues.isEmpty()) {
            box.addView(title(c, L.t("مهم‌ترین کارها برای بهتر شدن")));
            int n = 0;
            for (JSONObject it : issues) {
                if (n++ >= 8) {
                    break;
                }
                box.addView(checkCard(c, it), UI.wrap(c, 8));
            }
        }
        if (cats != null && cats.length() > 0) {
            box.addView(title(c, L.t("همه‌ی بخش‌ها")), UI.wrap(c, 18));
            for (int i = 0; i < cats.length(); i++) {
                box.addView(categoryCard(c, cats.optJSONObject(i)), UI.wrap(c, 8));
            }
        }
        return box;
    }

    private static View checkCard(Context c, JSONObject ck) {
        LinearLayout card = UI.card(c);
        String st = ck.optString("status");
        card.addView(UI.text(c, ("warn".equals(st) ? "⚠  " : "✕  ") + ck.optString("title"), 15, UI.statusColor(st), true));
        String detail = ck.optString("detail");
        if (!detail.isEmpty()) {
            card.addView(UI.text(c, detail, 13, UI.MUTED, false), UI.wrap(c, 4));
        }
        String fix = ck.optString("fix");
        if (!fix.isEmpty()) {
            LinearLayout f = UI.column(c);
            f.setBackground(UI.box(c, UI.GREEN_SOFT, 10, 0));
            int p = UI.dp(c, 10);
            f.setPadding(p, p, p, p);
            f.addView(UI.text(c, L.t("راه‌حل: ") + fix, 13, UI.GREEN_DARK, false));
            card.addView(f, UI.wrap(c, 8));
        }
        return card;
    }

    private static View categoryCard(Context c, JSONObject cat) {
        final LinearLayout card = UI.card(c);
        LinearLayout head = new LinearLayout(c);
        UI.rtl(head);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(UI.text(c, cat.optString("title"), 15, UI.INK, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        int sc = cat.optInt("score");
        head.addView(UI.text(c, UI.fa(String.valueOf(sc)) + L.t("٪  ▾"), 15, UI.scoreColor(sc), true));
        card.addView(head);
        final LinearLayout list = UI.column(c);
        list.setVisibility(View.GONE);
        JSONArray cs = cat.optJSONArray("checks");
        for (int i = 0; cs != null && i < cs.length(); i++) {
            JSONObject ck = cs.optJSONObject(i);
            String st = ck.optString("status");
            String mark = "pass".equals(st) ? "✓  " : ("warn".equals(st) ? "⚠  " : "✕  ");
            list.addView(UI.text(c, mark + ck.optString("title"), 14, UI.statusColor(st), false), UI.wrap(c, 10));
            String d = ck.optString("detail");
            if (!d.isEmpty()) {
                list.addView(UI.text(c, d, 12, UI.MUTED, false), UI.wrap(c, 2));
            }
        }
        card.addView(list);
        card.setOnClickListener(v -> list.setVisibility(list.getVisibility() == View.GONE ? View.VISIBLE : View.GONE));
        return card;
    }
}
