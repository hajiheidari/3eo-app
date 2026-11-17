package ir.threeeo.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/** Circular score gauge (0-100). */
final class ScoreView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint num = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sub = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int score;

    ScoreView(Context c, int score) {
        super(c);
        this.score = score;
        float d = c.getResources().getDisplayMetrics().density;
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(12 * d);
        track.setColor(UI.LINE);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(12 * d);
        arc.setStrokeCap(Paint.Cap.ROUND);
        arc.setColor(UI.scoreColor(score));
        num.setColor(UI.INK);
        num.setTextAlign(Paint.Align.CENTER);
        num.setTypeface(Typeface.DEFAULT_BOLD);
        num.setTextSize(40 * d);
        sub.setColor(UI.MUTED);
        sub.setTextAlign(Paint.Align.CENTER);
        sub.setTextSize(13 * d);
    }

    @Override
    protected void onDraw(Canvas c) {
        float d = getResources().getDisplayMetrics().density;
        float pad = 10 * d;
        float size = Math.min(getWidth(), getHeight()) - 2 * pad;
        float left = (getWidth() - size) / 2f, top = (getHeight() - size) / 2f;
        RectF r = new RectF(left, top, left + size, top + size);
        c.drawArc(r, 0, 360, false, track);
        c.drawArc(r, -90, 360f * score / 100f, false, arc);
        c.drawText(UI.fa(String.valueOf(score)), getWidth() / 2f, getHeight() / 2f + 10 * d, num);
        c.drawText(L.t("از ۱۰۰"), getWidth() / 2f, getHeight() / 2f + 34 * d, sub);
    }
}
