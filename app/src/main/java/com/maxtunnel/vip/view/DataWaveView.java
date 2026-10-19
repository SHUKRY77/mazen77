package com.maxtunnel.vip;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class DataWaveView extends View {
    private Paint paintLine1, paintLine2;
    private Path path1 = new Path();
    private Path path2 = new Path();
    private float phase = 0;
    private boolean isAnimating = false;

    public DataWaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintLine1 = new Paint();
        paintLine1.setColor(Color.parseColor("#4CAF50")); 
        paintLine1.setStyle(Paint.Style.STROKE);
        paintLine1.setStrokeWidth(2f); // خط رفيع
        paintLine1.setAntiAlias(true);

        paintLine2 = new Paint();
        paintLine2.setColor(Color.parseColor("#81C784")); 
        paintLine2.setStyle(Paint.Style.STROKE);
        paintLine2.setStrokeWidth(1.5f); // خط أنحف
        paintLine2.setAntiAlias(true);
    }

    public void startAnimation() {
        isAnimating = true;
        invalidate();
    }

    public void stopAnimation() {
        isAnimating = false;
        phase = 0;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int width = getWidth();
        int height = getHeight();
        int centerY = height / 2;

        path1.reset();
        path2.reset();

        if (!isAnimating) return; 

        path1.moveTo(0, centerY);
        path2.moveTo(0, centerY);

        for (int x = 0; x <= width; x += 10) {
            float y1 = (float) (Math.sin((x * 0.015) + phase) * (height * 0.25)) + centerY;
            path1.lineTo(x, y1);

            float y2 = (float) (Math.sin((x * 0.02) + phase + 1.5) * (height * 0.18)) + centerY;
            path2.lineTo(x, y2);
        }

        canvas.drawPath(path1, paintLine1);
        canvas.drawPath(path2, paintLine2);

        if (isAnimating) {
            phase += 0.1f; 
            invalidate(); 
        }
    }
}
