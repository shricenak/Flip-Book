package edu.bloomu.sjh91055.finalproj;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.CountDownTimer;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import org.jcodec.api.android.AndroidSequenceEncoder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A custom View that handles all drawing and animating. The user paints their frames, then
 * when they are finished drawing they may play them as an animation. This also handles
 * creating and exporting an mp4 video to the device.
 *
 * @author Steven Hricenak
 */
public class DoodleView extends View {
    public static final int MAX_FRAME = 32;
    private static final int BACKGROUND_COLOR = Color.WHITE;
    private static final int STROKE_WIDTH = 20;
    private static final int FRAME_RATE = 4;

    private int width;
    private int height;

    private Path drawPath;
    private Paint drawPaint;
    private Paint canvasPaint;
    private int paintColor = Color.BLACK;

    private Canvas drawCanvas;
    private Bitmap canvasBitmap;

    private boolean imageOverlay = true;
    private Paint previousPaint;

    private ArrayList<Bitmap> frames;
    private int frameIndex;

    private boolean animating;

    /**
     * Creates an empty project.
     */
    public DoodleView(Context context) {
        super(context);
        frames = new ArrayList<>();
        animating = false;
        drawCanvas = new Canvas();
        setupDrawing();
    }

    /**
     * Creates a project with the frames passed in the ArrayList of Bitmaps.
     *
     * @param bitmaps the frames to be initialized
     */
    public DoodleView(Context context, ArrayList<Bitmap> bitmaps) {
        super(context);
        frames = bitmaps;
        animating = false;
        setupDrawing();
        if (frames.size() > 0) {
            frameIndex = frames.size() - 1;
            drawCanvas = new Canvas(frames.get(frameIndex));
        }
    }

    /**
     * Takes care of all the settings in the Paint object. A helper method to be called
     * by the constructors.
     */
    private void setupDrawing() {
        drawPath = new Path();
        drawPaint = new Paint();

        drawPaint.setColor(paintColor);
        drawPaint.setStrokeWidth(STROKE_WIDTH);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        canvasPaint = new Paint(Paint.DITHER_FLAG);
        canvasPaint.setStyle(Paint.Style.STROKE);

        previousPaint = new Paint();
        previousPaint.setAlpha(25);
    }

