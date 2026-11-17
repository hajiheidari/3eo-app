package ir.threeeo.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** One client site: latest score, run a full analysis, automatic checks, report history, edit/delete. */
public class ClientActivity extends ScreenActivity {
    private int id;
    private JSONObject data;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        id = getIntent().getIntExtra("id", 0);
        titleView.setText("مشتری");
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
        Api.get(this, "/api/app/client/" + id, (j, e) -> {
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
        JSONObject c = data.optJSONObject("client");
        final String host = c.optString("host");
        LinearLayout hero = UI.column(this);
        hero.setBackground(UI.gradient(this, 0xFF059669, 0xFF064E3B, 22));
        int hp = UI.dp(this, 18);
        hero.setPadding(hp, hp, hp, hp);
        hero.addView(UI.text(this, c.optString("name"), 19, 0xFFFFFFFF, true));
        TextView h = UI.text(this, host, 13, 0xFFD1FAE5, false);
        h.setTextDirection(View.TEXT_DIRECTION_LTR);
        hero.addView(h, UI.wrap(this, 2));
        LinearLayout row = new LinearLayout(this);
        UI.rtl(row);
        row.setGravity(Gravity.CENTER_VERTICAL);
        if (!c.isNull("score")) {
            int sc = c.optInt("score");
            TextView s = UI.text(this, UI.fa(String.valueOf(sc)), 34, 0xFFFFFFFF, true);
            row.addView(s);
            if (!c.isNull("prev")) {
                int d = sc - c.optInt("prev");
                TextView dt = UI.text(this, (d > 0 ? "▲ " : (d < 0 ? "▼ " : "")) + UI.fa(String.valueOf(Math.abs(d))), 14, 0xFFA7F3D0, true);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMarginStart(UI.dp(this, 10));
                row.addView(dt, lp);
            }
        } else {
            row.addView(UI.text(this, "هنوز تحلیلی انجام نشده", 13, 0xFFD1FAE5, false));
        }
        hero.addView(row, UI.wrap(this, 8));
        content.addView(hero);

        final Button run = UI.primary(this, "تحلیل کامل همین حالا");
        run.setOnClickListener(v -> {
            run.setEnabled(false);
            run.setText("در حال تحلیل… (تا ۳۰ ثانیه)");
            JSONObject body = new JSONObject();
            try {
                body.put("url", host);
            } catch (Exception ignored) {
            }
            Api.post(this, "/api/app/tool/analyze", body, (j, e) -> {
                run.setEnabled(true);
                run.setText("تحلیل کامل همین حالا");
                if (j == null || e != null) {
                    Toast.makeText(this, e == null ? "خطایی رخ داد." : e, Toast.LENGTH_LONG).show();
                    return;
                }
                Intent i = new Intent(this, ReportActivity.class);
                i.putExtra("json", j.toString());
                startActivity(i);
            });
        });
        content.addView(run, UI.wrap(this, 12));

        // automatic checks
        content.addView(UI.text(this, "بررسی خودکار", 17, UI.INK, true), UI.wrap(this, 20));
        content.addView(UI.text(this, "هر بار اجرا " + UI.fa(String.valueOf(data.optInt("cost"))) + " اعتبار از حساب شما کم می‌کند. اگر اعتبار تمام شود، بررسی خودکار متوقف می‌شود.", 12, UI.MUTED, false), UI.wrap(this, 4));
        JSONArray sch = data.optJSONArray("schedules");
        JSONArray opts = data.optJSONArray("every_options");
        for (int i = 0; sch != null && i < sch.length(); i++) {
            content.addView(scheduleCard(sch.optJSONObject(i), opts), UI.wrap(this, 10));
        }

        // reports
        JSONArray reps = data.optJSONArray("reports");
        content.addView(UI.text(this, "گزارش‌های این سایت", 17, UI.INK, true), UI.wrap(this, 22));
        if (reps == null || reps.length() == 0) {
            content.addView(UI.text(this, "هنوز گزارشی ندارد.", 13, UI.MUTED, false), UI.wrap(this, 6));
        }
        for (int i = 0; reps != null && i < reps.length(); i++) {
            final JSONObject r = reps.optJSONObject(i);
            LinearLayout rr = new LinearLayout(this);
            UI.rtl(rr);
            rr.setGravity(Gravity.CENTER_VERTICAL);
            rr.setBackground(UI.box(this, UI.CARD, 14, UI.LINE));
            int p = UI.dp(this, 12);
            rr.setPadding(p, p, p, p);
            String type = r.optString("type");
            rr.addView(UI.iconTile(this, UI.slugOfType(type), ReportActivity.catOf(type), 38));
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tl.setMarginStart(UI.dp(this, 10));
            rr.addView(UI.text(this, UI.fa(r.optString("created_at").substring(0, Math.min(10, r.optString("created_at").length()))), 13, UI.INK, true), tl);
            if (!r.isNull("score")) {
                rr.addView(UI.text(this, UI.fa(String.valueOf(r.optInt("score"))), 18, UI.scoreColor(r.optInt("score")), true));
            }
            rr.setOnClickListener(v -> {
                Intent i2 = new Intent(this, ReportActivity.class);
                i2.putExtra("token", r.optString("token"));
                startActivity(i2);
            });
            content.addView(rr, UI.wrap(this, 8));
        }

        Button mr = UI.ghost(this, "ساخت گزارش ماهانه برای مشتری (با سربرگ شما)", UI.GREEN);
        mr.setOnClickListener(v -> monthlyDialog());
        content.addView(mr, UI.wrap(this, 18));

        Button edit = UI.ghost(this, "ویرایش نام و یادداشت", UI.GREEN);
        edit.setOnClickListener(v -> editDialog(c));
        content.addView(edit, UI.wrap(this, 24));
        Button del = UI.ghost(this, "حذف این مشتری", UI.RED);
        del.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("حذف مشتری")
                .setMessage("مشتری و بررسی‌های خودکارش حذف می‌شود. گزارش‌ها می‌مانند.")
                .setNegativeButton("انصراف", null)
                .setPositiveButton("حذف", (d, w) -> Api.post(this, "/api/app/client/" + id + "/delete", new JSONObject(), (j, e) -> {
                    if (e != null) {
                        Toast.makeText(this, e, Toast.LENGTH_LONG).show();
                    } else {
                        finish();
                    }
                })).show());
        content.addView(del, UI.wrap(this, 10));
    }

