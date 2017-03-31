package com.okason.prontonotepadfirebase.ui.notes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * Created by Valentine on 9/4/2015.
 */
public class LinedEditText extends EditText {

    private Rect linesRect;
    private Paint linesPaint;

    // we need this constructor for LayoutInflater
    public LinedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        linesRect = new Rect();
        linesPaint = new Paint();
        linesPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        linesPaint.setColor(Color.rgb(113,85,58));
    }


    @Override
    protected void onDraw(Canvas canvas) {

        int height = getHeight();
        int line_height = getLineHeight();

        int count = height / line_height;

        if (getLineCount() > count)
            count = getLineCount();

        Rect re = linesRect;
        Paint paint = linesPaint;
        int lineBounds = getLineBounds(0, re);

        for (int i = 0; i < count; i++) {

            canvas.drawLine(re.left, lineBounds + 1, re.right, lineBounds + 1, paint);
            lineBounds += getLineHeight();

            super.onDraw(canvas);
        }

    }
}
