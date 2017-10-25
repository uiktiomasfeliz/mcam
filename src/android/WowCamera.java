package org.apache.cordova.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import static com.flurgle.camerakit.CameraKit.Constants.FLASH_AUTO;
import static com.flurgle.camerakit.CameraKit.Constants.FLASH_OFF;
import static com.flurgle.camerakit.CameraKit.Constants.FLASH_ON;

import com.flurgle.camerakit.CameraListener;
import com.flurgle.camerakit.CameraView;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class WowCamera extends AppCompatActivity {

    private static final String TAG = "wOwCamera";

    private static final int REQUEST_READ_PERMISSION = 6969;

    private static final String FRAGMENT_DIALOG = "dialog";

    private static final int[] FLASH_OPTIONS = {
            FLASH_AUTO,
            FLASH_OFF,
            FLASH_ON,
    };

    private int[] FLASH_ICONS;

    private RecyclerView gallery;
    private GalleryItemAdapter adapter;
    private FloatingActionButton btnCapture;
    private AppCompatImageView btnFlash;

    private ActionMenuView wowMenu;

    private CameraView camera;
    private CropImageView cropImageView;

    private FakeR fakeR;
    private int currentFlash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fakeR = new FakeR(this);

        setContentView(fakeR.getId("layout", "wowcam_main"));

        Toolbar t = (Toolbar) findViewById(fakeR.getId("id", "wowToolbar" ));
        wowMenu = (ActionMenuView) t.findViewById(fakeR.getId("id", "wowMenu" ));
        wowMenu.setOnMenuItemClickListener(new ActionMenuView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                //Log.e(TAG, "onMenuItemClick: " + menuItem.toString());

                if(menuItem.getItemId() == fakeR.getId("id", "cancel" )) {
                    setResult(Activity.RESULT_CANCELED, new Intent());
                    finish();
                } else if(menuItem.getItemId() == fakeR.getId("id", "rotate" )) {
                    if(cropImageView != null) cropImageView.rotateImage(90);
                } else if(menuItem.getItemId() == fakeR.getId("id", "save" )) {

                    Bitmap b = cropImageView.getCroppedImage();

                    //String url = MediaStore.Images.Media.insertImage(getApplicationContext().getContentResolver(), b, "" , "");
                    //Log.w(TAG, "MediaStore.Images.Media.insertImage: " + url);


                    Calendar cal = new GregorianCalendar();
                    Date date = cal.getTime();
                    SimpleDateFormat df = new SimpleDateFormat("yyyyMMddhhmmss", new Locale("es_ES"));
                    String formatteDate = df.format(date);

                    String fotoName = "gecor" + System.currentTimeMillis();

                    //if()

                    File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                            fotoName + formatteDate + ".jpg");

                    Boolean res = false;

                    try {

                        res = b.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(file));

                    } catch (IOException e) {
                        Log.w(TAG, "Cannot write to " + file, e);
                    } finally {
                        if (res) {
                            setResult(Activity.RESULT_OK, new Intent().setData(Uri.fromFile(file)));
                            finish();
                        } else {
                            setResult(Activity.RESULT_CANCELED, new Intent());
                            finish();
                        }
                    }

                }//endif

                return onOptionsItemSelected(menuItem);
            }
        });

        setSupportActionBar(t);
        getSupportActionBar().setTitle(null);
        getSupportActionBar().hide();

        FLASH_ICONS = new int[] {
                fakeR.getId("drawable", "ic_flash_auto" ),
                fakeR.getId("drawable","ic_flash_off"),
                fakeR.getId( "drawable", "ic_flash_on"),
        };

        cropImageView = (CropImageView) findViewById(fakeR.getId("id", "cropImageView" ));

        camera = (CameraView) findViewById(fakeR.getId("id", "camera" ));

        camera.setCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(byte[] picture) {
                super.onPictureTaken(picture);

                switchToCropper(true);
                btnCapture.setEnabled(true);

                // Create a bitmap
                Bitmap result = BitmapFactory.decodeByteArray(picture, 0, picture.length);
                cropImageView.setImageBitmap(result);
            }
        });

        gallery = (RecyclerView) findViewById(fakeR.getId("id", "wowGallery" ));

        if(gallery != null) {

            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                gallery.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false));
            } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                gallery.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
            }


            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED ) {
                loadGallery();
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) || ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ConfirmationDialogFragment
                        .newInstance(fakeR.getId("string", "camera_permission_confirmation"),//resources.getIdentifier("camera_permission_confirmation", "string", package_name
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                REQUEST_READ_PERMISSION,
                                fakeR.getId("string", "camera_permission_not_granted"))
                        .show(getSupportFragmentManager(), FRAGMENT_DIALOG);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_READ_PERMISSION);
            }


        }

        btnCapture = (FloatingActionButton) findViewById(fakeR.getId("id", "btn_take_picture" ));
        if (btnCapture != null) {
            btnCapture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(camera!=null) {
                        btnCapture.setEnabled(false);
                        camera.captureImage();
                    }
                }
            });
        }

        btnFlash = (AppCompatImageView) findViewById(fakeR.getId("id", "btn_change_flash" ));
        if (btnFlash != null) {

            if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) btnFlash.setVisibility(View.GONE);

            btnFlash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(camera!=null) {
                        currentFlash = (currentFlash + 1) % FLASH_OPTIONS.length;
                        //btnFlash.setBackgroundResource(FLASH_ICONS[currentFlash]);
                        btnFlash.setImageResource(FLASH_ICONS[currentFlash]);
                        camera.setFlash(FLASH_OPTIONS[currentFlash]);
                    }
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        // use wowMenu here
        inflater.inflate(fakeR.getId("menu", "main" ), wowMenu.getMenu());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Do your actions here
        return true;
    }

    private ArrayList<String> getAllShownImagesPath() {

        Cursor cursor;
        String absolutePathOfImage;
        int column_index_data;
        ArrayList<String> listOfAllImages = new ArrayList<String>();

        cursor = getApplicationContext().getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DATA,
                        MediaStore.Images.Media.ORIENTATION,
                        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                        MediaStore.Images.Media.BUCKET_ID,
                        MediaStore.Images.Media.MIME_TYPE ,
                },
                null,
                null,
                MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC"
        );

        if(cursor != null) {
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

            while (cursor.moveToNext()) {
                absolutePathOfImage = cursor.getString(column_index_data);

                listOfAllImages.add(absolutePathOfImage);
            }
        }

        cursor = getApplicationContext().getContentResolver().query(
                MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DATA,
                        MediaStore.Images.Media.ORIENTATION,
                        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                        MediaStore.Images.Media.BUCKET_ID,
                        MediaStore.Images.Media.MIME_TYPE
                },
                null,
                null,
                MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC"
        );

        if(cursor != null) {
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

            while (cursor.moveToNext()) {
                absolutePathOfImage = cursor.getString(column_index_data);

                listOfAllImages.add(absolutePathOfImage);
            }
        }
        return listOfAllImages;
    }

    private void loadGallery(){

        adapter = new GalleryItemAdapter(getApplicationContext(), getAllShownImagesPath(),
                new GalleryItemAdapter.OnItemClickListener() {
                    @Override
                    public void galleryResponse(String img) {
                        Log.e(TAG, "galleryResponse: " + img);
                        try{
                            //Bundle conData = new Bundle();
                            //conData.putString("SDCardUrl", img);

                            Intent intent = new Intent();
                            //intent.putExtras(conData);
                            intent.setData(Uri.fromFile(new File(img)));
                            setResult(Activity.RESULT_OK, intent);
                        } catch (Exception e){
                            setResult(Activity.RESULT_CANCELED, new Intent());
                        }
                        finish();
                    }
                });

        gallery.setAdapter(adapter);

        gallery.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera.start();
        //startCamera();
    }

    @Override
    protected void onPause() {
        //camera.stop();
        stopCamera();
        super.onPause();
    }

    private void startCamera(){
        if(camera!=null) camera.start();
    }

    private void stopCamera(){
        if(camera!=null) camera.stop();
    }

    private void switchToCropper(Boolean iddle){
        if(iddle) {
            getSupportActionBar().show();
            camera.setVisibility(View.GONE);
            gallery.setVisibility(View.GONE);
            btnCapture.setVisibility(View.GONE);
            btnFlash.setVisibility(View.GONE);
            cropImageView.setVisibility(View.VISIBLE);
        } else {
            getSupportActionBar().hide();
            camera.setVisibility(View.VISIBLE);
            gallery.setVisibility(View.VISIBLE);
            btnCapture.setVisibility(View.VISIBLE);
            btnFlash.setVisibility(View.VISIBLE);
            cropImageView.setVisibility(View.GONE);
        }
    }

}
