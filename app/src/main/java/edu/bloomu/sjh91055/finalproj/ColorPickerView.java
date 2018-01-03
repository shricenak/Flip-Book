package edu.bloomu.sjh91055.finalproj;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Color;

/**
 * A custom View that allows the user to select one of eight colors.
 */
public class ColorPickerView extends View {
    private final int COLORS[] = {Color.BLACK, Color.WHITE, Color.RED, Color.GREEN,
            Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA};

    private int viewWidth;
    private int viewHeight;

    private int squareDim;
    private int selected = 0;

    private Paint paint;

    /**
     * Initializes the View.
     */
    public ColorPickerView(Context context) {
        super(context);
        paint = new Paint();
    }

    /**
     * Sets the width and height attributes, and calculates the size of the color select squares.
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        viewWidth = getWidth();
        viewHeight = getHeight();

        squareDim = viewHeight / 10;
        invalidate();
    }

    /**
     * Draws eight colored circles with a grey box behind the one currently selected.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setColor(Color.LTGRAY);
        canvas.drawRect(0, viewHeight / 2, 8 * squareDim, viewHeight / 2 + squareDim, paint);

        paint.setColor(Color.DKGRAY);
        canvas.drawRect(selected * squareDim, viewHeight / 2, (selected + 1) * squareDim,
                viewHeight / 2 + squareDim, paint);

        for (int i = 0; i < 8; i++) {
            paint.setColor(COLORS[i]);
            canvas.drawCircle(i * squareDim + squareDim / 2, viewHeight / 2 + squareDim / 2,
                    squareDim / 2, paint);
        }
    }

    /**
     * Detects the users click to set the new color.
     *
     * @return true
     */
    @Override
    public boolean onTouchEvent(MotionEvent e) {

        if (e.getActionMasked() == MotionEvent.ACTION_UP) {
            float x = e.getX();
            int s = (int) (x / squareDim);
            if (s < COLORS.length)
                selected = s;
            setColor();
            invalidate();
        }
        return true;
    }

    /**
     * Calls the Main Activity's method that sets the color of the ink in the DoodleView.
     */
    private void setColor() {
        ((MainActivity) getContext()).setDoodleColor(COLORS[selected]);
    }
}
