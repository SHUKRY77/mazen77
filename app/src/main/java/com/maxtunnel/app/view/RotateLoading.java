package com.maxtunnel.app.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class RotateLoading extends View {
    public RectF A;
    public RectF B;
    public int I;
    public float N1;
    public boolean O1;
    public int T;
    public int f;
    public int h;
    public int q;
    public boolean s;
    public final Paint t;

    public class a extends AnimatorListenerAdapter {
        public a() {}

        @Override
        public void onAnimationEnd(Animator animator) {
            super.onAnimationEnd(animator);
            RotateLoading.this.s = false;
            // 🌟 إخفاء الدائرة فور انتهاء حركة الإغلاق لتجنب البقاء باللون الخطأ
            setVisibility(INVISIBLE);
        }
    }

    public RotateLoading(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public RotateLoading(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        // 🌟 جعل سمك دائرة التحميل متناسقاً
        this.h = b(4); // سمك الدائرة المتحركة (4dp)
        this.q = b(2);
        this.s = false; // تبدأ مخفية
        this.t = new Paint();
        this.I = 10;
        this.T = 190;
        this.O1 = true;
        this.f = Color.parseColor("#E53935"); // لون مبدئي، سيتم تغييره من MainActivity
        c();
    }

    public final int b(int i) {
        return (int) TypedValue.applyDimension(1, i, getResources().getDisplayMetrics());
    }

    public final void c() {
        this.t.setColor(this.f);
        this.t.setAntiAlias(true);
        this.t.setStyle(Paint.Style.STROKE);
        this.t.setStrokeWidth(this.h);
        this.t.setStrokeCap(Paint.Cap.ROUND);
    }

    public boolean d() {
        return this.s;
    }

    public void f() {
        setVisibility(VISIBLE); // تأكيد الظهور
        g();
        this.s = true;
        invalidate();
    }

    public final void g() {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this, "scaleX", 0.0f, 1.0f);
        ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(this, "scaleY", 0.0f, 1.0f);
        ofFloat.setDuration(500L);
        ofFloat2.setDuration(500L);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(ofFloat, ofFloat2);
        animatorSet.start();
    }

    public void h() {
        i();
        invalidate();
    }

    public final void i() {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this, "scaleX", 1.0f, 0.0f);
        ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(this, "scaleY", 1.0f, 0.0f);
        ofFloat.setDuration(500L);
        ofFloat2.setDuration(500L);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(ofFloat, ofFloat2);
        animatorSet.addListener(new a());
        animatorSet.start();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.s) {
            Paint paint = this.t;
            paint.setColor(Color.parseColor("#33000000")); // ظل خفيف جداً
            canvas.drawArc(this.B, this.I, this.N1, false, this.t);
            canvas.drawArc(this.B, this.T, this.N1, false, this.t);
            this.t.setColor(this.f); // اللون الفعلي من الإعدادات
            canvas.drawArc(this.A, this.I, this.N1, false, this.t);
            canvas.drawArc(this.A, this.T, this.N1, false, this.t);
            try {
                Thread.sleep(2L); // سرعة الدوران
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int i = this.I;
            int i2 = i + 10;
            this.I = i2;
            int i3 = this.T;
            int i4 = i3 + 10;
            this.T = i4;
            if (i2 > 360) {
                this.I = i - 350;
            }
            if (i4 > 360) {
                this.T = i3 - 350;
            }
            if (this.O1) {
                float f = this.N1;
                if (f < 160.0f) {
                    this.N1 = (float) (f + 2.5d);
                    invalidate();
                }
            } else {
                float f2 = this.N1;
                if (f2 > 10.0f) {
                    this.N1 = f2 - 5.0f;
                    invalidate();
                }
            }
            float f3 = this.N1;
            if (f3 == 160.0f || f3 == 10.0f) {
                this.O1 = !this.O1;
                invalidate();
            }
        }
    }

    @Override
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        this.N1 = 10.0f;
        int i5 = this.h;
        // 🌟 ضبط حدود الدائرة لتبقى داخل الإطار (تمت زيادة المسافة الداخلية margin)
        int margin = i5 * 4; 
        this.A = new RectF(margin, margin, i - margin, i2 - margin);
        int i7 = this.q;
        this.B = new RectF(margin + i7, margin + i7, (i - margin) + i7, (i2 - margin) + i7);
    }

    // 🌟 دالة مهمة لتغيير لون الدائرة من MainActivity
    public void setColor(int i) { 
        this.f = i; 
        this.t.setColor(i);
        invalidate();
    }
}
