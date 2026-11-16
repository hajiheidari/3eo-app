package ir.threeeo.app;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONArray;
import org.json.JSONObject;

/** Weekly AI brand-visibility monitors. */
public class MonitorActivity extends ScreenActivity {
    private JSONObject data;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        titleView.setText("پایش برند در هوش مصنوعی");
        load();
    }

    private void load() {
        Api.get(this, "/api/app/monitor", (j, e) -> {
            if (j == null || e != null) {
                content.removeAllViews();
                content.addView(UI.text(this, e == null ? "خطا در دریافت." : e, 15, UI.RED, false));
                return;
            }
            data = j;
            render();
        });
    }

    private void render() {
        content.removeAllViews();
        String cost = data.optBoolean("free") ? "رایگان" : UI.fa(String.valueOf(data.optInt("cost"))) + " اعتبار";
        content.addView(UI.text(this, "هر هفته می‌سنجیم هوش مصنوعی (ChatGPT و مشابه) برند شما را می‌شناسد و پیشنهاد می‌دهد یا نه. هر اجرا " + cost + " · اعتبار شما: " + UI.fa(String.valueOf(data.optInt("credits"))), 12, UI.MUTED, false));
        JSONArray ms = data.optJSONArray("monitors");
        for (int i = 0; ms != null && i < ms.length(); i++) {
            content.addView(card(ms.optJSONObject(i)), UI.wrap(this, 12));
        }
        if (ms == null || ms.length() < data.optInt("max")) {
            Button add = UI.primary(this, "＋ پایش جدید");
            add.setOnClickListener(v -> addDialog());
            content.addView(add, UI.wrap(this, 16));
        } else {
            content.addView(UI.text(this, "به سقف " + UI.fa(String.valueOf(data.optInt("max"))) + " پایش رسیده‌اید.", 12, UI.MUTED, false), UI.wrap(this, 12));
        }
    }

    private View card(final JSONObject m) {
        LinearLayout card = UI.card(this);
        LinearLayout head = new LinearLayout(this);
        UI.rtl(head);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(UI.text(this, UI.bidi(m.optString("brand")), 16, UI.INK, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        boolean paused = "paused".equals(m.optString("status"));
        head.addView(UI.text(this, paused ? "متوقف" : "فعال", 12, paused ? UI.AMBER : UI.GREEN, true));
        card.addView(head);
        card.addView(UI.text(this, UI.bidi(m.optString("topic")) + (m.optString("domain").isEmpty() ? "" : " · " + UI.bidi(m.optString("domain"))), 12, UI.MUTED, false), UI.wrap(this, 2));
        JSONArray runs = m.optJSONArray("runs");
        if (runs == null || runs.length() == 0) {
            card.addView(UI.text(this, "اولین اجرا تا چند دقیقه‌ی دیگر انجام می‌شود.", 12, UI.MUTED, false), UI.wrap(this, 8));
        } else {
            JSONObject last = runs.optJSONObject(0);
            LinearLayout row = new LinearLayout(this);
            UI.rtl(row);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int sc = last.optInt("score");
            row.addView(UI.text(this, UI.fa(String.valueOf(sc)), 30, UI.scoreColor(sc), true));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginStart(UI.dp(this, 10));
            row.addView(UI.text(this, last.optBoolean("known") ? "هوش مصنوعی برند را می‌شناسد" : "هوش مصنوعی برند را نمی‌شناسد", 13, UI.INK, true), lp);
            card.addView(row, UI.wrap(this, 8));
            StringBuilder sb = new StringBuilder("روند اجراها: ");
            for (int i = runs.length() - 1; i >= 0; i--) {
                sb.append(UI.fa(String.valueOf(runs.optJSONObject(i).optInt("score")))).append(i > 0 ? " ← " : "");
            }
            card.addView(UI.text(this, sb.toString(), 12, UI.MUTED, false), UI.wrap(this, 4));
        }
        LinearLayout acts = new LinearLayout(this);
        UI.rtl(acts);
        if (paused) {
            Button rs = UI.ghost(this, "ازسرگیری", UI.GREEN);
            rs.setOnClickListener(v -> act(m, "resume"));
            acts.addView(rs, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        Button del = UI.ghost(this, "حذف", UI.RED);
        del.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("حذف پایش").setMessage("پایش و تاریخچه‌اش حذف می‌شود.")
                .setNegativeButton("انصراف", null).setPositiveButton("حذف", (d, w) -> act(m, "delete")).show());
        acts.addView(del, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(acts, UI.wrap(this, 10));
        return card;
    }

    private void act(JSONObject m, String act) {
        JSONObject b = new JSONObject();
        try {
            b.put("act", act);
        } catch (Exception ignored) {
        }
        Api.post(this, "/api/app/monitor/" + m.optInt("id"), b, (j, e) -> load());
    }

    private void addDialog() {
        LinearLayout box = UI.column(this);
        int p = UI.dp(this, 16);
        box.setPadding(p, p, p, 0);
        final EditText brand = UI.input(this, "نام برند");
        final EditText domain = UI.input(this, "آدرس سایت (اختیاری)");
        domain.setTextDirection(View.TEXT_DIRECTION_LTR);
        final EditText topic = UI.input(this, "حوزه‌ی فعالیت (مثلاً طراحی سایت)");
        box.addView(brand);
        box.addView(domain, UI.wrap(this, 8));
        box.addView(topic, UI.wrap(this, 8));
        new AlertDialog.Builder(this).setTitle("پایش جدید").setView(box).setNegativeButton("انصراف", null)
                .setPositiveButton("شروع", (d, w) -> {
                    JSONObject b = new JSONObject();
                    try {
                        b.put("brand", brand.getText().toString());
                        b.put("domain", domain.getText().toString());
                        b.put("topic", topic.getText().toString());
                    } catch (Exception ignored) {
                    }
                    Api.post(this, "/api/app/monitor", b, (j, e) -> {
                        if (e != null) {
                            Toast.makeText(this, e, Toast.LENGTH_LONG).show();
                        }
                        load();
                    });
                }).show();
    }
}
