package ir.threeeo.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Base for inner screens: top bar with back + optional share, scrolling content column. */
abstract class ScreenActivity extends AppCompatActivity {
    protected LinearLayout content;
    protected TextView titleView;
    protected String shareText = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = UI.column(this);
        root.setBackgroundColor(UI.BG);
        LinearLayout bar = new LinearLayout(this);
        UI.rtl(bar);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(UI.CARD);
        int p = UI.dp(this, 14);
        bar.setPadding(p, UI.dp(this, 12), p, UI.dp(this, 12));
        TextView back = UI.text(this, "‹  بازگشت", 15, UI.GREEN, true);
        back.setOnClickListener(v -> finish());
        bar.addView(back);
        titleView = UI.text(this, "", 16, UI.INK, true);
        titleView.setGravity(Gravity.CENTER);
        titleView.setSingleLine(true);
        bar.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView share = UI.text(this, "اشتراک", 15, UI.GREEN, true);
        share.setOnClickListener(v -> {
            if (shareText.isEmpty()) {
                return;
            }
            Intent s = new Intent(Intent.ACTION_SEND);
            s.setType("text/plain");
            s.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(s, "اشتراک‌گذاری"));
        });
        bar.addView(share);
        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ScrollView sv = new ScrollView(this);
        UI.rtl(sv);
        sv.setFillViewport(false);
        content = UI.column(this);
        content.setPadding(p, p, p, UI.dp(this, 36));
        sv.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }
}
