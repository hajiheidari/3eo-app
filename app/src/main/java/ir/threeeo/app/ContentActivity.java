package ir.threeeo.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

/** AI content: find topics or start from a topic; recent jobs. */
public class ContentActivity extends ScreenActivity {
    private JSONObject data;
    private String seed0 = "", topic0 = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        titleView.setText(L.t("تولید محتوا"));
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (data != null) {
            load();
        }
    }

    private void load() {
        Api.get(this, "/api/app/content", (j, e) -> {
            if (j == null || e != null) {
                content.removeAllViews();
                content.addView(UI.text(this, e == null ? L.t("خطا در دریافت.") : e, 15, UI.RED, false));
                return;
            }
            data = j;
            render();
        });
    }

    private String cost(String k) {
        return data.optBoolean("free") ? L.t("رایگان") : UI.fa(String.valueOf(data.optJSONObject("cost").optInt(k))) + L.t(" اعتبار");
    }

    private void render() {
        content.removeAllViews();
        content.addView(UI.text(this, L.t("از پیدا کردن موضوع تا مقاله‌ی کامل فارسی و سئوشده. اعتبار شما: ") + UI.fa(String.valueOf(data.optInt("credits"))), 12, UI.MUTED, false));

        LinearLayout a = UI.card(this);
        a.addView(UI.text(this, L.t("۱) پیشنهاد موضوع"), 15, UI.INK, true));
        final EditText seed = UI.input(this, L.t("حوزه یا کلمه‌ی کلیدی (مثلاً سئو سایت)"));
        final EditText aud = UI.input(this, L.t("مخاطب (اختیاری)"));
        a.addView(seed, UI.wrap(this, 8));
        a.addView(aud, UI.wrap(this, 8));
        Button go = UI.primary(this, L.t("پیشنهاد ۱۰ موضوع · ") + cost("topics"));
        go.setOnClickListener(v -> {
            JSONObject b = new JSONObject();
            try {
                b.put("seed", seed.getText().toString());
                b.put("audience", aud.getText().toString());
            } catch (Exception ignored) {
            }
            start("/api/app/content/topics", b);
        });
        a.addView(go, UI.wrap(this, 10));
        content.addView(a, UI.wrap(this, 12));

        LinearLayout c = UI.card(this);
        c.addView(UI.text(this, L.t("یا موضوع خودتان را دارید؟"), 15, UI.INK, true));
        final EditText topic = UI.input(this, L.t("موضوع مقاله"));
        final EditText kw = UI.input(this, L.t("کلمه‌ی کلیدی اصلی"));
        c.addView(topic, UI.wrap(this, 8));
        c.addView(kw, UI.wrap(this, 8));
        Button o = UI.ghost(this, L.t("ساخت ساختار مقاله · ") + cost("outline"), UI.GREEN);
        o.setOnClickListener(v -> openOutlineForm(topic.getText().toString(), kw.getText().toString()));
        c.addView(o, UI.wrap(this, 10));
        content.addView(c, UI.wrap(this, 12));

        content.addView(UI.text(this, L.t("کارهای اخیر"), 17, UI.INK, true), UI.wrap(this, 20));
        JSONArray rs = data.optJSONArray("recent");
        if (rs == null || rs.length() == 0) {
            content.addView(UI.text(this, L.t("هنوز کاری ندارید."), 13, UI.MUTED, false), UI.wrap(this, 6));
        }
        for (int i = 0; rs != null && i < rs.length(); i++) {
            final JSONObject r = rs.optJSONObject(i);
            LinearLayout row = new LinearLayout(this);
            UI.rtl(row);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(UI.box(this, UI.CARD, 14, UI.LINE));
            int p = UI.dp(this, 12);
            row.setPadding(p, p, p, p);
            LinearLayout info = UI.column(this);
            info.addView(UI.text(this, UI.bidi(r.optString("title")), 14, UI.INK, true));
            info.addView(UI.text(this, r.optString("name"), 11, UI.MUTED, false));
            row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            String st = r.optString("status");
            row.addView(UI.text(this, "done".equals(st) ? L.t("آماده") : ("failed".equals(st) ? L.t("ناموفق") : L.t("در حال انجام…")), 12, "done".equals(st) ? UI.GREEN : ("failed".equals(st) ? UI.RED : UI.AMBER), true));
            row.setOnClickListener(v -> openJob(r.optInt("id")));
            content.addView(row, UI.wrap(this, 8));
        }
    }

    void openOutlineForm(String topic, String kw) {
        Intent i = new Intent(this, ContentJobActivity.class);
        i.putExtra("form", true);
        i.putExtra("topic", topic);
        i.putExtra("keyword", kw);
        i.putExtra("data", data.toString());
        startActivity(i);
    }

    private void openJob(int id) {
        Intent i = new Intent(this, ContentJobActivity.class);
        i.putExtra("id", id);
        startActivity(i);
    }

    private void start(String path, JSONObject body) {
        Api.post(this, path, body, (j, e) -> {
            if (j == null || e != null) {
                Toast.makeText(this, e == null ? L.t("خطایی رخ داد.") : e, Toast.LENGTH_LONG).show();
                return;
            }
            openJob(j.optInt("id"));
        });
    }
}
