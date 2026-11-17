package ir.threeeo.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** One content job: outline form, polling, topic picker, editable outline, finished article. */
public class ContentJobActivity extends ScreenActivity {
    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean alive = true;
    private int id;
    private JSONObject cfg;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        titleView.setText(L.t("تولید محتوا"));
        id = getIntent().getIntExtra("id", 0);
        if (getIntent().getBooleanExtra("form", false)) {
            try {
                cfg = new JSONObject(getIntent().getStringExtra("data"));
            } catch (Exception e) {
                cfg = new JSONObject();
            }
            form(getIntent().getStringExtra("topic"), getIntent().getStringExtra("keyword"));
        } else {
            poll(0);
        }
    }

    @Override
    protected void onDestroy() {
        alive = false;
        super.onDestroy();
    }

    private String fa(int n) {
        return UI.fa(String.valueOf(n));
    }

    /** Outline settings form (topic, keyword, audience, tone, length, notes). */
    private void form(String topic, String kw) {
        content.removeAllViews();
        titleView.setText(L.t("ساختار مقاله"));
        final EditText t = UI.input(this, L.t("موضوع مقاله"));
        t.setText(topic == null ? "" : topic);
        final EditText k = UI.input(this, L.t("کلمه‌ی کلیدی اصلی"));
        k.setText(kw == null ? "" : kw);
        final EditText aud = UI.input(this, L.t("مخاطب (اختیاری)"));
        final EditText notes = UI.input(this, L.t("نکات و واقعیت‌های معتبر شما (اختیاری)"));
        notes.setSingleLine(false);
        notes.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        notes.setMinLines(3);
        notes.setGravity(Gravity.TOP | Gravity.START);
        content.addView(t);
        content.addView(k, UI.wrap(this, 8));
        content.addView(aud, UI.wrap(this, 8));
        final List<String> tv = new ArrayList<>(), tn = new ArrayList<>();
        JSONArray tones = cfg.optJSONArray("tones");
        for (int i = 0; tones != null && i < tones.length(); i++) {
            tv.add(tones.optJSONArray(i).optString(0));
            tn.add(tones.optJSONArray(i).optString(1));
        }
        final Spinner ts = new Spinner(this);
        ts.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, tn));
        ts.setBackground(UI.box(this, 0xFFF8FAFC, 12, UI.LINE));
        content.addView(UI.text(this, L.t("لحن نوشته"), 12, UI.MUTED, false), UI.wrap(this, 10));
        content.addView(ts, UI.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(this, 46), 4));
        final List<Integer> lv = new ArrayList<>();
        final List<String> ln = new ArrayList<>();
        JSONArray ls = cfg.optJSONArray("lengths");
        for (int i = 0; ls != null && i < ls.length(); i++) {
            lv.add(ls.optInt(i));
            ln.add(L.t("حدود ") + fa(ls.optInt(i)) + L.t(" کلمه"));
        }
        final Spinner lsp = new Spinner(this);
        lsp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ln));
        lsp.setBackground(UI.box(this, 0xFFF8FAFC, 12, UI.LINE));
        if (lv.size() > 1) {
            lsp.setSelection(1);
        }
        content.addView(UI.text(this, L.t("طول مقاله"), 12, UI.MUTED, false), UI.wrap(this, 10));
        content.addView(lsp, UI.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(this, 46), 4));
        content.addView(notes, UI.wrap(this, 10));
        String c = cfg.optBoolean("free") ? L.t("رایگان") : fa(cfg.optJSONObject("cost").optInt("outline")) + L.t(" اعتبار");
        Button go = UI.primary(this, L.t("ساخت ساختار · ") + c);
        go.setOnClickListener(v -> {
            JSONObject b = new JSONObject();
            try {
                b.put("topic", t.getText().toString());
                b.put("keyword", k.getText().toString());
                b.put("audience", aud.getText().toString());
                b.put("notes", notes.getText().toString());
                b.put("tone", tv.isEmpty() ? "friendly" : tv.get(ts.getSelectedItemPosition()));
                b.put("words", lv.isEmpty() ? 900 : lv.get(lsp.getSelectedItemPosition()));
            } catch (Exception ignored) {
            }
            go.setEnabled(false);
            Api.post(this, "/api/app/content/outline", b, (j, e) -> {
                go.setEnabled(true);
                if (j == null || e != null) {
                    Toast.makeText(this, e == null ? L.t("خطایی رخ داد.") : e, Toast.LENGTH_LONG).show();
                    return;
                }
                id = j.optInt("id");
                titleView.setText(L.t("تولید محتوا"));
                poll(0);
            });
        });
        content.addView(go, UI.wrap(this, 14));
    }

    private void poll(final int n) {
        Api.get(this, "/api/app/content/" + id, (j, e) -> {
            if (!alive) {
                return;
            }
            if (j == null || e != null) {
                if (n < 3) {
                    h.postDelayed(() -> poll(n + 1), 4000);
                } else {
                    content.removeAllViews();
                    content.addView(UI.text(this, e == null ? L.t("خطا در دریافت.") : e, 15, UI.RED, false));
                }
                return;
            }
            String st = j.optString("status");
            if ("done".equals(st)) {
                show(j);
            } else if ("failed".equals(st)) {
                content.removeAllViews();
                content.addView(UI.text(this, L.t("انجام نشد؛ اعتبار برگشت داده شد."), 15, UI.RED, true));
                content.addView(UI.text(this, j.optString("error"), 12, UI.MUTED, false), UI.wrap(this, 6));
            } else {
                content.removeAllViews();
                content.addView(UI.text(this, L.t("در حال نوشتن… معمولاً چند دقیقه طول می‌کشد. می‌توانید از این صفحه بیرون بروید؛ نتیجه در «کارهای اخیر» می‌ماند."), 14, UI.INK, false));
                h.postDelayed(() -> poll(0), 5000);
            }
        });
    }

    private void show(JSONObject j) {
        content.removeAllViews();
        String kind = j.optString("kind");
        titleView.setText(j.optString("title"));
        if ("topics".equals(kind)) {
            content.addView(UI.text(this, L.t("یکی را انتخاب کنید تا ساختار مقاله‌اش ساخته شود:"), 13, UI.MUTED, false));
            JSONArray ts = j.optJSONArray("topics");
            for (int i = 0; ts != null && i < ts.length(); i++) {
                final JSONObject t = ts.optJSONObject(i);
                LinearLayout c = UI.card(this);
                c.addView(UI.text(this, UI.bidi(t.optString("title")), 15, UI.INK, true));
                c.addView(UI.text(this, UI.bidi(t.optString("keyword")) + " · " + t.optString("intent"), 12, UI.GREEN, true), UI.wrap(this, 3));
                c.addView(UI.text(this, UI.bidi(t.optString("angle")), 12, UI.MUTED, false), UI.wrap(this, 3));
                c.setOnClickListener(v -> {
                    try {
                        cfg = new JSONObject();
                        Api.get(this, "/api/app/content", (j2, e2) -> {
                            if (j2 != null) {
                                cfg = j2;
                                form(t.optString("title"), t.optString("keyword"));
                            }
                        });
                    } catch (Exception ignored) {
                    }
                });
                content.addView(c, UI.wrap(this, 10));
            }
        } else if ("outline".equals(kind)) {
            content.addView(UI.text(this, L.t("ساختار را می‌توانید ویرایش کنید (سرتیترها با «## » شروع می‌شوند)، بعد مقاله نوشته می‌شود."), 12, UI.MUTED, false));
            final EditText ed = UI.input(this, "");
            ed.setSingleLine(false);
            ed.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            ed.setMinLines(10);
            ed.setGravity(Gravity.TOP | Gravity.START);
            ed.setText(j.optString("outline"));
            content.addView(ed, UI.wrap(this, 8));
            String c = j.optBoolean("free") ? L.t("رایگان") : fa(j.optJSONObject("cost").optInt("article")) + L.t(" اعتبار");
            final Button go = UI.primary(this, L.t("نوشتن مقاله · ") + c);
            go.setOnClickListener(v -> {
                JSONObject b = new JSONObject();
                try {
                    b.put("outline", ed.getText().toString());
                } catch (Exception ignored) {
                }
                go.setEnabled(false);
                Api.post(this, "/api/app/content/" + id + "/article", b, (j2, e2) -> {
                    go.setEnabled(true);
                    if (j2 == null || e2 != null) {
                        Toast.makeText(this, e2 == null ? L.t("خطایی رخ داد.") : e2, Toast.LENGTH_LONG).show();
                        return;
                    }
                    id = j2.optInt("id");
                    poll(0);
                });
            });
            content.addView(go, UI.wrap(this, 12));
        } else {
            JSONObject a = j.optJSONObject("article");
            final String full = a.optString("title") + "\n\n" + a.optString("body");
            shareText = full;
            content.addView(UI.text(this, UI.bidi(a.optString("title")), 20, UI.INK, true));
            content.addView(UI.text(this, L.t("حدود ") + fa(a.optInt("words")) + L.t(" کلمه"), 12, UI.MUTED, false), UI.wrap(this, 2));
            if (!a.optString("meta").isEmpty()) {
                LinearLayout m = UI.card(this);
                m.addView(UI.text(this, L.t("توضیح متا"), 12, UI.GREEN, true));
                m.addView(UI.text(this, a.optString("meta"), 13, UI.INK, false), UI.wrap(this, 3));
                content.addView(m, UI.wrap(this, 10));
            }
            TextView body = UI.text(this, a.optString("body"), 15, UI.INK, false);
            body.setLineSpacing(UI.dp(this, 4), 1f);
            body.setTextIsSelectable(true);
            content.addView(body, UI.wrap(this, 12));
            Button cp = UI.primary(this, L.t("کپی کل مقاله"));
            cp.setOnClickListener(v -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("article", full));
                Toast.makeText(this, L.t("کپی شد ✓"), Toast.LENGTH_SHORT).show();
            });
            content.addView(cp, UI.wrap(this, 16));
        }
    }
}
