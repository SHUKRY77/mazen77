package com.maxtunnel.vip.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.FrameLayout;

// استدعاء قاموس التطبيق الخاص بك
import com.maxtunnel.vip.R;

public class RoundCornerLayout extends FrameLayout {
    public boolean topLeftEnabled = true;
    public boolean topRightEnabled = true;
    public boolean bottomLeftEnabled = true;
    public boolean bottomRightEnabled = true;
    public float cornerRadius;

    public RoundCornerLayout(Context context) {
        this(context, null, 0);
    }

    public RoundCornerLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public RoundCornerLayout(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        this.cornerRadius = dpToPx(10.0f); // القيمة الافتراضية
        setupAttributes(context, attributeSet);
        setLayerType(LAYER_TYPE_SOFTWARE, null); // لضمان عمل القص (Clipping) بشكل صحيح
    }

    private void setupAttributes(Context context, AttributeSet attributeSet) {
        if (attributeSet != null) {
            // الطريقة الاحترافية والأكثر أماناً (تمنع انهيار التطبيق بسبب الـ dp)
            TypedArray a = context.obtainStyledAttributes(attributeSet, R.styleable.RoundCornerLayout);
            
            // قراءة نصف قطر الزاوية بأمان تام
            this.cornerRadius = a.getDimension(R.styleable.RoundCornerLayout_cornerRadius, this.cornerRadius);
            
            // قراءة حالات الزوايا (مفعلة أم لا)
            this.topLeftEnabled = a.getBoolean(R.styleable.RoundCornerLayout_topLeftEnabled, true);
            this.topRightEnabled = a.getBoolean(R.styleable.RoundCornerLayout_topRightEnabled, true);
            this.bottomLeftEnabled = a.getBoolean(R.styleable.RoundCornerLayout_bottomLeftEnabled, true);
            this.bottomRightEnabled = a.getBoolean(R.styleable.RoundCornerLayout_bottomRightEnabled, true);
            
            a.recycle(); // إغلاق المصفوفة لتنظيف الذاكرة
        }
    }

    public final float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int saveCount = canvas.save();
        Path path = new Path();
        RectF rectF = new RectF(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight());
        
        // مصفوفة لتحديد زوايا القص (كل زاوية تأخذ قيمتين X و Y)
        float[] radii = new float[8];
        
        if (this.topLeftEnabled) {
            radii[0] = this.cornerRadius;
            radii[1] = this.cornerRadius;
        }
        if (this.topRightEnabled) {
            radii[2] = this.cornerRadius;
            radii[3] = this.cornerRadius;
        }
        if (this.bottomRightEnabled) {
            radii[4] = this.cornerRadius;
            radii[5] = this.cornerRadius;
        }
        if (this.bottomLeftEnabled) {
            radii[6] = this.cornerRadius;
            radii[7] = this.cornerRadius;
        }
        
        path.addRoundRect(rectF, radii, Path.Direction.CW);
        canvas.clipPath(path);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(saveCount);
    }

    public float getRadius() {
        return this.cornerRadius;
    }

    public void setRadius(float radius) {
        this.cornerRadius = radius;
        invalidate();
    }
}