    private View scheduleCard(final JSONObject s, JSONArray opts) {
        LinearLayout card = UI.card(this);
        LinearLayout head = new LinearLayout(this);
        UI.rtl(head);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(UI.text(this, s.optString("title"), 14, UI.INK, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        String st = s.optString("status");
        if ("active".equals(st)) {
            head.addView(UI.text(this, "فعال", 12, UI.GREEN, true));
        } else if ("paused".equals(st)) {
            head.addView(UI.text(this, "متوقف", 12, UI.AMBER, true));
        }
        card.addView(head);
        if (!s.optString("error").isEmpty() && "paused".equals(st)) {
            card.addView(UI.text(this, s.optString("error"), 12, UI.RED, false), UI.wrap(this, 4));
        }
        if ("active".equals(st) && !s.optString("next_run").isEmpty()) {
            card.addView(UI.text(this, "اجرای بعدی: " + UI.fa(s.optString("next_run")) + " (UTC)", 11, UI.MUTED, false), UI.wrap(this, 4));
        }
        final List<String> vals = new ArrayList<>(), names = new ArrayList<>();
        vals.add("");
        names.add("غیرفعال");
        for (int i = 0; opts != null && i < opts.length(); i++) {
            vals.add(opts.optJSONArray(i).optString(0));
            names.add(opts.optJSONArray(i).optString(1));
        }
        final Spinner sp = new Spinner(this);
        sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        sp.setBackground(UI.box(this, 0xFFF8FAFC, 12, UI.LINE));
        sp.setSelection(Math.max(0, vals.indexOf(s.optString("every"))));
        card.addView(sp, UI.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(this, 46), 8));
        Button save = UI.ghost(this, "ذخیره", UI.GREEN);
        save.setOnClickListener(v -> {
            JSONObject body = new JSONObject();
            try {
                body.put("tool", s.optString("tool"));
                body.put("every", vals.get(sp.getSelectedItemPosition()));
            } catch (Exception ignored) {
            }
            Api.post(this, "/api/app/client/" + id + "/schedule", body, (j, e) -> {
                if (e != null) {
                    Toast.makeText(this, e, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "ذخیره شد ✓", Toast.LENGTH_SHORT).show();
                    load();
                }
            });
        });
        card.addView(save, UI.wrap(this, 8));
        return card;
    }

    private void monthlyDialog() {
        Api.get(this, "/api/app/client/" + id + "/monthly", (j, e) -> {
            if (j == null || e != null) {
                Toast.makeText(this, e == null ? "خطا" : e, Toast.LENGTH_LONG).show();
                return;
            }
            if (!j.optBoolean("brand")) {
                Toast.makeText(this, "اول از «سربرگ برند و کلید API» نام برند خود را ثبت کنید.", Toast.LENGTH_LONG).show();
                return;
            }
            final List<Integer> days = new ArrayList<>();
            final List<String> names = new ArrayList<>();
            JSONArray ps = j.optJSONArray("periods");
            for (int i = 0; ps != null && i < ps.length(); i++) {
                days.add(ps.optJSONArray(i).optInt(0));
                names.add(ps.optJSONArray(i).optString(1));
            }
            LinearLayout box = UI.column(this);
            int p = UI.dp(this, 16);
            box.setPadding(p, p, p, 0);
            final Spinner sp = new Spinner(this);
            sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
            box.addView(sp);
            final EditText note = UI.input(this, "یادداشت برای مشتری (اختیاری)");
            box.addView(note, UI.wrap(this, 10));
            box.addView(UI.text(this, "هزینه: " + UI.fa(String.valueOf(j.optInt("cost"))) + " اعتبار", 12, UI.MUTED, false), UI.wrap(this, 8));
            new AlertDialog.Builder(this).setTitle("گزارش ماهانه").setView(box).setNegativeButton("انصراف", null)
                    .setPositiveButton("ساخت", (d, w) -> {
                        JSONObject b = new JSONObject();
                        try {
                            b.put("days", days.get(sp.getSelectedItemPosition()));
                            b.put("note", note.getText().toString());
                        } catch (Exception ignored) {
                        }
                        Api.post(this, "/api/app/client/" + id + "/monthly", b, (r, er) -> {
                            if (r == null || er != null) {
                                Toast.makeText(this, er == null ? "خطا" : er, Toast.LENGTH_LONG).show();
                                return;
                            }
                            Intent s = new Intent(Intent.ACTION_SEND);
                            s.setType("text/plain");
                            s.putExtra(Intent.EXTRA_TEXT, "گزارش سئو\n" + r.optString("url") + "\n\nPDF:\n" + r.optString("pdf"));
                            startActivity(Intent.createChooser(s, "ارسال گزارش به مشتری"));
                        });
                    }).show();
        });
    }

    private void editDialog(JSONObject c) {
        LinearLayout box = UI.column(this);
        int p = UI.dp(this, 16);
        box.setPadding(p, p, p, 0);
        final EditText name = UI.input(this, "نام مشتری");
        name.setText(c.optString("name"));
        final EditText note = UI.input(this, "یادداشت (اختیاری)");
        note.setText(c.optString("note"));
        box.addView(name);
        box.addView(note, UI.wrap(this, 10));
        new AlertDialog.Builder(this).setTitle("ویرایش").setView(box).setNegativeButton("انصراف", null)
                .setPositiveButton("ذخیره", (d, w) -> {
                    JSONObject body = new JSONObject();
                    try {
                        body.put("name", name.getText().toString());
                        body.put("note", note.getText().toString());
                    } catch (Exception ignored) {
                    }
                    Api.post(this, "/api/app/client/" + id, body, (j, e) -> load());
                }).show();
    }
}
