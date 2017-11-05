package me.yusei.tangoplayer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.TimedText;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
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
import com.nononsenseapps.filepicker.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //    public class MainActivity extends AppCompatActivity  implements Runnable, View.OnClickListener, MediaPlayer.OnTimedTextListener {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 1;
    private static final String VIDEO_FILE_PATH = "VIDEO_FILE_PATH";
    private static final String SUBTITLE_FILE_PATH = "SUBTITLE_FILE_PATH";
    private static final String VIDEO_DURATION = "VIDEO_DURATION";
    private static final int FILE_CODE = 1;
    private static SimpleExoPlayer player;
    private static Handler handler = new Handler();
    private final SurfaceHolder.Callback mCallback = new SurfaceHolder.Callback() {

        /** SurfaceViewが生成された時に呼び出される */
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            String path = getVideoFilePath();
            Log.d("debug", path);
            try {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                // MediaPlayerを生成
                mMediaPlayer = new MediaPlayer();
                // 動画ファイルをMediaPlayerに読み込ませる
                mMediaPlayer.setDataSource(path);
                // 読み込んだ動画ファイルを画面に表示する
                mMediaPlayer.setDisplay(holder);
                mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        mp.start();
                    }
                });
                mMediaPlayer.prepare();

                //字幕の処理
                mMediaPlayer.addTimedTextSource(mShowSubtile.getSubtitleFile(R.raw.test),
                        MediaPlayer.MEDIA_MIMETYPE_TEXT_SUBRIP);
                int textTrackIndex = mShowSubtile.findTrackIndexFor(
                        MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT, mMediaPlayer.getTrackInfo());
                if (textTrackIndex >= 0) {
                    mMediaPlayer.selectTrack(textTrackIndex);
                } else {
                    Log.w("debug", "Cannot find text track!");
                }
                mMediaPlayer.setOnTimedTextListener(MainActivity.this);
                //字幕の処理

                startMedia.setText("pause");
                mSeekBar.setMax(mMediaPlayer.getDuration());
                Log.d("debug", "getDuration: " + getVideoDuration());
                mMediaPlayer.seekTo(getVideoDuration());
                //mSeekBar.setProgress(mMediaPlayer.getCurrentPosition());
                mMediaPlayer.start();
                //スタートされない？
                new Thread(MainActivity.this).start();

            } catch (IllegalArgumentException e) {
                e.printStackTrace();

            } catch (SecurityException e) {
                e.printStackTrace();

            } catch (IllegalStateException e) {
                e.printStackTrace();

            } catch (IOException e) {
                e.printStackTrace();

            } finally {

            }
        }

        //
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        /** SurfaceViewが終了した時に呼び出される */
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            //再生時間を保存する
            Log.d("debug", "checkPermission2");
            if (mMediaPlayer != null) {
                int duration = mMediaPlayer.getCurrentPosition();
                Log.d("debug", "getCurrentPosition:" + duration);
                if (duration != mMediaPlayer.getDuration()) {
                    setVideoDuration(duration);
                } else {
                    setVideoDuration(0);
                }
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }

    };
    private SimpleExoPlayerView mSimpleExoPlayerView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        mShowSubtile = new ShowSubtitle(this);
        setContentView(R.layout.activity_main);