    /**
     * Sets the width and height values. This is where the first frame is set, because
     * it needs the width and height that are set to zero before this method is called
     * for the first time.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        if (frames.size() < 1)
            newFrame();
    }

    /**
     * Draws the View: it can either be the frame the user is currently drawing, or the
     * animation itself.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (animating) {
            if (frameIndex < frames.size())
                canvas.drawBitmap(frames.get(frameIndex), 0, 0, drawPaint);
            else
                canvas.drawColor(BACKGROUND_COLOR);
            //The code is set up so that the index should not go out of bounds, but in very rare
            //cases, likely due to the countdown timer thread, it will try to index one greater
            //than the bound. The if statement is to prevent this.
        } else {
            canvas.drawColor(BACKGROUND_COLOR);
            canvas.drawRect(0, 0, getWidth(), getHeight(), canvasPaint);

            canvas.drawBitmap(frames.get(frameIndex), 0, 0, drawPaint);

            if (imageOverlay && frameIndex > 0 && frames.get(frameIndex - 1) != null)
                canvas.drawBitmap(frames.get(frameIndex - 1), 0, 0, previousPaint);

            canvas.drawPath(drawPath, drawPaint);
        }
    }

    /**
     * Draws paths when the user touches the screen.
     *
     * @return true
     */
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                drawPath.moveTo(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                drawPath.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
                drawCanvas.drawPath(drawPath, drawPaint);
                drawPath.reset();
                break;
        }
        invalidate();
        return true;
    }

    /**
     * Creates a new blank frame.
     */
    private void newFrame() {
        if (!animating) {
            canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            frames.add(canvasBitmap);
            frameIndex = frames.size() - 1;
            drawCanvas = new Canvas(frames.get(frameIndex));
            drawCanvas.drawColor(BACKGROUND_COLOR);
            invalidate();
        }
    }

    /**
     * Clears the images on the current frame.
     */
    public void resetCanvas() {
        if (!animating)
            drawCanvas.drawColor(BACKGROUND_COLOR);
        invalidate();
    }

    /**
     * Driver method for adding a new frame to the animation. The method will do nothing if
     * the animation is playing. This method handles the apps largest bug:
     * after a 37th frame is added, an OutOfMemoryError is thrown.
     * This will crash the app, so this method prevents a 37th frame from being added.
     */
    public void addFrame() {
        if (!animating)
            if (frames.size() < MAX_FRAME)
                newFrame();
            else
                Toast.makeText(getContext(), "Maximum number of frames reached",
                        Toast.LENGTH_SHORT).show();
    }

    /**
     * Sets a CountDownTimer to flip through each frame at a set frame rate. Nothing can be
     * done but watch the animation whil it is playing.
     */
    public void playAnimation() {
        if (!animating) {
            animating = true;
            frameIndex = -1;
            int frameCount = frames.size() + 1;
            //all intuition tells me that this should not be (size + 1)
            //yet in all testing, the last frame is left out if it's merely (size).

            int animationDuration = frameCount * (1000 / FRAME_RATE);
            new CountDownTimer(animationDuration, (1000 / FRAME_RATE)) {
                @Override
                public void onTick(long millisUntilFinished) {
                    frameIndex++;
                    invalidate();
                }

                @Override
                public void onFinish() {
                    animating = false;
                    frameIndex = frames.size() - 1;
                    setCurrentFrame();
                    invalidate(); //restores drawing pad to last frame
                }
            }.start();
        }
    }

    /**
     * Moves to the next frame in the ArrayList of Bitmaps.
     */
    public void nextFrame() {
        if (!animating) {
            if (frameIndex < frames.size() - 1) {
                frameIndex++;
                setCurrentFrame();
            } else
                Toast.makeText(getContext(), "End of frames", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Moves to the next frame in the ArrayList of Bitmaps.
     */
    public void prevFrame() {
        if (!animating) {
            if (frameIndex > 0) {
                frameIndex--;
                setCurrentFrame();
            } else
                Toast.makeText(getContext(), "Beginning of frames", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Helper method used to make sure the displayed frame is the Bitmap being drawn on.
     */
    private void setCurrentFrame() {
        drawCanvas = new Canvas(frames.get(frameIndex));
        invalidate();
    }


    /**
     * Sets the ink color to be drawn with.
     *
     * @param c the new color
     */
    public void setPaintColor(int c) {
        paintColor = c;
        drawPaint.setColor(paintColor);
    }

    /**
     * Toggles the image overlay.
     */
    public void toggleOnionSkin() {
        imageOverlay = !imageOverlay;
        String str = (imageOverlay ? "on" : "off");
        Toast.makeText(getContext(), "Overlay is now " + str, Toast.LENGTH_SHORT).show();
        invalidate();
    }

    /**
     * Creates a new frame with the Bitmap that is currently being displayed.
     */
    public void duplicateFrame() {
        if (!animating) {
            newFrame();
            drawCanvas.drawBitmap(frames.get(frameIndex - 1), 0, 0, drawPaint);
        }
    }

    /**
     * Uses the AndroidSequenceEncoder class from the external JCodec class to create
     * a video from the drawn frames. The video is saved as an mp4 in a sub-directory
     * withing the user's video files.
     *
     * @param videoName the name of the video file
     */
    public void saveToVideo(String videoName) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
            File dir = new File(path.getAbsolutePath() + "/FlipBook");
            if (!dir.isDirectory())
                dir.mkdir();
            File file = new File(dir, videoName);

            AndroidSequenceEncoder encoder =
                    AndroidSequenceEncoder.createSequenceEncoder(file, FRAME_RATE);

            for (Bitmap f : frames) {
                if (f.getHeight() % 2 != 0)
                    f.setHeight(f.getHeight() - 1); //errors occur when height is odd
                encoder.encodeImage(f);
            }

            encoder.finish();
            //Toast.makeText(getContext(), file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            Toast.makeText(getContext(), "Save successful", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Save Failed", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the ArrayList of frames.
     *
     * @return the ArrayList of frames
     */
    public ArrayList<Bitmap> getArray() {
        return frames;
    }
}
