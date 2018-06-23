package me.yusei.tangoplayer;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.nononsenseapps.filepicker.Utils;

import java.io.File;
import java.util.List;

import me.yusei.tangoplayer.filepicker.FilteredFilePickerActivity;

public class StartActivity extends AppCompatActivity {
    private static final int FILE_CODE = 1;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkWriteExternalStoragePermission();
    }

    @Override
    public void onResume() {
        super.onResume();
        //TODO: if videofilepath is invalid releasePlayer(), launch FilePicker(); in VideoPlayerActivity

    }

    private void startFilePickerActivity(){
        VideoPlayerConfig.setVideoPosition(this, 0);
        Intent i = new Intent(this, FilteredFilePickerActivity.class);
        i.putExtra(FilteredFilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        i.putExtra(FilteredFilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilteredFilePickerActivity.EXTRA_MODE, FilteredFilePickerActivity.MODE_FILE);
        i.putExtra(FilteredFilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
        startActivityForResult(i, FILE_CODE);
    }

    private void startVideoPlayerActivity(String filePath){
        Intent intent = new Intent(StartActivity.this, VideoPlayerActivity.class);
        intent.putExtra(VideoPlayerConfig.LOCATION, filePath);
        //delete all activity except this activity.
        startActivity(intent);
        finish();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == FILE_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            // Use the provided utility method to parse the result
            List<Uri> files = Utils.getSelectedFilesFromResult(intent);
//            for (Uri uri: files) {
//                File file = Utils.getFileForUri(uri);
//                // Do something with the result...
//            }
            File file = Utils.getFileForUri(files.get(0));
            VideoPlayerConfig.setVideoPosition(this, 0);
            startVideoPlayerActivity(file.toString());
            //TODO call setvideofilepath() in initialaizePlayer()


//            if (initializePlayer(file.toString())) {
//                setVideoFilePath(file.toString());
//            } else {
//                Toast.makeText(this, getResources().getString(R.string.error_invalid_video_file), Toast.LENGTH_SHORT).show();
//                launchFilePicker();
//            }
        }else if(resultCode ==  RESULT_CANCELED){
            finish();
        }
    }

    private void checkWriteExternalStoragePermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // need to get permission
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    //In the case, an user reject permission before, write code for show description of permission.

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    new AlertDialog.Builder(StartActivity.this)
                            .setTitle("Permission required")
                            .setMessage("Location is required for this application to work ! ")
                            .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                            MY_PERMISSIONS_REQUEST_WRITE_STORAGE);

                                }

                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                    finish();
                                }
                            }).show();
                } else {
                    // No explanation needed, we can request the permission.
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
                }

            } else {
                //no need to get permission
                startNextActivity();
            }
        }else{
            startNextActivity();
        }
    }

    private void startNextActivity(){
        if(VideoPlayerConfig.getVideoFilePath(this) == null) {
            startFilePickerActivity();
        }else{
            startVideoPlayerActivity(VideoPlayerConfig.getVideoFilePath(this));
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        if(requestCode == MY_PERMISSIONS_REQUEST_WRITE_STORAGE){
            // If request is cancelled, the result arrays are empty.
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                startNextActivity();
            }else{
                Toast.makeText(this, getResources().getString(R.string.error_msg_no_write_storage_permission), Toast.LENGTH_LONG).show();
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Utility.infoLog("write storage permission NOT granted.");
                //TODO: show permission dialog again.
                checkWriteExternalStoragePermission();
            }
        }
    }

}
