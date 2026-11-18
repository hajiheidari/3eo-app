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

import java.util.ArrayList;
import java.util.List;

/** The native shell of the app: Home (quick analysis + tools), Reports, Account. All tools run natively. */
public class MainActivity extends AppCompatActivity {
    private FrameLayout frame;
    private LinearLayout[] tabs = new LinearLayout[5];
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

        frame = new FrameLayout(this);
        root.addView(frame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        View line2 = new View(this);
        line2.setBackgroundColor(UI.LINE);
        root.addView(line2, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        LinearLayout bar = new LinearLayout(this);
        UI.rtl(bar);
        bar.setBackgroundColor(UI.CARD);
        String[] names = {L.t("خانه"), L.t("گزارش‌ها"), L.t("حساب من"), L.t("حرفه‌ای"), L.t("وبلاگ")};
        String[] icons = {"", "▤", "☺", "♚", "✎"};
        int[] posOrder = {1, 3, 0, 4, 2};
        for (int pi = 0; pi < 5; pi++) {
            final int idx = posOrder[pi];
            LinearLayout t = UI.column(this);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, UI.dp(this, 6), 0, UI.dp(this, 6));
            if (idx == 0) {
                ImageView lg = new ImageView(this);
                lg.setImageResource(R.drawable.logo);
                lg.setAdjustViewBounds(true);
                t.addView(lg, new LinearLayout.LayoutParams(UI.dp(this, 70), UI.dp(this, 40)));
            } else {
                TextView ic = UI.text(this, icons[idx], 20, UI.MUTED, false);
                ic.setGravity(Gravity.CENTER);
                TextView nm = UI.text(this, names[idx], 12, UI.MUTED, true);
                nm.setGravity(Gravity.CENTER);
                t.addView(ic);
                t.addView(nm);
            }
            t.setOnClickListener(v -> show(idx));
            tabs[idx] = t;
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
            if (current == 3) {
                show(3);
            }
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
        for (int k = 0; k < 5; k++) {
            int col = k == i ? UI.GREEN : UI.MUTED;
            if (k == 0) {
                tabs[0].setAlpha(k == i ? 1f : 0.75f);
            } else {
                ((TextView) tabs[k].getChildAt(0)).setTextColor(col);
                ((TextView) tabs[k].getChildAt(1)).setTextColor(col);
            }
        }
        frame.removeAllViews();
        frame.addView(i == 0 ? homeView() : (i == 1 ? reportsView() : (i == 3 ? clientsView() : (i == 4 ? blogView() : accountView()))));
    }

    // ---------- Blog
    private JSONArray blogPosts;
    private int blogPage = 1;
    private boolean blogMore = true, blogBusy = false;
    private String blogErr;

    private View blogView() {
        LinearLayout col = UI.column(this);
        col.addView(UI.text(this, L.t("وبلاگ سئو"), 20, UI.INK, true));
        col.addView(UI.text(this, L.t("آخرین مقاله‌های آموزشی ما؛ همه‌ی متن‌ها همین‌جا داخل اپ خوانده می‌شود."), 12, UI.MUTED, false), UI.wrap(this, 4));
        final LinearLayout list = UI.column(this);
        col.addView(list, UI.wrap(this, 10));
        paintBlog(list);
        if (blogPosts == null) {
            loadBlog(list);
        }
        return scroller(col);
    }

    private void loadBlog(final LinearLayout list) {
        if (blogBusy) {
            return;
        }
        blogBusy = true;
        Blog.posts(blogPage, (arr, err) -> {
            blogBusy = false;
            if (arr == null) {
                if (blogPosts != null) {
                    blogMore = false;
                } else {
                    blogErr = err;
                }
            } else {
                if (blogPosts == null) {
                    blogPosts = new JSONArray();
                }
                for (int i = 0; i < arr.length(); i++) {
                    blogPosts.put(arr.opt(i));
                }
                blogErr = null;
                blogMore = arr.length() >= 12;
                blogPage++;
            }
            if (current == 4) {
                paintBlog(list);
            }
        });
    }

    private void paintBlog(final LinearLayout list) {
        list.removeAllViews();
        if (blogPosts == null) {
            list.addView(UI.text(this, blogErr == null ? L.t("در حال دریافت…") : blogErr, 14, blogErr == null ? UI.MUTED : UI.RED, false));
            return;
        }
        for (int i = 0; i < blogPosts.length(); i++) {
            final JSONObject p = blogPosts.optJSONObject(i);
            LinearLayout card = UI.column(this);
            card.setBackground(UI.box(this, UI.CARD, 14, UI.LINE));
            int pd = UI.dp(this, 12);
            card.setPadding(pd, pd, pd, pd);
            String th = Blog.thumb(p);
            if (!th.isEmpty()) {
                android.widget.ImageView iv = new android.widget.ImageView(this);
                iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                iv.setBackground(UI.box(this, UI.GREEN_SOFT, 10, UI.LINE));
                card.addView(iv, UI.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(this, 150), 0));
                Blog.image(iv, th);
            }
            card.addView(UI.text(this, UI.bidi(Blog.text(p.optJSONObject("title").optString("rendered"))), 16, UI.INK, true), UI.wrap(this, 8));
            String ex = Blog.text(p.optJSONObject("excerpt") == null ? "" : p.optJSONObject("excerpt").optString("rendered"));
            if (!ex.isEmpty()) {
                card.addView(UI.text(this, UI.bidi(ex.length() > 140 ? ex.substring(0, 140) + "…" : ex), 12, UI.MUTED, false), UI.wrap(this, 4));
            }
            String d = p.optString("date");
            card.addView(UI.text(this, UI.fa(d.length() >= 10 ? d.substring(0, 10) : d), 11, UI.GREEN, true), UI.wrap(this, 6));
            card.setOnClickListener(v -> {
                Blog.current = p;
                startActivity(new Intent(this, BlogPostActivity.class));
            });
            list.addView(card, UI.wrap(this, 12));
        }
        if (blogMore) {
            Button more = UI.ghost(this, L.t("مقاله‌های بیشتر"), UI.GREEN);
            more.setOnClickListener(v -> loadBlog(list));
            list.addView(more, UI.wrap(this, 6));
        }
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
    private String filterCat = "";
    private String query = "";
    private LinearLayout grid;

    private View homeView() {
        LinearLayout col = UI.column(this);
        LinearLayout hero = UI.column(this);
        hero.setBackground(UI.gradient(this, 0xFF059669, 0xFF064E3B, 26));
        int hp = UI.dp(this, 20);
        hero.setPadding(hp, hp, hp, hp);
        hero.addView(UI.text(this, L.t("سلام 👋"), 14, 0xFFA7F3D0, true));
        hero.addView(UI.text(this, L.t("سئوی سایتت را در چند ثانیه بسنج"), 22, 0xFFFFFFFF, true), UI.wrap(this, 4));
        hero.addView(UI.text(this, L.t("امتحان سئو، سرعت، امنیت و آمادگی برای هوش مصنوعی؛ همه با یک آدرس."), 13, 0xFFD1FAE5, false), UI.wrap(this, 6));
        LinearLayout bar = new LinearLayout(this);
        UI.rtl(bar);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackground(UI.box(this, 0xFFFFFFFF, 16, 0));
        bar.setPadding(UI.dp(this, 14), UI.dp(this, 4), UI.dp(this, 6), UI.dp(this, 4));
        urlInput = new EditText(this);
        urlInput.setHint("example.com");
        urlInput.setTypeface(UI.face(this, false));
        urlInput.setHintTextColor(0xFF94A3B8);
        urlInput.setTextColor(UI.INK);
        urlInput.setTextSize(16);
        urlInput.setSingleLine(true);
        urlInput.setBackground(null);
        urlInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        urlInput.setTextDirection(View.TEXT_DIRECTION_LTR);
        urlInput.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        bar.addView(urlInput, new LinearLayout.LayoutParams(0, UI.dp(this, 48), 1f));
        final Button go = UI.primary(this, L.t("تحلیل"));
        go.setBackground(UI.gradient(this, 0xFF059669, 0xFF047857, 12));
        go.setOnClickListener(v -> analyze(go));
        bar.addView(go, new LinearLayout.LayoutParams(UI.dp(this, 92), UI.dp(this, 44)));
        hero.addView(bar, UI.wrap(this, 16));
        col.addView(hero, UI.wrap(this, 0));

        EditText search = UI.input(this, L.t("جستجوی ابزار…"));
        search.setText(query);
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence a, int b, int c, int d) {
            }

            public void onTextChanged(CharSequence a, int b, int c, int d) {
            }

            public void afterTextChanged(android.text.Editable e) {
                query = e.toString().trim();
                fillGrid();
            }
        });
        col.addView(search, UI.wrap(this, 16));

        LinearLayout chips = UI.column(this);
        String[] order = {"", "ai", "tech", "keyword", "content", "data"};
        LinearLayout crow = null;
        for (int ci = 0; ci < order.length; ci++) {
            final String cat = order[ci];
            if (ci % 3 == 0) {
                crow = new LinearLayout(this);
                UI.rtl(crow);
                chips.addView(crow, UI.wrap(this, ci == 0 ? 0 : 8));
            }
            String name = cat.isEmpty() ? L.t("همه") : (categories != null ? categories.optString(cat, cat) : cat);
            final TextView ch = UI.text(this, name, 13, UI.INK, true);
            ch.setGravity(Gravity.CENTER);
            ch.setSingleLine(true);
            ch.setPadding(UI.dp(this, 6), UI.dp(this, 9), UI.dp(this, 6), UI.dp(this, 9));
            boolean on = cat.equals(filterCat);
            ch.setBackground(UI.box(this, on ? (cat.isEmpty() ? UI.GREEN : UI.catColor(cat)) : 0xFFFFFFFF, 18, on ? 0 : UI.LINE));
            ch.setTextColor(on ? 0xFFFFFFFF : UI.INK);
            ch.setOnClickListener(v -> {
                filterCat = cat;
                show(0);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(UI.dp(this, 4), 0, UI.dp(this, 4), 0);
            crow.addView(ch, lp);
        }
        col.addView(chips, UI.wrap(this, 12));

        grid = UI.column(this);
        col.addView(grid, UI.wrap(this, 8));
        fillGrid();
        homeScroll = scroller(col);
        return homeScroll;
    }

    private void fillGrid() {
        if (grid == null) {
            return;
        }
        grid.removeAllViews();
        if (tools == null) {
            grid.addView(UI.text(this, L.t("در حال دریافت ابزارها…"), 14, UI.MUTED, false), UI.wrap(this, 20));
            return;
        }
        List<JSONObject> list = new ArrayList<>();
        String[] order = {"ai", "tech", "keyword", "content", "data", "util"};
        for (String cat : order) {
            for (int i = 0; i < tools.length(); i++) {
                JSONObject t = tools.optJSONObject(i);
                if (!cat.equals(t.optString("cat")) || (!filterCat.isEmpty() && !filterCat.equals(cat))) {
                    continue;
                }
                if (!query.isEmpty() && !(t.optString("title") + " " + t.optString("desc")).contains(query)) {
                    continue;
                }
                list.add(t);
            }
        }
        if (list.isEmpty()) {
            grid.addView(UI.text(this, L.t("ابزاری پیدا نشد."), 14, UI.MUTED, false), UI.wrap(this, 20));
        }
        for (int i = 0; i < list.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            UI.rtl(row);
            for (int k = 0; k < 2; k++) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                lp.setMargins(UI.dp(this, k == 0 ? 0 : 5), 0, UI.dp(this, k == 0 ? 5 : 0), 0);
                row.addView(i + k < list.size() ? tile(list.get(i + k)) : new View(this), lp);
            }
            grid.addView(row, UI.wrap(this, 10));
        }
    }

    private View tile(final JSONObject t) {
        LinearLayout c = UI.column(this);
        c.setBackground(UI.box(this, UI.CARD, 18, UI.LINE));
        int p = UI.dp(this, 14);
        c.setPadding(p, p, p, p);
        c.addView(UI.iconTile(this, t.optString("slug"), t.optString("cat"), 46));
        TextView title = UI.text(this, t.optString("title"), 14, UI.INK, true);
        title.setMaxLines(2);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        c.addView(title, UI.wrap(this, 10));
        TextView d = UI.text(this, t.optString("desc"), 11, UI.MUTED, false);
        d.setMaxLines(2);
        d.setEllipsize(android.text.TextUtils.TruncateAt.END);
        c.addView(d, UI.wrap(this, 4));
        boolean paid = t.optBoolean("paid");
        TextView cost = UI.text(this, paid ? L.t("سهمیه‌ی روزانه + اعتبار") : L.t("رایگان"), 11, paid ? UI.AMBER : UI.GREEN, true);
        cost.setBackground(UI.box(this, paid ? 0x1AD97706 : 0x1A047857, 10, 0));
        cost.setPadding(UI.dp(this, 8), UI.dp(this, 3), UI.dp(this, 8), UI.dp(this, 3));
        c.addView(cost, UI.wrap(this, 8));
        c.setOnClickListener(v -> openTool(t));
        return c;
    }

    private void openTool(JSONObject t) {
        if ("analyze".equals(t.optString("slug")) && urlInput != null && !urlInput.getText().toString().trim().isEmpty()) {
            analyze(null);
            return;
        }
        Intent i = new Intent(this, ToolActivity.class);
        i.putExtra("tool", t.toString());
        startActivity(i);
    }

    private void analyze(final Button go) {
        String u = urlInput.getText().toString().trim();
        if (u.length() < 4) {
            Toast.makeText(this, L.t("آدرس سایت را وارد کنید."), Toast.LENGTH_SHORT).show();
            return;
        }
        if (go != null) {
            go.setEnabled(false);
            go.setText("…");
        } else {
            Toast.makeText(this, L.t("در حال تحلیل… (تا ۳۰ ثانیه)"), Toast.LENGTH_LONG).show();
        }
        JSONObject body = new JSONObject();
        try {
            body.put("url", u);
        } catch (Exception ignored) {
        }
        Api.post(this, "/api/app/tool/analyze", body, (j, e) -> {
            if (go != null) {
                go.setEnabled(true);
                go.setText(L.t("تحلیل"));
            }
            if (j == null || e != null) {
                Toast.makeText(this, e == null ? L.t("خطایی رخ داد.") : e, Toast.LENGTH_LONG).show();
                return;
            }
            Intent i = new Intent(this, ReportActivity.class);
            i.putExtra("json", j.toString());
            startActivity(i);
        });
    }

    // ---------- Clients
    private JSONObject clientsData;

    private boolean seoer() {
        return me != null && "seoer".equals(me.optString("mode"));
    }

    private void setMode(String mode) {
        JSONObject b = new JSONObject();
        try {
            b.put("mode", mode);
        } catch (Exception ignored) {
        }
        Api.post(this, "/api/app/mode", b, (j, e) -> {
            if (e != null) {
                Toast.makeText(this, e, Toast.LENGTH_LONG).show();
                return;
            }
            try {
                me.put("mode", mode);
            } catch (Exception ignored) {
            }
            show(3);
        });
    }

    private View clientsView() {
        LinearLayout col = UI.column(this);
        col.addView(UI.text(this, L.t("ابزارهای حرفه‌ای"), 20, UI.INK, true));
        col.addView(UI.text(this, L.t("تولید محتوا، رتبه‌یابی، پایش برند و گزارش برای مشتریان."), 12, UI.MUTED, false), UI.wrap(this, 4));
        col.addView(proCard(L.t("تولید محتوا با هوش مصنوعی"), L.t("موضوع‌یاب، ساختار و مقاله‌ی کامل فارسی"), ContentActivity.class), UI.wrap(this, 12));
        col.addView(proCard(L.t("رتبه‌ی کلمات کلیدی"), L.t("جایگاه سایت در گوگل و پایش روزانه/هفتگی"), RankActivity.class), UI.wrap(this, 8));
        col.addView(proCard(L.t("پایش برند در هوش مصنوعی"), L.t("ببینید ChatGPT و مشابه برند شما را می‌شناسند"), MonitorActivity.class), UI.wrap(this, 8));
        col.addView(proCard(L.t("سربرگ برند و کلید API"), L.t("نام و مشخصات شما روی گزارش مشتریان"), BrandActivity.class), UI.wrap(this, 8));
        if (!seoer()) {
            LinearLayout box = UI.card(this);
            box.addView(UI.text(this, L.t("سئوکار هستید یا برای چند سایت کار می‌کنید؟"), 15, UI.INK, true));
            box.addView(UI.text(this, L.t("با فعال‌کردن «حالت سئوکار» می‌توانید سایت مشتریان را ثبت کنید، امتیازشان را دنبال کنید، بررسی خودکار تنظیم کنید و گزارش ماهانه‌ی برندشده بدهید."), 12, UI.MUTED, false), UI.wrap(this, 4));
            Button on = UI.primary(this, L.t("فعال‌سازی حالت سئوکار"));
            on.setOnClickListener(v -> setMode("seoer"));
            box.addView(on, UI.wrap(this, 10));
            col.addView(box, UI.wrap(this, 18));
            return scroller(col);
        }
        LinearLayout head = new LinearLayout(this);
        UI.rtl(head);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(UI.text(this, L.t("مشتریان من"), 18, UI.INK, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button add = UI.primary(this, L.t("＋ افزودن سایت"));
        add.setMinHeight(UI.dp(this, 40));
        add.setOnClickListener(v -> addClientDialog());
        head.addView(add, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, UI.dp(this, 42)));
        col.addView(head, UI.wrap(this, 22));
        col.addView(UI.text(this, L.t("سایت مشتریان را اضافه کنید، امتیازشان را دنبال کنید و بررسی خودکار برایشان تنظیم کنید."), 12, UI.MUTED, false), UI.wrap(this, 6));
        final LinearLayout list = UI.column(this);
        col.addView(list, UI.wrap(this, 10));
        paintClients(list);
        Api.get(this, "/api/app/clients", (j, e) -> {
            if (j != null && e == null) {
                clientsData = j;
                if (current == 3) {
                    paintClients(list);
                }
            }
        });
        Button off = UI.ghost(this, L.t("خروج از حالت سئوکار"), UI.MUTED);
        off.setOnClickListener(v -> setMode("owner"));
        col.addView(off, UI.wrap(this, 20));
        return scroller(col);
    }

    private View proCard(String t, String d, final Class<?> cls) {
        LinearLayout row = new LinearLayout(this);
        UI.rtl(row);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(UI.box(this, UI.CARD, 14, UI.LINE));
        int p = UI.dp(this, 14);
        row.setPadding(p, p, p, p);
        LinearLayout info = UI.column(this);
        info.addView(UI.text(this, t, 15, UI.INK, true));
        info.addView(UI.text(this, d, 12, UI.MUTED, false), UI.wrap(this, 2));
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(UI.text(this, "‹", 22, UI.GREEN, true));
        row.setOnClickListener(v -> startActivity(new Intent(this, cls)));
        return row;
    }

    private void paintClients(LinearLayout list) {
        list.removeAllViews();
        JSONArray cs = clientsData == null ? null : clientsData.optJSONArray("clients");
        if (cs == null) {
            list.addView(UI.text(this, L.t("در حال دریافت…"), 14, UI.MUTED, false));
            return;
        }
        if (cs.length() == 0) {
            list.addView(UI.text(this, L.t("هنوز مشتری‌ای ندارید. با «افزودن سایت» شروع کنید."), 14, UI.MUTED, false));
        }
        for (int i = 0; i < cs.length(); i++) {
            final JSONObject c = cs.optJSONObject(i);
            LinearLayout row = new LinearLayout(this);
            UI.rtl(row);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(UI.box(this, UI.CARD, 16, UI.LINE));
            int p = UI.dp(this, 14);
            row.setPadding(p, p, p, p);
            LinearLayout txt = UI.column(this);
            txt.addView(UI.text(this, c.optString("name"), 15, UI.INK, true));
            TextView host = UI.text(this, c.optString("host"), 12, UI.MUTED, false);
            host.setTextDirection(View.TEXT_DIRECTION_LTR);
            txt.addView(host, UI.wrap(this, 2));
            if (c.optInt("auto") > 0) {
                txt.addView(UI.text(this, L.t("بررسی خودکار: ") + UI.fa(String.valueOf(c.optInt("auto"))) + L.t(" فعال"), 11, UI.GREEN, true), UI.wrap(this, 4));
            }
            row.addView(txt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            if (!c.isNull("score")) {
                row.addView(UI.text(this, UI.fa(String.valueOf(c.optInt("score"))), 24, UI.scoreColor(c.optInt("score")), true));
            }
            row.setOnClickListener(v -> {
                Intent i2 = new Intent(this, ClientActivity.class);
                i2.putExtra("id", c.optInt("id"));
                startActivity(i2);
            });
            list.addView(row, UI.wrap(this, 10));
        }
    }

    private void addClientDialog() {
        LinearLayout box = UI.column(this);
        int p = UI.dp(this, 16);
        box.setPadding(p, p, p, 0);
        final EditText site = UI.input(this, L.t("آدرس سایت، مثلاً example.ir"));
        site.setTextDirection(View.TEXT_DIRECTION_LTR);
        final EditText name = UI.input(this, L.t("نام مشتری (اختیاری)"));
        box.addView(site);
        box.addView(name, UI.wrap(this, 10));
        new AlertDialog.Builder(this).setTitle(L.t("افزودن سایت مشتری")).setView(box).setNegativeButton(L.t("انصراف"), null)
                .setPositiveButton(L.t("افزودن"), (d, w) -> {
                    JSONObject body = new JSONObject();
                    try {
                        body.put("site", site.getText().toString());
                        body.put("name", name.getText().toString());
                    } catch (Exception ignored) {
                    }
                    Api.post(this, "/api/app/clients", body, (j, e) -> {
                        if (j == null || e != null) {
                            Toast.makeText(this, e == null ? L.t("خطایی رخ داد.") : e, Toast.LENGTH_LONG).show();
                            return;
                        }
                        Intent i2 = new Intent(this, ClientActivity.class);
                        i2.putExtra("id", j.optInt("id"));
                        startActivity(i2);
                    });
                }).show();
    }

    // ---------- Reports
    private String toolTitle(String type) {
        String slug = UI.slugOfType(type);
        for (int i = 0; tools != null && i < tools.length(); i++) {
            if (slug.equals(tools.optJSONObject(i).optString("slug"))) {
                return tools.optJSONObject(i).optString("title");
            }
        }
        return L.t("گزارش");
    }

    private View reportsView() {
        LinearLayout col = UI.column(this);
        col.addView(UI.text(this, L.t("گزارش‌های من"), 20, UI.INK, true));
        JSONArray rs = me == null ? null : me.optJSONArray("reports");
        for (int i = 0; rs != null && i < rs.length(); i++) {
            final JSONObject r = rs.optJSONObject(i);
            LinearLayout row = new LinearLayout(this);
            UI.rtl(row);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(UI.box(this, UI.CARD, 16, UI.LINE));
            int p = UI.dp(this, 12);
            row.setPadding(p, p, p, p);
            row.addView(UI.iconTile(this, UI.slugOfType(r.optString("type")), ReportActivity.catOf(r.optString("type")), 42));
            LinearLayout txt = UI.column(this);
            txt.addView(UI.text(this, toolTitle(r.optString("type")), 14, UI.INK, true));
            TextView host = UI.text(this, r.optString("host"), 12, UI.MUTED, false);
            host.setTextDirection(View.TEXT_DIRECTION_LTR);
            host.setSingleLine(true);
            txt.addView(host, UI.wrap(this, 2));
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tl.setMarginStart(UI.dp(this, 12));
            row.addView(txt, tl);
            if (!r.isNull("score") && r.optInt("score", -1) >= 0 && ("analyze".equals(r.optString("type")) || "ai".equals(r.optString("type")) || "speed".equals(r.optString("type")))) {
                int sc = r.optInt("score");
                row.addView(UI.text(this, UI.fa(String.valueOf(sc)), 20, UI.scoreColor(sc), true));
            }
            row.setOnClickListener(v -> {
                Intent open = new Intent(this, ReportActivity.class);
                open.putExtra("token", r.optString("token"));
                startActivity(open);
            });
            col.addView(row, UI.wrap(this, 10));
        }
        if (rs == null || rs.length() == 0) {
            col.addView(UI.text(this, me == null ? L.t("در حال دریافت…") : L.t("هنوز گزارشی ندارید. یک ابزار را اجرا کنید."), 14, UI.MUTED, false), UI.wrap(this, 12));
        }
        return scroller(col);
    }

    // ---------- Account
    private View accountView() {
        LinearLayout col = UI.column(this);
        col.addView(UI.text(this, L.t("حساب من"), 19, UI.INK, true));
        LinearLayout card = UI.card(this);
        card.addView(UI.text(this, L.t("شماره‌ی موبایل"), 12, UI.MUTED, false));
        card.addView(UI.text(this, me == null ? "…" : UI.fa(me.optString("mobile")), 17, UI.INK, true), UI.wrap(this, 2));
        card.addView(UI.text(this, L.t("اعتبار شما"), 12, UI.MUTED, false), UI.wrap(this, 14));
        card.addView(UI.text(this, me == null ? "…" : UI.fa(String.valueOf(me.optInt("credits"))), 30, UI.GREEN, true), UI.wrap(this, 2));
        col.addView(card, UI.wrap(this, 12));

        Button privacy = UI.ghost(this, L.t("حریم خصوصی و قوانین"), UI.GREEN);
        privacy.setOnClickListener(v -> {
            Intent i = new Intent(this, WebActivity.class);
            i.putExtra("url", L.fa() ? "https://3eo.ir/privacy" : "https://3eo.ir/privacy-en");
            startActivity(i);
        });
        col.addView(privacy, UI.wrap(this, 10));
        card.addView(UI.text(this, L.fa() ? "زبان" : "Language", 12, UI.MUTED, false), UI.wrap(this, 14));
        card.addView(UI.langSwitch(this), UI.wrap(this, 4));
        Button out = UI.ghost(this, L.t("خروج از حساب"), UI.INK);
        out.setOnClickListener(v -> {
            Api.logout(this);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        col.addView(out, UI.wrap(this, 10));
        TextView del = UI.text(this, L.t("حذف حساب کاربری"), 12, UI.MUTED, false);
        del.setPaintFlags(del.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        del.setGravity(android.view.Gravity.CENTER);
        del.setPadding(0, UI.dp(this, 14), 0, UI.dp(this, 14));
        del.setOnClickListener(v -> confirmDelete());
        col.addView(del, UI.wrap(this, 60));
        return scroller(col);
    }

    private void confirmDelete() {
        final EditText in = UI.input(this, L.t("حذف"));
        LinearLayout box = UI.column(this);
        int p = UI.dp(this, 20);
        box.setPadding(p, UI.dp(this, 8), p, 0);
        box.addView(in);
        new AlertDialog.Builder(this)
                .setTitle(L.t("حذف حساب کاربری"))
                .setMessage(L.t("همه‌ی اطلاعات و گزارش‌های حساب شما برای همیشه پاک می‌شود و برگشت‌پذیر نیست. برای تأیید، کلمه‌ی «حذف» را بنویسید."))
                .setView(box)
                .setNegativeButton(L.t("انصراف"), null)
                .setPositiveButton(L.t("حذف حساب"), (d, w) -> {
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
                        Toast.makeText(this, L.t("حساب شما حذف شد."), Toast.LENGTH_LONG).show();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    });
                })
                .show();
    }
}
