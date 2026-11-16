package ir.threeeo.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

/** The native shell of the app: Home (quick analysis + tools), Reports, Account. Heavy tools open as signed-in web pages. */
public class MainActivity extends AppCompatActivity {
    private FrameLayout frame;
    private LinearLayout[] tabs = new LinearLayout[3];
    private int current = 0;
    private JSONObject me;
    private JSONArray tools;
    private JSONObject categories;
    private EditText urlInput;
    private ScrollView homeScroll;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Api.token(this) == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        LinearLayout root = UI.column(this);
        root.setBackgroundColor(UI.BG);

        LinearLayout top = new LinearLayout(this);
        UI.rtl(top);
        top.setBackgroundColor(UI.CARD);
        top.setGravity(Gravity.CENTER_VERTICAL);
        int p = UI.dp(this, 16);
        top.setPadding(p, UI.dp(this, 8), p, UI.dp(this, 8));
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo);
        logo.setAdjustViewBounds(true);
        top.addView(logo, new LinearLayout.LayoutParams(UI.dp(this, 84), ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        View line = new View(this);
        line.setBackgroundColor(UI.LINE);
        root.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        frame = new FrameLayout(this);
        root.addView(frame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        View line2 = new View(this);
        line2.setBackgroundColor(UI.LINE);
        root.addView(line2, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        LinearLayout bar = new LinearLayout(this);
        UI.rtl(bar);
        bar.setBackgroundColor(UI.CARD);
        String[] names = {"خانه", "گزارش‌ها", "حساب من"};
        String[] icons = {"⌂", "▤", "☺"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            LinearLayout t = UI.column(this);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, UI.dp(this, 8), 0, UI.dp(this, 8));
            TextView ic = UI.text(this, icons[i], 20, UI.MUTED, false);
            ic.setGravity(Gravity.CENTER);
            TextView nm = UI.text(this, names[i], 12, UI.MUTED, true);
            nm.setGravity(Gravity.CENTER);
            t.addView(ic);
            t.addView(nm);
            t.setOnClickListener(v -> show(idx));
            tabs[i] = t;
            bar.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(root);
        show(0);
        loadTools();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (frame != null) {
            loadMe();
        }
    }

    private void loadMe() {
        Api.get(this, "/api/app/me", (j, e) -> {
            if (Api.token(this) == null) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }
            if (j != null && e == null) {
                me = j;
                if (current != 0) {
                    show(current);
                }
            }
        });
    }

    private void loadTools() {
        Api.get(this, "/api/app/tools", (j, e) -> {
            if (j != null && e == null) {
                tools = j.optJSONArray("tools");
                categories = j.optJSONObject("categories");
                if (current == 0) {
                    show(0);
                }
            }
        });
    }

    private void show(int i) {
        current = i;
        for (int k = 0; k < 3; k++) {
            int col = k == i ? UI.GREEN : UI.MUTED;
            ((TextView) tabs[k].getChildAt(0)).setTextColor(col);
            ((TextView) tabs[k].getChildAt(1)).setTextColor(col);
        }
        frame.removeAllViews();
        frame.addView(i == 0 ? homeView() : (i == 1 ? reportsView() : accountView()));
    }

    private ScrollView scroller(LinearLayout inner) {
        ScrollView sv = new ScrollView(this);
        int p = UI.dp(this, 16);
        inner.setPadding(p, p, p, UI.dp(this, 28));
        sv.addView(inner, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        UI.rtl(sv);
        return sv;
    }

    // ---------- Home
    private View homeView() {
        LinearLayout col = UI.column(this);
        LinearLayout hero = UI.column(this);
        hero.setBackground(UI.box(this, UI.GREEN, 20, 0));
        int hp = UI.dp(this, 18);
        hero.setPadding(hp, hp, hp, hp);
        hero.addView(UI.text(this, "تحلیل سریع سایت", 19, 0xFFFFFFFF, true));
        hero.addView(UI.text(this, "آدرس سایت را بدهید؛ در چند ثانیه امتحان سئو، سرعت پاسخ، امنیت و آمادگی برای هوش مصنوعی را ببینید.", 13, 0xFFD1FAE5, false), UI.wrap(this, 6));
        urlInput = UI.input(this, "example.com");
        urlInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        urlInput.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        urlInput.setTextDirection(View.TEXT_DIRECTION_LTR);
        hero.addView(urlInput, UI.wrap(this, 14));
        final Button go = UI.primary(this, "تحلیل کن");
        go.setBackground(UI.box(this, UI.INK, 12, 0));
        hero.addView(go, UI.wrap(this, 10));
        go.setOnClickListener(v -> analyze(go));
        col.addView(hero, UI.wrap(this, 0));

        if (tools == null) {
            col.addView(UI.text(this, "در حال دریافت ابزارها…", 14, UI.MUTED, false), UI.wrap(this, 20));
        } else {
            String[] order = {"ai", "tech", "keyword", "content", "data", "util"};
            for (String cat : order) {
                boolean header = false;
                for (int i = 0; i < tools.length(); i++) {
                    JSONObject t = tools.optJSONObject(i);
                    if (!cat.equals(t.optString("cat"))) {
                        continue;
                    }
                    if (!header) {
                        col.addView(UI.text(this, categories != null ? categories.optString(cat, cat) : cat, 16, UI.INK, true), UI.wrap(this, 22));
                        header = true;
                    }
                    col.addView(toolRow(t), UI.wrap(this, 8));
                }
            }
        }
        homeScroll = scroller(col);
        return homeScroll;
    }

    private View toolRow(final JSONObject t) {
        LinearLayout row = new LinearLayout(this);
        UI.rtl(row);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(UI.box(this, UI.CARD, 14, UI.LINE));
        int p = UI.dp(this, 14);
        row.setPadding(p, p, p, p);
        LinearLayout txt = UI.column(this);
        txt.addView(UI.text(this, t.optString("title"), 15, UI.INK, true));
        txt.addView(UI.text(this, t.optString("desc"), 12, UI.MUTED, false), UI.wrap(this, 3));
        row.addView(txt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(UI.text(this, "‹", 22, UI.GREEN, true));
        row.setOnClickListener(v -> openTool(t.optString("slug")));
        return row;
    }

    private void openTool(String slug) {
        if ("analyze".equals(slug)) {
            if (homeScroll != null) {
                homeScroll.smoothScrollTo(0, 0);
            }
            if (urlInput != null) {
                urlInput.requestFocus();
            }
            return;
        }
        openWeb("/tools/" + slug);
    }

    /** opens a website page already signed in */
    private void openWeb(String path) {
        Toast.makeText(this, "در حال باز کردن…", Toast.LENGTH_SHORT).show();
        JSONObject body = new JSONObject();
        try {
            body.put("to", path);
        } catch (Exception ignored) {
        }
        Api.post(this, "/api/app/webauth", body, (j, e) -> {
            if (j == null || e != null) {
                Toast.makeText(this, e == null ? "خطایی رخ داد." : e, Toast.LENGTH_LONG).show();
                return;
            }
            Intent i = new Intent(this, WebActivity.class);
            i.putExtra("url", j.optString("url"));
            startActivity(i);
        });
    }

    private void analyze(final Button go) {
        String u = urlInput.getText().toString().trim();
        if (u.length() < 4) {
            Toast.makeText(this, "آدرس سایت را وارد کنید.", Toast.LENGTH_SHORT).show();
            return;
        }
        go.setEnabled(false);
        go.setText("در حال تحلیل… (تا ۳۰ ثانیه)");
        JSONObject body = new JSONObject();
        try {
            body.put("url", u);
        } catch (Exception ignored) {
        }
        Api.post(this, "/api/app/analyze", body, (j, e) -> {
            go.setEnabled(true);
            go.setText("تحلیل کن");
            if (j == null || e != null) {
                Toast.makeText(this, e == null ? "خطایی رخ داد." : e, Toast.LENGTH_LONG).show();
                return;
            }
            Intent i = new Intent(this, ReportActivity.class);
            i.putExtra("json", j.toString());
            startActivity(i);
        });
    }

    // ---------- Reports
    private View reportsView() {
        LinearLayout col = UI.column(this);
        col.addView(UI.text(this, "گزارش‌های من", 19, UI.INK, true));
        JSONArray rs = me == null ? null : me.optJSONArray("reports");
        boolean any = false;
        for (int i = 0; rs != null && i < rs.length(); i++) {
            final JSONObject r = rs.optJSONObject(i);
            if (!"analyze".equals(r.optString("type"))) {
                continue;
            }
            any = true;
            LinearLayout row = new LinearLayout(this);
            UI.rtl(row);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(UI.box(this, UI.CARD, 14, UI.LINE));
            int p = UI.dp(this, 14);
            row.setPadding(p, p, p, p);
            LinearLayout txt = UI.column(this);
            txt.addView(UI.text(this, r.optString("host"), 15, UI.INK, true));
            String when = r.optString("created_at");
            txt.addView(UI.text(this, when.length() >= 10 ? UI.fa(when.substring(0, 10)) : when, 12, UI.MUTED, false), UI.wrap(this, 3));
            row.addView(txt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            int sc = r.optInt("score");
            row.addView(UI.text(this, UI.fa(String.valueOf(sc)), 20, UI.scoreColor(sc), true));
            row.setOnClickListener(v -> {
                Intent i = new Intent(this, ReportActivity.class);
                i.putExtra("token", r.optString("token"));
                startActivity(i);
            });
            col.addView(row, UI.wrap(this, 10));
        }
        if (!any) {
            col.addView(UI.text(this, me == null ? "در حال دریافت…" : "هنوز گزارشی ندارید. از صفحه‌ی خانه یک سایت را تحلیل کنید.", 14, UI.MUTED, false), UI.wrap(this, 12));
        }
        return scroller(col);
    }

    // ---------- Account
    private View accountView() {
        LinearLayout col = UI.column(this);
        col.addView(UI.text(this, "حساب من", 19, UI.INK, true));
        LinearLayout card = UI.card(this);
        card.addView(UI.text(this, "شماره‌ی موبایل", 12, UI.MUTED, false));
        card.addView(UI.text(this, me == null ? "…" : UI.fa(me.optString("mobile")), 17, UI.INK, true), UI.wrap(this, 2));
        card.addView(UI.text(this, "اعتبار شما", 12, UI.MUTED, false), UI.wrap(this, 14));
        card.addView(UI.text(this, me == null ? "…" : UI.fa(String.valueOf(me.optInt("credits"))), 30, UI.GREEN, true), UI.wrap(this, 2));
        col.addView(card, UI.wrap(this, 12));

        Button panel = UI.primary(this, "پنل کامل در وب");
        panel.setOnClickListener(v -> openWeb("/dashboard"));
        col.addView(panel, UI.wrap(this, 16));
        Button privacy = UI.ghost(this, "حریم خصوصی و قوانین", UI.GREEN);
        privacy.setOnClickListener(v -> {
            Intent i = new Intent(this, WebActivity.class);
            i.putExtra("url", "https://3eo.ir/privacy");
            startActivity(i);
        });
        col.addView(privacy, UI.wrap(this, 10));
        Button out = UI.ghost(this, "خروج از حساب", UI.INK);
        out.setOnClickListener(v -> {
            Api.logout(this);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        col.addView(out, UI.wrap(this, 10));
        Button del = UI.ghost(this, "حذف حساب کاربری", UI.RED);
        del.setOnClickListener(v -> confirmDelete());
        col.addView(del, UI.wrap(this, 10));
        return scroller(col);
    }

    private void confirmDelete() {
        final EditText in = UI.input(this, "حذف");
        LinearLayout box = UI.column(this);
        int p = UI.dp(this, 20);
        box.setPadding(p, UI.dp(this, 8), p, 0);
        box.addView(in);
        new AlertDialog.Builder(this)
                .setTitle("حذف حساب کاربری")
                .setMessage("همه‌ی اطلاعات و گزارش‌های حساب شما برای همیشه پاک می‌شود و برگشت‌پذیر نیست. برای تأیید، کلمه‌ی «حذف» را بنویسید.")
                .setView(box)
                .setNegativeButton("انصراف", null)
                .setPositiveButton("حذف حساب", (d, w) -> {
                    JSONObject body = new JSONObject();
                    try {
                        body.put("confirm", in.getText().toString().trim());
                    } catch (Exception ignored) {
                    }
                    Api.post(this, "/api/app/delete", body, (j, e) -> {
                        if (e != null) {
                            Toast.makeText(this, e, Toast.LENGTH_LONG).show();
                            return;
                        }
                        Api.logout(this);
                        Toast.makeText(this, "حساب شما حذف شد.", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    });
                })
                .show();
    }
}
