package ir.threeeo.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONArray;
import org.json.JSONObject;

/** Keyword rank: one-time check (polled), daily/weekly tracking, history. */
public class RankActivity extends ScreenActivity {
    private JSONObject data;
    private final Handler h = new Handler(Looper.getMainLooper());
    private LinearLayout resultBox;
    private EditText domain, kws;
    private boolean alive = true;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        titleView.setText(L.t("رتبه‌ی کلمات کلیدی"));
        load();
    }

    @Override
    protected void onDestroy() {
        alive = false;
        super.onDestroy();
    }

    private void load() {
        Api.get(this, "/api/app/rank", (j, e) -> {
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
        String d0 = domain == null ? "" : domain.getText().toString(), k0 = kws == null ? "" : kws.getText().toString();
        content.removeAllViews();
        content.addView(UI.text(this, L.t("جایگاه سایت شما در نتایج گوگل ایران برای هر کلمه (تا ۵۰ نتیجه‌ی اول). اعتبار شما: ") + UI.fa(String.valueOf(data.optInt("credits"))), 12, UI.MUTED, false));
        if (!data.optBoolean("enabled", true)) {
            content.addView(UI.text(this, L.t("این ابزار هنوز فعال نشده است."), 14, UI.RED, true), UI.wrap(this, 8));
        }
        LinearLayout card = UI.card(this);
        domain = UI.input(this, L.t("آدرس سایت (مثل example.ir)"));
        domain.setTextDirection(View.TEXT_DIRECTION_LTR);
        domain.setText(d0);
        card.addView(domain);
        kws = UI.input(this, L.t("کلمات کلیدی (هر کلمه در یک خط، تا ") + UI.fa(String.valueOf(data.optInt("max"))) + L.t(" کلمه)"));
        kws.setSingleLine(false);
        kws.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        kws.setMinLines(3);
        kws.setGravity(Gravity.TOP | Gravity.START);
        kws.setText(k0);
        card.addView(kws, UI.wrap(this, 8));
        Button check = UI.primary(this, L.t("بررسی یک‌باره · هر کلمه ") + cost("check"));
        check.setOnClickListener(v -> check(check));
        card.addView(check, UI.wrap(this, 10));
        Button wk = UI.ghost(this, L.t("پایش هفتگی · هر کلمه ") + cost("weekly"), UI.GREEN);
        wk.setOnClickListener(v -> track("weekly"));
        card.addView(wk, UI.wrap(this, 8));
        Button dl = UI.ghost(this, L.t("پایش روزانه · هر کلمه ") + cost("daily"), UI.GREEN);
        dl.setOnClickListener(v -> track("daily"));
        card.addView(dl, UI.wrap(this, 6));
        content.addView(card, UI.wrap(this, 10));
        resultBox = UI.column(this);
        content.addView(resultBox, UI.wrap(this, 6));

        JSONObject s = data.optJSONObject("summary");
        JSONArray tr = data.optJSONArray("tracks");
        content.addView(UI.text(this, L.t("کلمات تحت پایش"), 17, UI.INK, true), UI.wrap(this, 20));
        if (tr == null || tr.length() == 0) {
            content.addView(UI.text(this, L.t("هنوز کلمه‌ای را پایش نمی‌کنید."), 13, UI.MUTED, false), UI.wrap(this, 6));
        } else if (s != null && !s.isNull("avg")) {
            content.addView(UI.text(this, L.t("میانگین رتبه ") + UI.fa(String.valueOf(s.optDouble("avg"))) + " · " + UI.fa(String.valueOf(s.optInt("top3"))) + L.t(" کلمه در ۳ رتبه‌ی اول · ") + UI.fa(String.valueOf(s.optInt("top10"))) + L.t(" کلمه در صفحه‌ی اول"), 12, UI.MUTED, false), UI.wrap(this, 4));
        }
        for (int i = 0; tr != null && i < tr.length(); i++) {
            final JSONObject t = tr.optJSONObject(i);
            LinearLayout row = new LinearLayout(this);
            UI.rtl(row);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(UI.box(this, UI.CARD, 14, UI.LINE));
            int p = UI.dp(this, 12);
            row.setPadding(p, p, p, p);
            LinearLayout info = UI.column(this);
            info.addView(UI.text(this, UI.bidi(t.optString("keyword")), 14, UI.INK, true));
            info.addView(UI.text(this, UI.bidi(t.optString("domain")) + " · " + ("daily".equals(t.optString("every")) ? L.t("روزانه") : L.t("هفتگی")) + (t.optBoolean("active") ? "" : L.t(" · منقضی")), 11, UI.MUTED, false));
            row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            Object now = t.opt("now");
            String pos;
            int col = UI.MUTED;
            if ("pending".equals(now)) {
                pos = L.t("در انتظار");
            } else if (t.isNull("now")) {
                pos = L.t("خارج از ۵۰");
                col = UI.RED;
            } else {
                int n = t.optInt("now");
                pos = UI.fa(String.valueOf(n));
                col = n <= 3 ? UI.GREEN : (n <= 10 ? UI.AMBER : UI.INK);
            }
            LinearLayout right = UI.column(this);
            right.setGravity(Gravity.CENTER_HORIZONTAL);
            right.addView(UI.text(this, pos, 18, col, true));
            if (!t.isNull("delta") && t.optInt("delta") != 0) {
                int d = t.optInt("delta");
                right.addView(UI.text(this, (d > 0 ? "▲ " : "▼ ") + UI.fa(String.valueOf(Math.abs(d))), 11, d > 0 ? UI.GREEN : UI.RED, true));
            }
            row.addView(right);
            row.setOnClickListener(v -> history(t));
            content.addView(row, UI.wrap(this, 8));
        }
    }

    private boolean fields() {
        if (domain.getText().toString().trim().isEmpty() || kws.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, L.t("آدرس سایت و دست‌کم یک کلمه را وارد کنید."), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private JSONObject body() {
        JSONObject b = new JSONObject();
        try {
            b.put("domain", domain.getText().toString().trim());
            b.put("keywords", kws.getText().toString());
        } catch (Exception ignored) {
        }
        return b;
    }

    private void check(final Button btn) {
        if (!fields()) {
            return;
        }
        btn.setEnabled(false);
        Api.post(this, "/api/app/rank/check", body(), (j, e) -> {
            btn.setEnabled(true);
            if (j == null || e != null) {
                Toast.makeText(this, e == null ? L.t("خطایی رخ داد.") : e, Toast.LENGTH_LONG).show();
                return;
            }
            resultBox.removeAllViews();
            resultBox.addView(UI.text(this, L.t("در حال بررسی…"), 13, UI.MUTED, false));
            poll(j.optInt("job"), 0);
        });
    }

    private void poll(final int job, final int n) {
        h.postDelayed(() -> {
            if (!alive) {
                return;
            }
            Api.get(this, "/api/app/rank/job/" + job, (j, e) -> {
                if (!alive || resultBox == null) {
                    return;
                }
                if (j == null || e != null) {
                    if (n < 40) {
                        poll(job, n + 1);
                    }
                    return;
                }
                resultBox.removeAllViews();
                LinearLayout card = UI.card(this);
                JSONArray rows = j.optJSONArray("rows");
                for (int i = 0; rows != null && i < rows.length(); i++) {
                    JSONObject r = rows.optJSONObject(i);
                    LinearLayout line = new LinearLayout(this);
                    UI.rtl(line);
                    line.setGravity(Gravity.CENTER_VERTICAL);
                    line.addView(UI.text(this, UI.bidi(r.optString("kw")), 14, UI.INK, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    String st = r.optString("state");
                    String txt;
                    int col = UI.MUTED;
                    if ("done".equals(st)) {
                        if (r.isNull("pos")) {
                            txt = L.t("خارج از ۵۰");
                            col = UI.RED;
                        } else {
                            int p = r.optInt("pos");
                            txt = L.t("رتبه ") + UI.fa(String.valueOf(p));
                            col = p <= 3 ? UI.GREEN : (p <= 10 ? UI.AMBER : UI.INK);
                        }
                    } else if ("error".equals(st)) {
                        txt = L.t("خطا (برگشت اعتبار)");
                        col = UI.RED;
                    } else {
                        txt = L.t("در حال بررسی…");
                    }
                    line.addView(UI.text(this, txt, 14, col, true));
                    card.addView(line, UI.wrap(this, i == 0 ? 0 : 8));
                }
                resultBox.addView(card);
                if (!j.optBoolean("done") && n < 40) {
                    poll(job, n + 1);
                } else {
                    load();
                }
            });
        }, 3000);
    }

    private void track(String every) {
        if (!fields()) {
            return;
        }
        JSONObject b = body();
        try {
            b.put("every", every);
        } catch (Exception ignored) {
        }
        Api.post(this, "/api/app/rank/track", b, (j, e) -> {
            if (j == null || e != null) {
                Toast.makeText(this, e == null ? L.t("خطایی رخ داد.") : e, Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(this, L.t("پایش شروع شد؛ اولین نتیجه تا چند دقیقه‌ی دیگر ثبت می‌شود."), Toast.LENGTH_LONG).show();
            kws.setText("");
            load();
        });
    }

    private void history(final JSONObject t) {
        Api.get(this, "/api/app/rank/t/" + t.optInt("id"), (j, e) -> {
            if (j == null || e != null) {
                Toast.makeText(this, e == null ? L.t("خطا") : e, Toast.LENGTH_SHORT).show();
                return;
            }
            StringBuilder sb = new StringBuilder();
            JSONArray rows = j.optJSONArray("rows");
            for (int i = 0; rows != null && i < Math.min(rows.length(), 30); i++) {
                JSONObject r = rows.optJSONObject(i);
                sb.append(UI.fa(r.optString("day"))).append("   ").append(r.isNull("pos") ? L.t("خارج از ۵۰") : L.t("رتبه ") + UI.fa(String.valueOf(r.optInt("pos")))).append("\n");
            }
            if (sb.length() == 0) {
                sb.append(L.t("هنوز نتیجه‌ای ثبت نشده است."));
            }
            new AlertDialog.Builder(this).setTitle(t.optString("keyword")).setMessage(sb.toString())
                    .setNegativeButton(L.t("بستن"), null)
                    .setPositiveButton(L.t("حذف از پایش"), (d, w) -> Api.post(this, "/api/app/rank/t/" + t.optInt("id") + "/delete", new JSONObject(), (j2, e2) -> load()))
                    .show();
        });
    }
}
