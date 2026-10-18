package com.maxtunnel.app.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.LinearInterpolator;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LinearOutView extends View {
    public List<Float> A;
    public int B = 200;
    public float I = 1.0f;
    public float N1 = 0.0f;
    public float O1 = 0.0f;
    public float P1 = 1000000.0f;
    public int Q1 = 1;
    public float T = 0.0f;
    public int a = 10;
    public int b = Color.parseColor("#FF5252"); // أحمر
    public int c;
    public RectF fRect;
    public Paint h;
    public Queue<Float> s;
    public List<Float> t;

    public class a implements ValueAnimator.AnimatorUpdateListener {
        public a() {}
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            LinearOutView.this.I = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            LinearOutView.this.invalidate();
        }
    }

    public LinearOutView(Context context) { super(context); init(); }
    public LinearOutView(Context context, AttributeSet attributeSet) { super(context, attributeSet); init(); }

    private void init() {
        this.c = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.5f, getResources().getDisplayMetrics());
        this.h = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.h.setColor(this.b);
        this.h.setStyle(Paint.Style.STROKE);
        this.h.setStrokeCap(Paint.Cap.ROUND);
        this.h.setStrokeWidth(this.c);
        l();
    }

    public void b(float f) {
        if (this.Q1 == 0 && (f < this.O1 || f > this.P1)) {
            f = Math.max(this.O1, Math.min(this.P1, f));
        }
        this.t = new ArrayList<>(this.s);
        if (this.s.size() == this.a) this.s.poll();
        this.s.add(f);
        this.A = new ArrayList<>(this.s);
        if (this.Q1 == 1) e();
        o();
    }

    public final Path c() {
        Path path = new Path();
        if (this.fRect == null) return path;
        
        float f3 = this.fRect.left;
        float f2 = this.fRect.bottom;
        for (int i2 = 0; i2 < this.A.size(); i2++) {
            float fFloatValue = this.t.get(i2);
            float fFloatValue2 = fFloatValue + ((this.A.get(i2) - fFloatValue) * this.I);
            float f4 = this.fRect.left + (this.T * i2);
            float f5 = this.fRect.bottom - ((fFloatValue2 - this.O1) * this.N1);
            if (i2 != 0) {
                float f6 = f3 + ((f4 - f3) * 0.5f);
                path.cubicTo(f6, f2, f6, f5, f4, f5);
            } else {
                path.moveTo(f4, f5);
            }
            f2 = f5;
            f3 = f4;
        }
        return path;
    }

    public final void d(int i, int i2) {
        this.fRect = new RectF((this.c * 2) + getPaddingLeft(), (this.c * 2) + getPaddingTop(), (i - getPaddingRight()) - this.c, (i2 - getPaddingBottom()) - this.c);
    }

    public final void e() {
        if (this.fRect == null) {
            this.N1 = 0.0f; this.T = 0.0f; return;
        }
        if (this.Q1 == 1) {
            for (int i = 0; i < this.A.size(); i++) {
                float f1 = this.t.get(i);
                float f2 = this.A.get(i);
                if (i == 0) {
                    this.O1 = Math.min(f2, f1);
                    this.P1 = Math.max(f2, f1);
                } else {
                    this.O1 = Math.min(this.O1, Math.min(f2, f1));
                    this.P1 = Math.max(this.P1, Math.max(f2, f1));
                }
            }
        }
        this.T = this.fRect.width() / (this.a - 1);
        float diff = this.P1 - this.O1;
        this.N1 = (diff == 0) ? 0 : this.fRect.height() / diff;
    }

    public void f() { l(); invalidate(); }

    public final void l() {
        this.s = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < this.a; i++) this.s.add(this.O1);
        this.t = new ArrayList<>(this.s);
        this.A = new ArrayList<>(this.s);
    }

    public final void o() {
        ValueAnimator anim = ValueAnimator.ofFloat(0.0f, 1.0f);
        anim.addUpdateListener(new a());
        anim.setDuration(this.B);
        anim.setInterpolator(new LinearInterpolator());
        anim.start();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.s == null || this.s.isEmpty()) return;
        canvas.drawPath(c(), this.h); 
    }

    @Override
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        d(i, i2); e();
    }
}
