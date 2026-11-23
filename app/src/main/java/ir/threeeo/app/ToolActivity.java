package ir.threeeo.app;

import android.os.Bundle;
import android.text.InputType;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One tool: native form built from the server's field spec, result drawn natively below it. */
public class ToolActivity extends ScreenActivity {
    private JSONObject tool;
    private final Map<String, View> inputs = new HashMap<>();
    private final Map<String, List<String>> spinnerValues = new HashMap<>();
    private final Map<String, Set<String>> multi = new HashMap<>();
    private LinearLayout result;
    private Button run;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            tool = new JSONObject(getIntent().getStringExtra("tool"));
        } catch (Exception e) {
            finish();
            return;
        }
        titleView.setText("");
        String slug = tool.optString("slug");
        String cat = tool.optString("cat");

        LinearLayout hero = UI.column(this);
        int col = UI.catColor(cat);
        hero.setBackground(UI.gradient(this, col, UI.darker(col, 0.7f), 20));
        int hp = UI.dp(this, 18);
        hero.setPadding(hp, hp, hp, hp);
        LinearLayout row = new LinearLayout(this);
        UI.rtl(row);
        row.setGravity(Gravity.CENTER_VERTICAL);
        android.widget.ImageView iv = new android.widget.ImageView(this);
        int id = getResources().getIdentifier("ic_" + slug, "drawable", getPackageName());
        iv.setImageResource(id);
        iv.setBackground(UI.box(this, 0x33FFFFFF, 14, 0));
        iv.setPadding(UI.dp(this, 10), UI.dp(this, 10), UI.dp(this, 10), UI.dp(this, 10));
        row.addView(iv, new LinearLayout.LayoutParams(UI.dp(this, 52), UI.dp(this, 52)));
        TextView t = UI.text(this, tool.optString("title"), 17, 0xFFFFFFFF, true);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tl.setMarginStart(UI.dp(this, 12));
        row.addView(t, tl);
        hero.addView(row);
        hero.addView(UI.text(this, tool.optString("desc"), 13, 0xE6FFFFFF, false), UI.wrap(this, 10));
        content.addView(hero);
        String costLabel = tool.optString("cost");
        if (!costLabel.isEmpty()) {
            boolean paid = tool.optBoolean("paid");
            TextView cl = UI.text(this, (paid ? L.t("هزینه: ") : L.t("هزینه: ")) + costLabel, 13, paid ? UI.AMBER : UI.GREEN, true);
            cl.setBackground(UI.box(this, paid ? 0x1AD97706 : 0x1A047857, 12, 0));
            cl.setPadding(UI.dp(this, 12), UI.dp(this, 8), UI.dp(this, 12), UI.dp(this, 8));
            content.addView(cl, UI.wrap(this, 10));
        }

        LinearLayout form = UI.card(this);
        JSONArray fields = tool.optJSONArray("fields");
        for (int i = 0; fields != null && i < fields.length(); i++) {
            addField(form, fields.optJSONObject(i));
        }
        content.addView(form, UI.wrap(this, 14));

        run = UI.primary(this, L.t("اجرا"));
        run.setBackground(UI.gradient(this, col, UI.darker(col, 0.75f), 12));
        run.setOnClickListener(v -> run());
        content.addView(run, UI.wrap(this, 12));
        result = UI.column(this);
        content.addView(result, UI.wrap(this, 6));
    }

    private void addField(LinearLayout form, JSONObject f) {
        String key = f.optString("key"), type = f.optString("type");
        TextView label = UI.text(this, f.optString("label"), 13, UI.MUTED, true);
        form.addView(label, UI.wrap(this, form.getChildCount() == 0 ? 0 : 14));
        if ("select".equals(type)) {
            JSONArray o = f.optJSONArray("options");
            List<String> names = new ArrayList<>(), vals = new ArrayList<>();
            for (int i = 0; o != null && i < o.length(); i++) {
                vals.add(o.optJSONArray(i).optString(0));
                names.add(o.optJSONArray(i).optString(1));
            }
            Spinner sp = new Spinner(this);
            sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
            sp.setBackground(UI.box(this, 0xFFF8FAFC, 12, UI.LINE));
            int di = vals.indexOf(f.optString("def"));
            sp.setSelection(Math.max(di, 0));
            spinnerValues.put(key, vals);
            inputs.put(key, sp);
            form.addView(sp, UI.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(this, 48), 6));
        } else if ("multi".equals(type)) {
            Set<String> sel = new HashSet<>();
            for (String d : f.optString("def").split(",")) {
                if (!d.isEmpty()) {
                    sel.add(d);
                }
            }
            multi.put(key, sel);
            JSONArray o = f.optJSONArray("options");
            LinearLayout wrap = UI.column(this);
            LinearLayout line = null;
            for (int i = 0; o != null && i < o.length(); i++) {
                final String val = o.optJSONArray(i).optString(0);
                final TextView chip = UI.text(this, o.optJSONArray(i).optString(1), 13, UI.INK, false);
                chip.setPadding(UI.dp(this, 12), UI.dp(this, 7), UI.dp(this, 12), UI.dp(this, 7));
                paintChip(chip, sel.contains(val));
                chip.setOnClickListener(v -> {
                    if (!sel.remove(val)) {
                        sel.add(val);
                    }
                    paintChip(chip, sel.contains(val));
                });
                if (i % 3 == 0) {
                    line = new LinearLayout(this);
                    UI.rtl(line);
                    wrap.addView(line, UI.wrap(this, 6));
                }
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMarginEnd(UI.dp(this, 6));
                line.addView(chip, lp);
            }
            form.addView(wrap, UI.wrap(this, 4));
        } else {
            EditText e = UI.input(this, f.optString("hint"));
            if ("textarea".equals(type)) {
                e.setSingleLine(false);
                e.setMinLines(4);
                e.setGravity(Gravity.TOP | Gravity.START);
                e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            } else if ("url".equals(type)) {
                e.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
                e.setTextDirection(View.TEXT_DIRECTION_LTR);
            }
            inputs.put(key, e);
            form.addView(e, UI.wrap(this, 6));
        }
    }

    private void paintChip(TextView t, boolean on) {
        t.setBackground(UI.box(this, on ? UI.GREEN : 0xFFF1F5F9, 16, 0));
        t.setTextColor(on ? 0xFFFFFFFF : UI.INK);
    }

    private void run() {
        JSONObject body = new JSONObject();
        try {
            for (Map.Entry<String, View> en : inputs.entrySet()) {
                if (en.getValue() instanceof EditText) {
                    body.put(en.getKey(), ((EditText) en.getValue()).getText().toString().trim());
                } else if (en.getValue() instanceof Spinner) {
                    body.put(en.getKey(), spinnerValues.get(en.getKey()).get(((Spinner) en.getValue()).getSelectedItemPosition()));
                }
            }
            for (Map.Entry<String, Set<String>> en : multi.entrySet()) {
                body.put(en.getKey(), new JSONArray(en.getValue()));
            }
        } catch (Exception ignored) {
        }
        run.setEnabled(false);
        run.setText(L.t("در حال انجام… (تا ۴۵ ثانیه)"));
        result.removeAllViews();
        Api.post(this, "/api/app/tool/" + tool.optString("slug"), body, (j, e) -> {
            run.setEnabled(true);
            run.setText(L.t("اجرا"));
            if (j == null || e != null) {
                if (Api.token(this) == null) {
                    finish();
                    return;
                }
                Toast.makeText(this, e == null ? L.t("خطایی رخ داد.") : e, Toast.LENGTH_LONG).show();
                return;
            }
            if (!j.optString("share").isEmpty()) {
                shareText = L.shareText(j.optString("title"), j.optString("host"), j.optString("share"));
            }
            result.addView(UI.text(this, L.t("نتیجه"), 18, UI.INK, true));
            Blocks.render(this, result, j.optJSONArray("blocks"));
            final View r = result;
            r.post(() -> ((android.widget.ScrollView) content.getParent()).smoothScrollTo(0, r.getTop()));
        });
    }
}
