package ir.threeeo.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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

/** Brand letterhead for white-label client reports + API keys. */
public class BrandActivity extends ScreenActivity {
    private EditText name, color, site, phone, email, address;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        titleView.setText("سربرگ برند و API");
        load();
    }

    private void load() {
        Api.get(this, "/api/app/brand", (j, e) -> {
            if (j == null || e != null) {
                content.removeAllViews();
                content.addView(UI.text(this, e == null ? "خطا در دریافت." : e, 15, UI.RED, false));
                return;
            }
            render(j);
        });
    }

    private EditText field(LinearLayout card, String hint, String val, boolean ltr) {
        EditText f = UI.input(this, hint);
        f.setText(val);
        if (ltr) {
            f.setTextDirection(View.TEXT_DIRECTION_LTR);
        }
        card.addView(f, UI.wrap(this, 8));
        return f;
    }

    private void render(JSONObject j) {
        content.removeAllViews();
        JSONObject br = j.optJSONObject("brand");
        content.addView(UI.text(this, "گزارش‌هایی که برای مشتریان می‌سازید با نام و مشخصات شما نمایش داده می‌شود.", 12, UI.MUTED, false));
        LinearLayout card = UI.card(this);
        name = field(card, "نام برند یا شرکت شما", br.optString("name"), false);
        color = field(card, "رنگ برند (مثل #047857)", br.optString("color"), true);
        site = field(card, "وب‌سایت", br.optString("site"), true);
        phone = field(card, "تلفن", br.optString("phone"), true);
        email = field(card, "ایمیل", br.optString("email"), true);
        address = field(card, "نشانی", br.optString("address"), false);
        Button save = UI.primary(this, "ذخیره‌ی سربرگ");
        save.setOnClickListener(v -> {
            JSONObject b = new JSONObject();
            try {
                b.put("name", name.getText().toString());
                b.put("color", color.getText().toString());
                b.put("site", site.getText().toString());
                b.put("phone", phone.getText().toString());
                b.put("email", email.getText().toString());
                b.put("address", address.getText().toString());
            } catch (Exception ignored) {
            }
            Api.post(this, "/api/app/brand", b, (r, e) -> Toast.makeText(this, e != null ? e : "ذخیره شد ✓", Toast.LENGTH_LONG).show());
        });
        card.addView(save, UI.wrap(this, 12));
        content.addView(card, UI.wrap(this, 10));
        content.addView(UI.text(this, "لوگو را فعلاً می‌توانید از 3eo.ir/dashboard/brand بارگذاری کنید.", 11, UI.MUTED, false), UI.wrap(this, 4));

        content.addView(UI.text(this, "کلید API", 17, UI.INK, true), UI.wrap(this, 22));
        content.addView(UI.text(this, "برای اتصال ابزارهای خودتان به سرویس. کلید فقط یک بار کامل نشان داده می‌شود.", 12, UI.MUTED, false), UI.wrap(this, 3));
        final LinearLayout keys = UI.column(this);
        content.addView(keys, UI.wrap(this, 6));
        loadKeys(keys);
    }

    private void loadKeys(final LinearLayout keys) {
        Api.get(this, "/api/app/keys", (j, e) -> {
            keys.removeAllViews();
            if (j == null || e != null) {
                return;
            }
            JSONArray ks = j.optJSONArray("keys");
            for (int i = 0; ks != null && i < ks.length(); i++) {
                final JSONObject k = ks.optJSONObject(i);
                LinearLayout row = new LinearLayout(this);
                UI.rtl(row);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setBackground(UI.box(this, UI.CARD, 14, UI.LINE));
                int p = UI.dp(this, 12);
                row.setPadding(p, p, p, p);
                String t = k.optString("label").isEmpty() ? "کلید" : k.optString("label");
                row.addView(UI.text(this, UI.bidi(t) + "  …" + k.optString("hint"), 13, UI.INK, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                TextView_revoke(row, k, keys);
                keys.addView(row, UI.wrap(this, 8));
            }
            if (ks == null || ks.length() < j.optInt("max")) {
                Button add = UI.ghost(this, "＋ ساخت کلید جدید", UI.GREEN);
                add.setOnClickListener(v -> {
                    JSONObject b = new JSONObject();
                    Api.post(this, "/api/app/keys", b, (r, er) -> {
                        if (r == null || er != null) {
                            Toast.makeText(this, er == null ? "خطا" : er, Toast.LENGTH_LONG).show();
                            return;
                        }
                        final String key = r.optString("key");
                        TextViewKey(key);
                        loadKeys(keys);
                    });
                });
                keys.addView(add, UI.wrap(this, 10));
            }
        });
    }

    private void TextView_revoke(LinearLayout row, final JSONObject k, final LinearLayout keys) {
        android.widget.TextView t = UI.text(this, "ابطال", 13, UI.RED, true);
        t.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("ابطال کلید").setMessage("برنامه‌هایی که از این کلید استفاده می‌کنند از کار می‌افتند.")
                .setNegativeButton("انصراف", null)
                .setPositiveButton("ابطال", (d, w) -> Api.post(this, "/api/app/keys/" + k.optInt("id") + "/revoke", new JSONObject(), (r, e) -> loadKeys(keys))).show());
        row.addView(t);
    }

    private void TextViewKey(final String key) {
        EditText e = UI.input(this, "");
        e.setText(key);
        e.setTextDirection(View.TEXT_DIRECTION_LTR);
        e.setTextIsSelectable(true);
        new AlertDialog.Builder(this).setTitle("کلید شما (فقط همین حالا کامل دیده می‌شود)").setView(e)
                .setPositiveButton("کپی", (d, w) -> {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("key", key));
                    Toast.makeText(this, "کپی شد ✓", Toast.LENGTH_SHORT).show();
                }).setNegativeButton("بستن", null).show();
    }
}
