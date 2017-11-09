package me.yusei.tangoplayer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.nononsenseapps.filepicker.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    //    public class MainActivity extends AppCompatActivity  implements Runnable, View.OnClickListener, MediaPlayer.OnTimedTextListener {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 1;
    private static final String VIDEO_FILE_PATH = "VIDEO_FILE_PATH";
    private static final String SUBTITLE_FILE_PATH = "SUBTITLE_FILE_PATH";
    private static final String VIDEO_DURATION = "VIDEO_DURATION";
    private static final int FILE_CODE = 1;
    private static SimpleExoPlayer player;
    private SimpleExoPlayerView mSimpleExoPlayerView;
    private boolean shouldAutoPlay;
    private DefaultTrackSelector trackSelector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button file = (Button) findViewById(R.id.file_button);

        file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lunchFilePicker();
            }
        });

        initializePlayer();

        //書き込み権限確認
        checkPermission();

    }

    private void initializePlayer(){
        if (getVideoFilePath() == null || playVideoFromFileUri(getVideoFilePath()) == false) {
            lunchFilePicker();
        }
    }

    private void releasePlayer() {
        if (player != null) {
            shouldAutoPlay = player.getPlayWhenReady();
            player.release();
            player = null;
            trackSelector = null;
        }
    }

    /**
     * Play Video From uri and return result
     *
     * @param uri
     * @return true if uri is valid and playable. false if uri is null or invalid.
     */
    private boolean playVideoFromFileUri(Uri uri) {
        if (uri == null) {
            return false;
        }
        // 1. Create a default TrackSelector
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        // 2. Create the player
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);

        // 3. Bind the player to the view.
        mSimpleExoPlayerView = new SimpleExoPlayerView(this);
        mSimpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.playerView);

        //Set media controller
        mSimpleExoPlayerView.setUseController(true);
        mSimpleExoPlayerView.requestFocus();
        mSimpleExoPlayerView.setControllerShowTimeoutMs(-1);

        // Bind the player to the view.
        mSimpleExoPlayerView.setPlayer(player);

        if (prepareExoPlayerFromFileUri(uri) == false) {
            Toast.makeText(this, "VideoFilePath is invalid. Chose Video file again.[playVideoFromFileUri]", Toast.LENGTH_SHORT);
            releasePlayer();
            return false;
        }

        SimpleExoPlayerView playerView = (SimpleExoPlayerView) findViewById(R.id.playerView);
        playerView.setPlayer(player);
        return true;
    }

    /**
     * prepare video file with subtitle if available.
     *
     * @param uri
     * @return true if prepare completed. false if prepare is not completed.
     */
    private boolean prepareExoPlayerFromFileUri(Uri uri) {
        DataSpec dataSpec = new DataSpec(uri);
        final FileDataSource fileDataSource = new FileDataSource();
        try {
            fileDataSource.open(dataSpec);
        } catch (FileDataSource.FileDataSourceException e) {
            e.printStackTrace();
            return false;
        }

        DataSource.Factory factory = new DataSource.Factory() {
            @Override
            public DataSource createDataSource() {
                return fileDataSource;
            }
        };

        MediaSource videoSource = new ExtractorMediaSource(fileDataSource.getUri(),
                factory, new DefaultExtractorsFactory(), null, null);
        if (subtitleFileExist(uri) == true) {
            Uri subtitleUri = getSubtitleFilePath();
            // https://google.github.io/ExoPlayer/doc/reference/com/google/android/exoplayer2/Format.html#sampleMimeType
            MediaSource subtitleSource = new SingleSampleMediaSource(subtitleUri, factory,
                    Format.createTextSampleFormat(null, MimeTypes.APPLICATION_SUBRIP, Format.NO_VALUE, null), 0);
            MergingMediaSource mergedSource = new MergingMediaSource(videoSource, subtitleSource);
            player.prepare(mergedSource);
            return true;
        }
        player.prepare(videoSource);
        return true;
    }

    /**
     * Check if subtitle file is exist or not
     *
     * @param uri video file
     * @return return true if the file is exist. return false if the file is not exist.
     */
    private boolean subtitleFileExist(Uri uri) {
        Objects.requireNonNull(uri, "uri cannot be null!!");
        List<String> subtitleSupportFormatList = new ArrayList<>(Arrays.asList("srt", "vtt", "acc"));
        String uriString = uri.toString();
        int numExtension = uriString.lastIndexOf(".");
        if (numExtension < 0) {
            return false;
        }
        //Remove extension
        uriString = uriString.substring(0, numExtension + 1);
        Iterator<String> iterator = subtitleSupportFormatList.iterator();
        while (iterator.hasNext()) {
            //extension removed filepath + subtitle extension = subtitleFilePath
            String subtitleFilePath = uriString + iterator.next();
            Uri subtitleFileUri = Uri.parse(subtitleFilePath);
            File subtitleFile = new File(subtitleFileUri.getPath());
            if (subtitleFile.exists()) {
                setSubtitleFilePath(Uri.parse(subtitleFilePath));
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }
//
//    private void createSurfaceView() {
//        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceview);
//        // SurfaceViewにコールバックを設定
//        mHolder = mSurfaceView.getHolder();
//        mHolder.addCallback(mCallback);
//    }

    private void checkPermission() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // permissionが許可されていません
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Log.d("debug", "checkPermission1");
                // 許可ダイアログで今後表示しないにチェックされていない場合
            }
            Log.d("debug", "checkPermission2");
            // permissionを許可してほしい理由の表示など

            // 許可ダイアログの表示
            // MY_PERMISSIONS_REQUEST_READ_CONTACTSはアプリ内で独自定義したrequestCodeの値
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
        }
        return;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT);

                } else {
                    Toast.makeText(this, "Permission NOT granted", Toast.LENGTH_SHORT);
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    finish();
                }
                return;
            }
        }
    }

    /**
     * 動画ファイルのファイルパスをSPから取得する。
     *
     * @return SPに保存した値がなければnullを返す　値があればそのuriを返す。
     */
    public Uri getVideoFilePath() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String filePath = sharedPreferences.getString(VIDEO_FILE_PATH, "");
        if (filePath.isEmpty()) {
            return null;
        } else {
            //TODO: try-catch?
            return Uri.parse(filePath);
        }
    }

    /**
     * 動画ファイルのファイルパスをSPに保存する。
     *
     * @param mFilePath 動画ファイルのファイルパス
     */
    public void setVideoFilePath(Uri mFilePath) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(VIDEO_FILE_PATH, mFilePath.toString());
        editor.apply();
    }

    /**
     * 字幕ファイルのファイルパスをSPから取得する。
     *
     * @return SPに保存した値がなければnullを返す　値があればそのuriを返す。
     */
    public Uri getSubtitleFilePath() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String filePath = sharedPreferences.getString(SUBTITLE_FILE_PATH, "");
        if (filePath.isEmpty()) {
            return null;
        } else {
            //TODO: try-catch?
            return Uri.parse(filePath);
        }
    }

    /**
     * 字幕ファイルのファイルパスをSPに保存する。
     *
     * @param mFilePath 動画ファイルのファイルパス
     */
    public void setSubtitleFilePath(Uri mFilePath) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SUBTITLE_FILE_PATH, mFilePath.toString());
        editor.apply();
    }

    /**
     * ビデオの現在の再生時間を保持してたら返す。
     *
     * @return　再生時間
     */
    public int getVideoDuration() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getInt(VIDEO_DURATION, 0);
    }

    /**
     * ビデオの現在の再生時間を保持する
     *
     * @param duration 再生時間
     */
    public void setVideoDuration(int duration) {
        if (duration >= 0) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(VIDEO_DURATION, duration);
            editor.apply();
        }
    }

    public void lunchFilePicker() {
        Intent i = new Intent(getApplicationContext(), FilteredFilePickerActivity.class);
        // ここで複数ファイル選択できないようにしている？
        i.putExtra(FilteredFilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        i.putExtra(FilteredFilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilteredFilePickerActivity.EXTRA_MODE, FilteredFilePickerActivity.MODE_FILE);
        i.putExtra(FilteredFilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
        startActivityForResult(i, FILE_CODE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
            // Use the provided utility method to parse the result
            List<Uri> files = Utils.getSelectedFilesFromResult(intent);
//            for (Uri uri: files) {
//                File file = Utils.getFileForUri(uri);
//                // Do something with the result...
//            }
            File file = Utils.getFileForUri(files.get(0));
            Uri videoFile = Uri.fromFile(file);
            releasePlayer();

            if (playVideoFromFileUri(videoFile) != false) {
                setVideoFilePath(videoFile);
            } else {
                Toast.makeText(this, "VideoFilePath is invalid. Chose Video file again.[onActivityResult]", Toast.LENGTH_SHORT);
                lunchFilePicker();
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if ((Util.SDK_INT <= 23 || player == null)) {
            initializePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

}