package com.github.mateohi.poc7_ocr;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import com.google.android.glass.content.Intents;
import com.google.android.glass.widget.CardBuilder;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.IOException;

public class CameraActivity extends Activity {

    private static final String TAG = CameraActivity.class.getSimpleName();
    private static final int TAKE_PICTURE_REQUEST = 1;
    private static final String TESS_DATA_PATH = Environment.getExternalStorageDirectory() +
            "/DCIM/tesseract-ocr";
    public static final String TESS_DATA_LANG = "eng";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        takePicture();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) {
            String thumbnailPath = data.getStringExtra(Intents.EXTRA_THUMBNAIL_FILE_PATH);
            String picturePath = data.getStringExtra(Intents.EXTRA_PICTURE_FILE_PATH);

            showPreview(thumbnailPath);
            processPicture(thumbnailPath);
            //processPictureWhenReady(picturePath);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showPreview(String thumbnailPath) {
        Log.i(TAG, "Showing image preview ...");

        Bitmap preview = BitmapFactory.decodeFile(thumbnailPath);
        View initialView = new CardBuilder(this, CardBuilder.Layout.CAPTION)
                .setText("Analysing image ...")
                .addImage(preview)
                .getView();
        setContentView(initialView);
    }

    private void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, TAKE_PICTURE_REQUEST);
    }

    private void processPictureWhenReady(final String picturePath) {
        Log.i(TAG, "Trying to process...");

        final File pictureFile = new File(picturePath);

        if (pictureFile.exists()) {
            processPicture(picturePath);
        }
        else {
            // The file does not exist yet. Before starting the file observer, you
            // can update your UI to let the user know that the application is
            // waiting for the picture (for example, by displaying the thumbnail
            // image and a progress indicator).

            final File parentDirectory = pictureFile.getParentFile();
            FileObserver observer = new FileObserver(parentDirectory.getPath(),
                    FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
                // Protect against additional pending events after CLOSE_WRITE
                // or MOVED_TO is handled.
                private boolean isFileWritten;

                @Override
                public void onEvent(int event, String path) {
                    if (!isFileWritten) {
                        // For safety, make sure that the file that was created in
                        // the directory is actually the one that we're expecting.
                        File affectedFile = new File(parentDirectory, path);
                        isFileWritten = affectedFile.equals(pictureFile);

                        if (isFileWritten) {
                            stopWatching();

                            // Now that the file is ready, recursively call
                            // processPictureWhenReady again (on the UI thread).
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    processPictureWhenReady(picturePath);
                                }
                            });
                        }
                    }
                }
            };
            observer.startWatching();
        }
    }

    private void processPicture(String path) {
        Log.i(TAG, "Processing image ...");

        Bitmap picture = correctTesseractBitmap(path);

        TessBaseAPI tessBaseAPI = new TessBaseAPI();
        tessBaseAPI.init(TESS_DATA_PATH, TESS_DATA_LANG);
        tessBaseAPI.setImage(picture);

        String result = tessBaseAPI.getUTF8Text();
        tessBaseAPI.end();

        updateResponseView(result);
    }

    private void updateResponseView(String result) {
        View resultView = new CardBuilder(this, CardBuilder.Layout.TEXT)
                .setText(result)
                .getView();

        setContentView(resultView);
    }

    private Bitmap correctTesseractBitmap(String path) {
        Bitmap initial = BitmapFactory.decodeFile(path);

        return initial.copy(Bitmap.Config.ARGB_8888, true);
        /*int rotation = getExifRotationFromPath(path);

        if (rotation != 0) {
            int width = initial.getWidth();
            int height = initial.getHeight();

            // Setting pre rotation
            Matrix matrix = new Matrix();
            matrix.preRotate(rotation);

            // Rotating Bitmap
            Bitmap corrected = Bitmap.createBitmap(initial, 0, 0, width, height, matrix, false);

            // Convert to ARGB_8888
            return corrected.copy(Bitmap.Config.ARGB_8888, true);
        } else {
            return initial;
        }*/
    }

    private int getExifRotationFromPath(String path) {
        try {
            ExifInterface exif = new ExifInterface(path);

            int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return  270;
                default:
                    return 0;
            }
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    private static Bitmap getTextImage(String text, int width, int height) {
        //getTextImage("HELLO THERE", 640, 320)
        final Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Paint paint = new Paint();
        final Canvas canvas = new Canvas(bmp);

        canvas.drawColor(Color.WHITE);

        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(24.0f);
        canvas.drawText(text, width / 2, height / 2, paint);

        return bmp;
    }

}
