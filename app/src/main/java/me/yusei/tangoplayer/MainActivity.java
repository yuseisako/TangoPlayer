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
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
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
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity implements OnTaskCompleted{
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
        ImageButton file = (ImageButton) findViewById(R.id.select_file);

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
        if (getVideoFilePath() == null || playVideoFromFilePath(getVideoFilePath()) == false) {
            lunchFilePicker();
        }else{
            // when video file path is invalid or no stored video file path => no seek.
            // otherwise (video file path is valid) => seek video to stored duration.
            player.seekTo(getVideoDuration());
        }
    }

    private void releasePlayer() {
        if (player != null) {
            shouldAutoPlay = player.getPlayWhenReady();
            player.release();
            setVideoDuration(player.getCurrentPosition());
            player = null;
            trackSelector = null;
        }
    }

    /**
     * Run background async task: reading srt file, and call back this class.
     */
    private void startReadSrtFileTask(){
        ReadSrtFileTask task = new ReadSrtFileTask(this);
        task.setSubtitleFilePath(getSubtitleFilePath());
        task.setOnCallBack(new ReadSrtFileTask.CallBackTask(){
                @Override
                public void CallBack(TimedTextObject tto){
                    drawSubtitles(tto);
                }
        });
        task.execute(0);
    }

    /**
     * draw subtitles on View. Callback by ReadSrtFileTask.
     * @param timedTextObject
     */
    public void drawSubtitles(TimedTextObject timedTextObject){
        TreeMap<Integer, Caption> captionTreeMap = timedTextObject.captions;
        Iterator<Integer> it = captionTreeMap.keySet().iterator();
        ListView listView = (ListView)findViewById(R.id.subtitles);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.subtitles_col);
        while (it.hasNext()){
            Integer integer = it.next();
            Caption caption = captionTreeMap.get(integer);
            String content = caption.content;
            //TODO Replace below to treemap custom adapter if this need more flexibility.
            //https://stackoverflow.com/questions/18532850/treemap-to-listview-in-android
            arrayAdapter.add(content);
        }
        if(arrayAdapter.getCount() > 0){
            listView.setAdapter(arrayAdapter);
        }
    }

    /**
     * Play Video From uri and return result
     *
     * @param filePath
     * @return true if uri is valid and playable. false if uri is null or invalid.
     */
    private boolean playVideoFromFilePath(@NonNull String filePath) {
        if (filePath == null) {
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
        mSimpleExoPlayerView.setControllerHideOnTouch(false);

        // Bind the player to the view.
        mSimpleExoPlayerView.setPlayer(player);

        if (prepareExoPlayerFromFilePath(filePath) == false) {
            Toast.makeText(this, "VideoFilePath is invalid. Chose Video file again.[playVideoFromFilePath]", Toast.LENGTH_SHORT);
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
     * @param filePath
     * @return true if prepare completed. false if prepare is not completed.
     */
    private boolean prepareExoPlayerFromFilePath(@NonNull String filePath) {
        Uri uri = Uri.fromFile(new File(Utility.nonNull(filePath)));
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
        if (isSubtitleFileExist(filePath) == true) {
            //call this to read subtitles, this method will callback this activity's "drawSubtitles" method.
            startReadSrtFileTask();

            // https://google.github.io/ExoPlayer/doc/reference/com/google/android/exoplayer2/Format.html#sampleMimeType
            String subtitleFilePath = getSubtitleFilePath();
            MediaSource subtitleSource = new SingleSampleMediaSource(Uri.fromFile(new File(subtitleFilePath)), factory,
                    Format.createTextSampleFormat(null, MimeTypes.APPLICATION_SUBRIP, Format.NO_VALUE, null), 0);
            MergingMediaSource mergedSource = new MergingMediaSource(videoSource, subtitleSource);
            player.prepare(mergedSource);
            return true;
        }else{
            ListView listView = (ListView)findViewById(R.id.subtitles);
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.subtitles_col);
            arrayAdapter.add("Subtitles NOT FOUND.");
            arrayAdapter.add("It should be same file name as video file plus extension.");
            listView.setAdapter(arrayAdapter);
        }
        player.prepare(videoSource);
        return true;
    }

    /**
     * Check if subtitle file is exist or not
     *
     * @param filePath video file
     * @return return true if the file is exist. return false if the file is not exist.
     */
    private boolean isSubtitleFileExist(@NonNull String filePath) {
        //null check
        Utility.nonNull(filePath);

        List<String> subtitleSupportFormatList = new ArrayList<>(Arrays.asList("srt", "vtt", "acc"));
        int numExtension = filePath.lastIndexOf(".");
        if (numExtension < 0) {
            return false;
        }
        //Remove extension
        filePath = filePath.substring(0, numExtension + 1);
        Iterator<String> iterator = subtitleSupportFormatList.iterator();
        while (iterator.hasNext()) {
            //extension removed filepath + subtitle extension = subtitleFilePath
            String subtitleFilePath = filePath + iterator.next();
            File subtitleFile = new File(subtitleFilePath);
            if (subtitleFile.exists()) {
                //TODO:subtitleFileUri is better?
                setSubtitleFilePath(subtitleFilePath);
                //start reading srt file async task
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

    private void checkPermission() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // need to get permission
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Log.d("debug", "checkPermission1");
                // In the case of no-checking to "Never ask again"
            }
            Log.d("debug", "checkPermission2");
            // Describe permission reason.

            // Show permission dialog.
            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is requestCode uniquely defined within this app by your own.
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
     * return video file path from SP.
     * @return video file path or null if nothing.
     */
    public String getVideoFilePath() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String filePath = sharedPreferences.getString(VIDEO_FILE_PATH, "");
        if (filePath.isEmpty()) {
            return null;
        } else {
            //TODO: try-catch?
            return filePath;
        }
    }

    /**
     * set video file path to SP.
     * @param mFilePath video file path
     */
    private void setVideoFilePath(String mFilePath) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(VIDEO_FILE_PATH, mFilePath);
        editor.apply();
    }

    /**
     * 字幕ファイルのファイルパスをSPから取得する。
     *
     * @return SPに保存した値がなければnullを返す　値があればそのuriを返す。
     */
    public String getSubtitleFilePath() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String filePath = sharedPreferences.getString(SUBTITLE_FILE_PATH, "");
        if (filePath.isEmpty()) {
            return null;
        } else {
            //TODO: try-catch?
            return filePath;
        }
    }

    /**
     * 字幕ファイルのファイルパスをSPに保存する。
     *
     * @param mFilePath 動画ファイルのファイルパス
     */
    private void setSubtitleFilePath(String mFilePath) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SUBTITLE_FILE_PATH, mFilePath);
        editor.apply();
    }

    /**
     * ビデオの現在の再生時間を保持してたら返す。
     *
     * @return long duration
     */
    public long getVideoDuration() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getLong(VIDEO_DURATION, 0);
    }

    /**
     * ビデオの現在の再生時間を保持する
     *
     * @param duration 再生時間
     */
    public void setVideoDuration(long duration) {
        if (duration >= 0) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(VIDEO_DURATION, duration);
            editor.apply();
        }
    }

    public void lunchFilePicker() {
        setVideoDuration(0);
        Intent i = new Intent(this, FilteredFilePickerActivity.class);
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
            releasePlayer();

            if (playVideoFromFilePath(file.toString()) != false) {
                setVideoFilePath(file.toString());
                setVideoDuration(0);
            } else {
                Toast.makeText(this, "VideoFilePath is invalid. Chose Video file again.[onActivityResult]", Toast.LENGTH_SHORT);
                lunchFilePicker();
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (player == null) {
            initializePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(player != null){
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if(player != null){
            releasePlayer();
        }
    }

    @Override
    public void onTaskCompleted() {

    }
}