package ir.threeeo.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

/** Sign in with the mobile number and the SMS code. */
public class LoginActivity extends AppCompatActivity {
    private LinearLayout box;
    private String mobile = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(UI.BG);
        sv.setFillViewport(true);
        UI.rtl(sv);
        LinearLayout root = UI.column(this);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int p = UI.dp(this, 24);
        root.setPadding(p, UI.dp(this, 56), p, p);
        sv.addView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo);
        logo.setAdjustViewBounds(true);
        root.addView(logo, new LinearLayout.LayoutParams(UI.dp(this, 190), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView t = UI.text(this, "ابزارهای سئو، همیشه همراه شما", 15, UI.MUTED, false);
        t.setGravity(Gravity.CENTER);
        root.addView(t, UI.wrap(this, 6));

        box = UI.card(this);
        root.addView(box, UI.wrap(this, 28));
        stepMobile(null);
        setContentView(sv);
    }

    private void stepMobile(String error) {
        box.removeAllViews();
        box.addView(UI.text(this, "ورود یا ثبت‌نام", 18, UI.INK, true));
        box.addView(UI.text(this, "شماره موبایل خود را وارد کنید تا کد تأیید برایتان پیامک شود.", 14, UI.MUTED, false), UI.wrap(this, 6));
        final EditText in = UI.input(this, "مثلاً ۰۹۱۲۳۴۵۶۷۸۹");
        in.setInputType(InputType.TYPE_CLASS_PHONE);
        in.setText(mobile);
        box.addView(in, UI.wrap(this, 16));
        final TextView err = UI.text(this, error == null ? "" : error, 13, UI.RED, false);
        err.setVisibility(error == null ? View.GONE : View.VISIBLE);
        box.addView(err, UI.wrap(this, 8));
        final Button go = UI.primary(this, "دریافت کد تأیید");
        box.addView(go, UI.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 14));
        go.setOnClickListener(v -> {
            mobile = in.getText().toString().trim();
            if (mobile.length() < 10) {
                err.setText("شماره موبایل را کامل وارد کنید.");
                err.setVisibility(View.VISIBLE);
                return;
            }
            go.setEnabled(false);
            go.setText("در حال ارسال…");
            JSONObject body = new JSONObject();
            try {
                body.put("mobile", mobile);
            } catch (Exception ignored) {
            }
            Api.post(this, "/api/app/login/send", body, (j, e) -> {
                if (e != null) {
                    go.setEnabled(true);
                    go.setText("دریافت کد تأیید");
                    err.setText(e);
                    err.setVisibility(View.VISIBLE);
                } else {
                    stepCode(null);
                }
            });
        });
        box.addView(UI.text(this, "با ورود، قوانین و حریم خصوصی 3eo.ir را می‌پذیرید.", 12, UI.MUTED, false), UI.wrap(this, 14));
    }

    private void stepCode(String error) {
        box.removeAllViews();
        box.addView(UI.text(this, "کد تأیید", 18, UI.INK, true));
        box.addView(UI.text(this, "کد ۶ رقمی ارسال‌شده به " + UI.fa(mobile) + " را وارد کنید.", 14, UI.MUTED, false), UI.wrap(this, 6));
        final EditText in = UI.input(this, "کد ۶ رقمی");
        in.setInputType(InputType.TYPE_CLASS_NUMBER);
        in.setGravity(Gravity.CENTER);
        in.setTextSize(22);
        box.addView(in, UI.wrap(this, 16));
        final TextView err = UI.text(this, error == null ? "" : error, 13, UI.RED, false);
        err.setVisibility(error == null ? View.GONE : View.VISIBLE);
        box.addView(err, UI.wrap(this, 8));
        final Button go = UI.primary(this, "ورود");
        box.addView(go, UI.wrap(this, 14));
        go.setOnClickListener(v -> {
            String code = in.getText().toString().trim();
            if (code.length() < 6) {
                err.setText("کد ۶ رقمی را کامل وارد کنید.");
                err.setVisibility(View.VISIBLE);
                return;
            }
            go.setEnabled(false);
            go.setText("در حال ورود…");
            JSONObject body = new JSONObject();
            try {
                body.put("mobile", mobile);
                body.put("code", code);
            } catch (Exception ignored) {
            }
            Api.post(this, "/api/app/login/verify", body, (j, e) -> {
                if (e != null || j == null) {
                    go.setEnabled(true);
                    go.setText("ورود");
                    err.setText(e == null ? "خطایی رخ داد." : e);
                    err.setVisibility(View.VISIBLE);
                    return;
                }
                Api.setToken(this, j.optString("token"));
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
        });
        Button back = UI.ghost(this, "ویرایش شماره", UI.GREEN);
        box.addView(back, UI.wrap(this, 10));
        back.setOnClickListener(v -> stepMobile(null));
        in.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(in, 0);
        }
    }
}
