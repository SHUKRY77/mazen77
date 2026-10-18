package com.maxtunnel.app.view;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class CircleProgressBar extends View {
    public float a;
    public float b;
    public int c;
    public int d;
    public int f;
    public int h;
    public RectF q;
    public Paint s;
    public Paint t;

    public CircleProgressBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        // 🌟 تحديد سُمك الدائرة ليكون 9dp تماماً مثل التطبيق الأصلي
        this.a = context.getResources().getDisplayMetrics().density * 9;
        this.b = 0.0f;
        this.c = 0;
        this.d = 100;
        this.f = -90;
        this.h = Color.parseColor("#E53935"); // اللون الأحمر الافتراضي
        b(context, attributeSet);
    }

    public int a(int i, float f) {
        return Color.argb(Math.round(Color.alpha(i) * f), Color.red(i), Color.green(i), Color.blue(i));
    }

    public final void b(Context context, AttributeSet attributeSet) {
        this.q = new RectF();
        
        Paint paint = new Paint(1);
        this.s = paint;
        // 🌟 هذا السطر هو الذي يصنع اللون الوردي الفاتح (شفافية 30% من اللون الأساسي)
        paint.setColor(a(this.h, 0.3f)); 
        Paint paint2 = this.s;
        Paint.Style style = Paint.Style.STROKE;
        paint2.setStyle(style);
        this.s.setStrokeWidth(this.a);
        
        Paint paint3 = new Paint(1);
        this.t = paint3;
        paint3.setColor(this.h);
        this.t.setStyle(style);
        this.t.setStrokeWidth(this.a);
        this.t.setStrokeCap(Paint.Cap.ROUND);
    }

    public void c(float f, int i) {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this, "progress", f);
        ofFloat.setDuration(i);
        ofFloat.setInterpolator(new DecelerateInterpolator());
        ofFloat.start();
    }

    public int getColor() { return this.h; }
    public int getMax() { return this.d; }
    public int getMin() { return this.c; }
    public float getProgress() { return this.b; }
    public float getStrokeWidth() { return this.a; }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawOval(this.q, this.s);
        canvas.drawArc(this.q, this.f, (this.b * 360.0f) / this.d, false, this.t);
    }

    @Override
    public void onMeasure(int i, int i2) {
        int min = Math.min(View.getDefaultSize(getSuggestedMinimumWidth(), i), View.getDefaultSize(getSuggestedMinimumHeight(), i2));
        setMeasuredDimension(min, min);
        RectF rectF = this.q;
        float f = this.a;
        float f2 = min;
        rectF.set((f / 2.0f) + 0.0f, (f / 2.0f) + 0.0f, f2 - (f / 2.0f), f2 - (f / 2.0f));
    }

    public void setColor(int i) {
        this.h = i;
        this.s.setColor(a(i, 0.3f));
        this.t.setColor(i);
        invalidate();
        requestLayout();
    }

    public void setMax(int i) { this.d = i; invalidate(); }
    public void setMin(int i) { this.c = i; invalidate(); }
    public void setProgress(float f) { this.b = f; invalidate(); }

    public void setProgressWithAnimation(float f) {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this, "progress", f);
        ofFloat.setDuration(1500L);
        ofFloat.setInterpolator(new DecelerateInterpolator());
        ofFloat.start();
    }

    public void setStrokeWidth(float f) {
        this.a = f;
        this.s.setStrokeWidth(f);
        this.t.setStrokeWidth(f);
        invalidate();
        requestLayout();
    }
}
