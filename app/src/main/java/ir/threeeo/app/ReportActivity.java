package ir.threeeo.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Native view of one site-analysis report: score, counts, biggest problems first, then every category. */
public class ReportActivity extends AppCompatActivity {
    private LinearLayout content;
    private String shareUrl = "";
    private String host = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = UI.column(this);
        root.setBackgroundColor(UI.BG);

        LinearLayout bar = new LinearLayout(this);
        UI.rtl(bar);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(UI.CARD);
        int p = UI.dp(this, 14);
        bar.setPadding(p, UI.dp(this, 10), p, UI.dp(this, 10));
        TextView back = UI.text(this, "‹  بازگشت", 15, UI.GREEN, true);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView share = UI.text(this, "اشتراک‌گذاری", 15, UI.GREEN, true);
        share.setOnClickListener(v -> {
            if (shareUrl.isEmpty()) {
                return;
            }
            Intent s = new Intent(Intent.ACTION_SEND);
            s.setType("text/plain");
            s.putExtra(Intent.EXTRA_TEXT, "گزارش سئوی " + host + "\n" + shareUrl);
            startActivity(Intent.createChooser(s, "اشتراک‌گذاری"));
        });
        bar.addView(share);
        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView sv = new ScrollView(this);
        content = UI.column(this);
        content.setPadding(p, p, p, UI.dp(this, 32));
        sv.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        String json = getIntent().getStringExtra("json");
        if (json != null) {
            try {
                render(new JSONObject(json));
                return;
            } catch (Exception ignored) {
            }
        }
        String token = getIntent().getStringExtra("token");
        content.addView(UI.text(this, "در حال دریافت گزارش…", 15, UI.MUTED, false));
        Api.get(this, "/api/app/report/" + token, (j, e) -> {
            content.removeAllViews();
            if (e != null || j == null) {
                content.addView(UI.text(this, e == null ? "گزارش پیدا نشد." : e, 15, UI.RED, false));
            } else {
                render(j);
            }
        });
    }

    private void render(JSONObject j) {
        content.removeAllViews();
        host = j.optString("host");
        shareUrl = j.optString("share");
        int score = j.optInt("score");
        JSONObject counts = j.optJSONObject("counts");

        LinearLayout hero = UI.card(this);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.addView(UI.text(this, host, 17, UI.INK, true));
        ScoreView sc = new ScoreView(this, score);
        hero.addView(sc, new LinearLayout.LayoutParams(UI.dp(this, 190), UI.dp(this, 190)));
        LinearLayout pills = new LinearLayout(this);
        UI.rtl(pills);
        pills.setGravity(Gravity.CENTER);
        pills.addView(pill("موفق", counts == null ? 0 : counts.optInt("pass"), UI.GREEN));
        pills.addView(pill("هشدار", counts == null ? 0 : counts.optInt("warn"), UI.AMBER));
        pills.addView(pill("خطا", counts == null ? 0 : counts.optInt("fail"), UI.RED));
        hero.addView(pills, UI.wrap(this, 8));
        content.addView(hero, UI.wrap(this, 0));

        JSONArray cats = j.optJSONArray("categories");
        // the biggest problems first
        List<JSONObject[]> issues = new ArrayList<>();
        if (cats != null) {
            for (int i = 0; i < cats.length(); i++) {
                JSONArray cs = cats.optJSONObject(i).optJSONArray("checks");
                for (int k = 0; cs != null && k < cs.length(); k++) {
                    JSONObject c = cs.optJSONObject(k);
                    if (!"pass".equals(c.optString("status"))) {
                        issues.add(new JSONObject[]{c});
                    }
                }
            }
        }
        Collections.sort(issues, (a, c) -> Double.compare(c[0].optDouble("gain"), a[0].optDouble("gain")));
        if (!issues.isEmpty()) {
            content.addView(UI.text(this, "مهم‌ترین کارها برای بهتر شدن امتیاز", 16, UI.INK, true), UI.wrap(this, 20));
            int n = 0;
            for (JSONObject[] it : issues) {
                if (n++ >= 8) {
                    break;
                }
                content.addView(checkCard(it[0]), UI.wrap(this, 8));
            }
        }
        if (cats != null) {
            content.addView(UI.text(this, "همه‌ی بخش‌ها", 16, UI.INK, true), UI.wrap(this, 22));
            for (int i = 0; i < cats.length(); i++) {
                content.addView(categoryCard(cats.optJSONObject(i)), UI.wrap(this, 8));
            }
        }
        Button web = UI.ghost(this, "گزارش کامل در وب", UI.GREEN);
        web.setOnClickListener(v -> {
            Intent i = new Intent(this, WebActivity.class);
            i.putExtra("url", shareUrl);
            startActivity(i);
        });
        content.addView(web, UI.wrap(this, 20));
    }

    private View pill(String label, int n, int color) {
        TextView t = UI.text(this, label + ": " + UI.fa(String.valueOf(n)), 13, color, true);
        t.setBackground(UI.box(this, 0x00000000, 20, color));
        int h = UI.dp(this, 12), v = UI.dp(this, 5);
        t.setPadding(h, v, h, v);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(UI.dp(this, 4), 0, UI.dp(this, 4), 0);
        t.setLayoutParams(lp);
        return t;
    }

    private View checkCard(JSONObject c) {
        LinearLayout card = UI.card(this);
        String st = c.optString("status");
        TextView title = UI.text(this, ("warn".equals(st) ? "⚠  " : "✕  ") + c.optString("title"), 15, UI.statusColor(st), true);
        card.addView(title);
        String detail = c.optString("detail");
        if (!detail.isEmpty()) {
            card.addView(UI.text(this, detail, 13, UI.MUTED, false), UI.wrap(this, 4));
        }
        String fix = c.optString("fix");
        if (!fix.isEmpty()) {
            LinearLayout f = UI.column(this);
            f.setBackground(UI.box(this, UI.GREEN_SOFT, 10, 0));
            int p = UI.dp(this, 10);
            f.setPadding(p, p, p, p);
            f.addView(UI.text(this, "راه‌حل: " + fix, 13, UI.GREEN_DARK, false));
            card.addView(f, UI.wrap(this, 8));
        }
        return card;
    }

    private View categoryCard(JSONObject cat) {
        final LinearLayout card = UI.card(this);
        LinearLayout head = new LinearLayout(this);
        UI.rtl(head);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = UI.text(this, cat.optString("title"), 15, UI.INK, true);
        head.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        int sc = cat.optInt("score");
        head.addView(UI.text(this, UI.fa(String.valueOf(sc)) + "٪  ▾", 15, UI.scoreColor(sc), true));
        card.addView(head);
        final LinearLayout list = UI.column(this);
        list.setVisibility(View.GONE);
        JSONArray cs = cat.optJSONArray("checks");
        for (int i = 0; cs != null && i < cs.length(); i++) {
            JSONObject c = cs.optJSONObject(i);
            String st = c.optString("status");
            String mark = "pass".equals(st) ? "✓  " : ("warn".equals(st) ? "⚠  " : "✕  ");
            list.addView(UI.text(this, mark + c.optString("title"), 14, UI.statusColor(st), false), UI.wrap(this, 10));
            String d = c.optString("detail");
            if (!d.isEmpty()) {
                list.addView(UI.text(this, d, 12, UI.MUTED, false), UI.wrap(this, 2));
            }
        }
        card.addView(list);
        card.setOnClickListener(v -> list.setVisibility(list.getVisibility() == View.GONE ? View.VISIBLE : View.GONE));
        return card;
    }
}
