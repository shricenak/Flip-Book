package edu.bloomu.sjh91055.finalproj;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;

/**
 * The main activity of the Flip Book app. Most of the activity is taken up by the
 * custom DoodleView class, with a small ColorPickerView at the bottom. Four buttons
 * take up the lower right corner. It also provides the functionality of
 * many menu items.
 *
 * @author Steven Hricenak
 */
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_STORAGE = 0;

    private DoodleView doodle;
    private ColorPickerView colorPicker;
    private FrameLayout f1;
    private FrameLayout f2;
    private String videoName;

    /**
     * Starts the activity by instantiating the DoodleView and ColorPickerView and adding
     * them to their proper FrameViews. It loads the pre-saved strings from
     * SharedPreferences and converts them to Bitmaps that are filled in an ArrayList
     * that is then passed to the DoodleView constructor.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ArrayList<Bitmap> bitmaps = new ArrayList<>();
        SharedPreferences sp = getPreferences(Context.MODE_PRIVATE);
        int frameCount = sp.getInt("numberOfFrames", 0);

        byte[] imageAsBytes;
        String encodedBitmap;
        Bitmap bm;
        for (int i = 0; i < frameCount && i < DoodleView.MAX_FRAME; i++) {
            encodedBitmap = sp.getString("frame" + i, null);
            if (encodedBitmap != null) {
                imageAsBytes = Base64.decode(encodedBitmap.getBytes(), Base64.DEFAULT);
                bm = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
                bm = bm.copy(Bitmap.Config.ARGB_8888, true); //makes mutable
                bitmaps.add(bm);
            }
        }

        doodle = new DoodleView(this, bitmaps);
        f1 = (FrameLayout) findViewById(R.id.doodle);
        f1.addView(doodle);

        colorPicker = new ColorPickerView(this);
        f2 = (FrameLayout) findViewById(R.id.colorPicker);
        f2.addView(colorPicker);

        findAvailableName();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    /**
     * When the app is stopped, the ArrayList of frames is received from the DoodleView object,
     * and every Bitmap in it is converted to a string and stored into SharedPreferences.
     */
    @Override
    protected void onStop() {
        super.onStop();
        ArrayList<Bitmap> bitmaps = doodle.getArray();
        int frameCount = bitmaps.size();
        SharedPreferences sp = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("numberOfFrames", frameCount);

        for (int i = 0; i < frameCount; i++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Bitmap bm;
            byte[] b;
            String encodedBitmap;

            bm = bitmaps.get(i);
            bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
            b = baos.toByteArray();
            encodedBitmap = Base64.encodeToString(b, Base64.DEFAULT);
            editor.putString("frame" + i, encodedBitmap);
        }
        editor.commit();
    }

    /*
     * These following five methods simply call methods with in the DoodleView class.
     */

    public void next(View view) {
        doodle.nextFrame();
    }

    public void prev(View view) {
        doodle.prevFrame();
    }

    public void addFrame(View view) {
        doodle.addFrame();
    }

    public void playAnimation(View view) {
        doodle.playAnimation();
    }

    public void setDoodleColor(int color) {
        doodle.setPaintColor(color);
    }

    /**
     * Presents a dialog box to the user, making sure they want to restart the project,
     * then clears all frames and the drawing canvas.
     *
     * @return true
     */
    public boolean newDoodle(MenuItem m) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle("Restart project?");
        builder.setMessage("Everything will be cleared, and cannot be recovered.");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                resetFrameViews();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //do nothing
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        return true;
    }

    /**
     * A helper method the resets the DoodleView and ColorPickerView.
     */
    private void resetFrameViews() {
        doodle = new DoodleView(this);
        f1 = (FrameLayout) findViewById(R.id.doodle);
        f1.addView(doodle);

        colorPicker = new ColorPickerView(this);
        f2 = (FrameLayout) findViewById(R.id.colorPicker);
        f2.addView(colorPicker);
    }

    /**
     * Toggles whether or not the previous frame should be overlaid behind the current one.
     *
     * @return true
     */
    public boolean toggleOnion(MenuItem m) {
        doodle.toggleOnionSkin();
        return true;
    }

    /**
     * Creates a new frame, with the contents of the current one.
     *
     * @return true
     */
    public boolean duplicateFrame(MenuItem m) {
        doodle.duplicateFrame();
        return true;
    }

    /**
     * Clears the current canvas.
     *
     * @return true
     */
    public boolean clearFrame(MenuItem m) {
        doodle.resetCanvas();
        return true;
    }

    /**
     * Sets the videoName field to an unused filename. All video names are in the format
     * of VideoX.mp4. X is usually the number of pre-existing videos in the directory.
     * But this may cause problems if the user were to delete videos (say the user
     * has ten videos, but deletes Video3.mp4; then the app would try to save to
     * Video9.mp4, even though it already exists.) To remedy this a technique is employed
     * that is similar to quadratic probing in hash tables. If the video title already
     * exists in the directory, the number is added by one, then four, nine, etc.
     * It is done this way to ensure an unused name is found, and to do it somewhat
     * efficiently. (Using linear probing would result in a O(n^2) runtime, which is
     * unfavorable, since the current method for checking if the video exists is already O(n).)
     */
    private void findAvailableName() {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        File dir = new File(path.getAbsolutePath() + "/FlipBook");
        if (dir.isDirectory()) {
            String[] files = dir.list();
            int h = files.length;
            int offset = 0;
            videoName = "Video" + h + ".mp4";
            while (fileInList(files, videoName)) {
                offset++;
                videoName = "Video" + (h + (offset * offset)) + ".mp4";
            }
        } else
            videoName = "Video0.mp4";
    }

    /**
     * Checks a array of file names to see if the passed file is contained. Simply a linear
     * list, with a pretty inefficient O(n) runtime.
     *
     * @param list     the array of file names
     * @param fileName the file to be checked for
     * @return true if the file is found, false otherwise
     */
    private boolean fileInList(String[] list, String fileName) {
        for (String file : list)
            if (file.equals(fileName))
                return true;
        return false;
    }

    /**
     * Method to save the current animation as an mp4 file on the device.
     * Called from both the menu item and the onRequestPermissionsResult methods.
     */
    public void saveVideo() {
        //Checking permissions
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_STORAGE);
        } else {
            doodle.saveToVideo(videoName);
            findAvailableName();
        }
    }

    /**
     * Driver method for saving the video, called from the dropdown menu.
     *
     * @param m the menu item pressed
     * @return true
     */
    public boolean saveVideo(MenuItem m) {
        saveVideo();
        return true;
    }

    /**
     * Action listener called when the user first gives the app permission to save media to
     * the device. Once given permission, it simply finishes by calling the saveVideo method.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    saveVideo();
        }
    }
}
