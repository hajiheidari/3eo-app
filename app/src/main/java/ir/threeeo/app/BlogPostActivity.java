package ir.threeeo.app;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.List;

/** One blog article rendered natively. */
public class BlogPostActivity extends ScreenActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        titleView.setText(L.t("وبلاگ"));
        JSONObject p = Blog.current;
        if (p == null) {
            finish();
            return;
        }
        String title = Blog.text(p.optJSONObject("title") == null ? "" : p.optJSONObject("title").optString("rendered"));
        String th = Blog.thumb(p);
        if (!th.isEmpty()) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setBackground(UI.box(this, UI.GREEN_SOFT, 14, UI.LINE));
            content.addView(iv, UI.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(this, 190), 0));
            Blog.image(iv, th);
        }
        content.addView(UI.text(this, UI.bidi(title), 22, UI.INK, true), UI.wrap(this, 12));
        String d = p.optString("date");
        content.addView(UI.text(this, UI.fa(d.length() >= 10 ? d.substring(0, 10) : d), 12, UI.MUTED, false), UI.wrap(this, 4));
        StringBuilder share = new StringBuilder(title).append("\n").append(p.optString("link"));
        shareText = share.toString();
        String html = p.optJSONObject("content") == null ? "" : p.optJSONObject("content").optString("rendered");
        List<String[]> bl = Blog.blocks(html);
        for (String[] x : bl) {
            if ("img".equals(x[0])) {
                ImageView iv = new ImageView(this);
                iv.setAdjustViewBounds(true);
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                content.addView(iv, UI.lp(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 12));
                Blog.image(iv, x[1]);
            } else if ("h".equals(x[0])) {
                content.addView(UI.text(this, UI.bidi(x[1]), 18, UI.INK, true), UI.wrap(this, 18));
            } else {
                TextView t = UI.text(this, ("li".equals(x[0]) ? "• " : "") + UI.bidi(x[1]), 15, UI.INK, false);
                t.setLineSpacing(UI.dp(this, 4), 1f);
                t.setTextIsSelectable(true);
                content.addView(t, UI.wrap(this, 10));
            }
        }
    }
}