//        mSeekBar = (SeekBar) findViewById(R.id.mediaSeekBar);
//        startMedia = (Button) findViewById(R.id.playButton);
//        fileButton = (Button) findViewById(R.id.fileButton);
//        subtitle = (TextView) findViewById(R.id.subtitle);
//        fileButton.setOnClickListener(this);
//        startMedia.setOnClickListener(this);
//        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                try {
//                    if (mMediaPlayer == null) {
//                        Toast.makeText(getApplicationContext(), "Media is not running",
//                                Toast.LENGTH_SHORT).show();
//                        seekBar.setProgress(0);
//                    }else if (mMediaPlayer.isPlaying() || mMediaPlayer != null) {
//                        if (fromUser) {
//                            Log.d("debug", "seek to progress");
//                            mMediaPlayer.pause();
//                            mMediaPlayer.seekTo(progress);
//                        }
//                    }
//                } catch (Exception e) {
//                    Log.e("seek bar", "" + e);
//                    seekBar.setEnabled(false);
//
//                }
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//
//            }
//        });
//        mSeekBar.setEnabled(true);
//
//        //mSeekBar.setEnabled(false);

        // 1. Create a default TrackSelector
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        // 2. Create the player
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);

        // Bind the player to the view.
        mSimpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.SimpleExoPlayerView);
        mSimpleExoPlayerView.setPlayer(player);

        //書き込み権限確認
        checkPermission();

        if (getVideoFilePath() == null) {
            lunchFilePicker();
        } else {
            while (true) {
                if (prepareExoPlayerFromFileUri(getVideoFilePath()) == false) {
                    Toast.makeText(this, "VideoFilePath is invalid. Chose Video file again.", Toast.LENGTH_SHORT);
                    lunchFilePicker();
                } else {
                    break;
                }
            }
        }
        player.setVideoTextureView(textureView);
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
        uriString = uriString.substring(0, numExtension);
        Iterator<String> iterator = subtitleSupportFormatList.iterator();
        while (iterator.hasNext()) {
            //extension removed filepath + subtitle extension = subtitleFilePath
            String subtitleFilePath = uriString + iterator.next();
            File subtitleFile = new File(subtitleFilePath);
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
        player.release();

    }

    private void createSurfaceView() {
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        // SurfaceViewにコールバックを設定
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(mCallback);
    }

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

            return;
        }
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
        // This always works
        Log.d("debug", "this first?");
        //Intent i = new Intent(getApplicationContext(), FilteredFilePickerActivity.class);
        Intent i = new Intent(getApplicationContext(), FilteredFilePickerActivity.class);

        // This works if you defined the intent filter
        // Intent i = new Intent(Intent.ACTION_GET_CONTENT);

        // Set these depending on your use case. These are the defaults.
//            i.putExtra(FilteredFilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
//            i.putExtra(FilteredFilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
//            i.putExtra(FilteredFilePickerActivity.EXTRA_MODE, FilteredFilePickerActivity.MODE_FILE);
        // ここで複数ファイル選択できないようにしている？
        i.putExtra(FilteredFilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        i.putExtra(FilteredFilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilteredFilePickerActivity.EXTRA_MODE, FilteredFilePickerActivity.MODE_FILE);

        // Configure initial directory by specifying a String.
        // You could specify a String like "/storage/emulated/0/", but that can
        // dangerous. Always use Android's API calls to get paths to the SD-card or
        // internal memory.
        //i.putExtra(FilteredFilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
        i.putExtra(FilteredFilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
        startActivityForResult(i, FILE_CODE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
            // Use the provided utility method to parse the result
            List<Uri> files = Utils.getSelectedFilesFromResult(intent);
            for (Uri uri : files) {
                File file = Utils.getFileForUri(uri);
                //保存してるファイル名と同じときは保存する必要ない　違うときだけ保存と保持している生成時間を初期化
                if (file.getPath().compareTo(getVideoFilePath()) != 0) {
                    setVideoDuration(0);
                    setVideoFilePath(file.getPath());
                }
                if (mMediaPlayer != null) {
                    mMediaPlayer.release();
                    mMediaPlayer = null;
                }
                createSurfaceView();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.equals(startMedia)) {
            if (mMediaPlayer == null) {
                lunchFilePicker();
                mSeekBar.setEnabled(true);
            }
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                startMedia.setText("play");
            } else {
                mMediaPlayer.start();
                startMedia.setText("pause");
            }
        }
        if (v.equals(fileButton)) {
            lunchFilePicker();
        }
    }


    @Override
    public void run() {
        int currentPosition = mMediaPlayer.getCurrentPosition();
        int total = mMediaPlayer.getDuration();

        while (mMediaPlayer != null && currentPosition < total) {
            try {
                //Log.d("debug", "thread run success");
                Thread.sleep(1000);
                currentPosition = mMediaPlayer.getCurrentPosition();
            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                return;
            }
            mSeekBar.setProgress(currentPosition);
        }
    }

    @Override
    public void onTimedText(final MediaPlayer mp, final TimedText text) {
        if (text != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d("debug", "getCurrentPosition");
                    int seconds = mp.getCurrentPosition() / 1000;
                    subtitle.setText("[" + mShowSubtile.secondsToDuration(seconds) + "] "
                            + text.getText());
                }
            });
        }
    }
}