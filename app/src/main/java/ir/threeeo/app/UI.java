package ir.threeeo.app;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small helpers to build the native screens in code (colors, cards, buttons, RTL). */
final class UI {
    static final int GREEN = 0xFF047857, GREEN_DARK = 0xFF065F46, GREEN_SOFT = 0xFFD1FAE5, INK = 0xFF0F172A, MUTED = 0xFF64748B,
            BG = 0xFFF5FAF7, CARD = 0xFFFFFFFF, LINE = 0xFFE2E8F0, RED = 0xFFDC2626, AMBER = 0xFFD97706;

    private UI() {
    }

    static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable box(Context c, int fill, int radiusDp, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        if (stroke != 0) {
            g.setStroke(dp(c, 1), stroke);
        }
        return g;
    }

    static void rtl(View v) {
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    }

    static TextView text(Context c, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setLineSpacing(0, 1.25f);
        t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        if (bold) {
            t.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return t;
    }

    static LinearLayout.LayoutParams lp(Context c, int w, int h, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.topMargin = dp(c, top);
        return p;
    }

    static LinearLayout.LayoutParams wrap(Context c, int top) {
        return lp(c, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, top);
    }

    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        rtl(l);
        return l;
    }

    static LinearLayout card(Context c) {
        LinearLayout l = column(c);
        l.setBackground(box(c, CARD, 16, LINE));
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        return l;
    }

    static Button primary(Context c, String s) {
        Button b = new Button(c);
        b.setText(s);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setTextColor(0xFFFFFFFF);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(box(c, GREEN, 12, 0));
        b.setStateListAnimator(null);
        b.setMinHeight(dp(c, 48));
        return b;
    }

    static Button ghost(Context c, String s, int color) {
        Button b = new Button(c);
        b.setText(s);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTextColor(color);
        b.setBackground(box(c, 0x00000000, 12, color));
        b.setStateListAnimator(null);
        b.setMinHeight(dp(c, 44));
        return b;
    }

    static EditText input(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setTextSize(16);
        e.setSingleLine(true);
        e.setTextColor(INK);
        e.setHintTextColor(0xFF94A3B8);
        e.setBackground(box(c, 0xFFF8FAFC, 12, LINE));
        int p = dp(c, 14);
        e.setPadding(p, p, p, p);
        e.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        e.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        return e;
    }

    static int statusColor(String st) {
        return "pass".equals(st) ? GREEN : ("warn".equals(st) ? AMBER : RED);
    }

    static int scoreColor(int score) {
        return score >= 80 ? GREEN : (score >= 50 ? AMBER : RED);
    }

    /** western digits to Persian digits */
    static String fa(String s) {
        StringBuilder b = new StringBuilder();
        for (char ch : s.toCharArray()) {
            b.append(ch >= '0' && ch <= '9' ? (char) ('۰' + (ch - '0')) : ch);
        }
        return b.toString();
    }
}
