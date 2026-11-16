package ir.threeeo.app;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

/** Native view of any saved or fresh report (blocks rendered by Blocks). */
public class ReportActivity extends ScreenActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String json = getIntent().getStringExtra("json");
        if (json != null) {
            try {
                render(new JSONObject(json));
                return;
            } catch (Exception ignored) {
            }
        }
        content.addView(UI.text(this, "در حال دریافت گزارش…", 15, UI.MUTED, false));
        Api.get(this, "/api/app/report/" + getIntent().getStringExtra("token"), (j, e) -> {
            content.removeAllViews();
            if (e != null || j == null) {
                content.addView(UI.text(this, e == null ? "گزارش پیدا نشد." : e, 15, UI.RED, false));
            } else {
                render(j);
            }
        });
    }

    private void render(JSONObject j) {
        content.removeAllViews();
        titleView.setText(j.optString("title"));
        String host = j.optString("host");
        shareText = j.optString("share").isEmpty() ? "" : "گزارش " + j.optString("title") + " " + host + "\n" + j.optString("share");
        LinearLayout head = new LinearLayout(this);
        UI.rtl(head);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(UI.iconTile(this, UI.slugOfType(j.optString("type")), catOf(j.optString("type")), 44));
        LinearLayout t = UI.column(this);
        t.addView(UI.text(this, j.optString("title"), 17, UI.INK, true));
        TextView h = UI.text(this, host, 13, UI.MUTED, false);
        h.setTextDirection(android.view.View.TEXT_DIRECTION_LTR);
        t.addView(h);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMarginStart(UI.dp(this, 12));
        head.addView(t, lp);
        content.addView(head);
        Blocks.render(this, content, j.optJSONArray("blocks"));
    }

    static String catOf(String type) {
        switch (type) {
            case "ai": case "aimon": return "ai";
            case "cluster": case "suggest": case "density": case "pinkw": return "keyword";
            case "ideas": case "metagen": case "headings": return "content";
            case "authority": return "data";
            default: return "tech";
        }
    }
}
