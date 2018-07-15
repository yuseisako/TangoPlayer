package me.yusei.tangoplayer;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nex3z.flowlayout.FlowLayout;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.yusei.tangoplayer.anki.AnkiDroidController;
import me.yusei.tangoplayer.anki.AnkiDroidHelper;
import me.yusei.tangoplayer.subtitle.Caption;
import me.yusei.tangoplayer.subtitle.Strings;
import me.yusei.tangoplayer.subtitle.TimedTextObject;
import me.yusei.tangoplayer.task.ReadSubtitleFileTask;
import me.yusei.tangoplayer.task.ReadSubtitleFileTaskCallback;
import me.yusei.tangoplayer.task.TranslateTask;
import me.yusei.tangoplayer.task.TranslateTaskCallback;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

public class VideoPlayerActivity extends AppCompatActivity implements IVLCVout.Callback , LibVLC.HardwareAccelerationError{
    private static final int MY_PERMISSIONS_REQUEST_WRITE_ANKI_DECK = 2;
    private static final int REWIND_FFWD_UNIT = 5000;
    private static final int SUBTITLE_DELAY_UNIT = 250;
    private static final int VIDEO_INFO_HIDE_TIME = 3000;
    private static final int SUBTITLE_DELAY_CONTROLLER_HIDE_TIME = 5000;
    private static final String ANKI_DROID_PACKAGE_NAME = "com.ichi2.anki";

    //display surface
    private SurfaceHolder mSurfaceHolder;
    private SurfaceView mSurfaceView;
    private SurfaceView mSubtitlesSurface = null;

    // media player
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private Handler scrollSubtitleHandler = new Handler();
    private Handler subtitleDelayControllerHandler = new Handler();
    private Handler videoInfoHandler = new Handler();
    private Handler drawOrverlayProgressHandler =  new Handler();
    private int mVideoWidth;
    private int mVideoHeight;
    private TextView subtitleTextView;

    //Overlay Video Controller
    ImageButton settingButton;
    ImageButton rewindButton;
    ImageButton playPauseButton;
    ImageButton fastForwardButton;
    ImageButton fileButton;
    ImageButton subtitleFastForward;
    ImageButton subtitleRewind;
    TextView timeCurrentTextView;
    SeekBar progressSeekBar;
    TextView timeTotalTextView;
    TextView videoSurfaceInfo;
    boolean mDisplayRemainingTime = false;
    boolean mDragging;
    private int subtitleDelay = 0;
    private boolean isShowSubtitleDelayController = false;

    private TimedTextObject timedTextObject = new TimedTextObject();
    Runnable scrollSubtitleRunnable;
    private boolean mBackPressed;
    AlertDialog ankiDialog;
    private AnkiDroidHelper mAnkiDroidHelper;
    private AnkiDroidController mAnkiDroidController;
    private static final String[] WORDS_REPLACE_LIST = {",", "- ", ".", "[", "]", "\"" };
    private static final String[] SUPPORTED_SUBTITLE_EXTENSION = {"srt", "ass", "scc", "ttml"};

    /* ===============================================================
       Application Lifecycle
     =============================================================== */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Receive path to play from intent
        Intent intent = getIntent();
        //String videoFilePath = intent.getExtras().getString(VideoPlayerConfig.LOCATION);
        String videoFilePath = intent.getStringExtra(VideoPlayerConfig.LOCATION);
        if(videoFilePath == null){
            videoFilePath = VideoPlayerConfig.getVideoFilePath(this);
        }
        if ( videoFilePath == null || ! initPlayer(videoFilePath)) {
            releasePlayer();
            intent = new Intent(this, StartActivity.class);
            startActivity(intent);
        }else{
            mAnkiDroidHelper = new AnkiDroidHelper(this);
            mAnkiDroidController = new AnkiDroidController(this, mAnkiDroidHelper);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        releasePlayer();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mSurfaceView != null) {
            if (mBackPressed) {
                releasePlayer();
            }
        }
    }

    @Override
    protected void onDestroy() {
        //called this method when resume app
        super.onDestroy();
        releasePlayer();
    }

    @Override
    public void onBackPressed() {
        mBackPressed = true;
        super.onBackPressed();
    }



    /* ===============================================================
       Task
     =============================================================== */

    private void startTranslateTask(@NonNull final EditText editTextWordMeaning, String word){
        TranslateTask task = new TranslateTask(new TranslateTaskCallback() {
            @Override
            public void onPreExecute() {
                //do nothing.
            }

            @Override
            public void onPostExecute(String result) {
                String hint = editTextWordMeaning.getHint().toString();
                if (result != null && !result.isEmpty()) {
                    editTextWordMeaning.setText(result);

                    if(hint.contains("Word")){
                        editTextWordMeaning.setHint(getResources().getString(R.string.ankidialog_hint_word_meaning));
                    }else {
                        editTextWordMeaning.setHint(getResources().getString(R.string.ankidialog_hint_sentence_meaning));
                    }
                }else{
                    if(hint.contains("Word")){
                        editTextWordMeaning.setHint(getResources().getString(R.string.subtitle_task_failed_word));
                    }else {
                        editTextWordMeaning.setHint(getResources().getString(R.string.subtitle_task_failed_sentence));
                    }
                }
            }

            @Override
            public void onProgressUpdate(int progress) {
                //do nothing.
            }

            @Override
            public void onCancelled() {
                //do nothing.
            }
        });

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String language = sharedPreferences.getString(getResources().getString(R.string.key_list_translation_language), getResources().getString(R.string.pref_translation_language_default));

        task.execute(language, word);
    }

    /**
     * Run background async task: reading srt file, and call back this class.
     */
    private void startReadSrtFileTask(){
        ReadSubtitleFileTask task = new ReadSubtitleFileTask(new ReadSubtitleFileTaskCallback() {
            @Override
            public void onPreExecute() {
                //do nothing.
            }

            @Override
            public void onPostExecute(TimedTextObject parsedTimedTextObject) {
                if(parsedTimedTextObject == null ||
                        parsedTimedTextObject.captions == null || parsedTimedTextObject.captions.size() == 0){
                    return;
                }
                drawSubtitles(parsedTimedTextObject);
                scrollSubtitle(parsedTimedTextObject);
            }

            @Override
            public void onProgressUpdate(int progress) {
                //do nothing.
            }

            @Override
            public void onCancelled() {
                //do nothing.
            }
        });
        task.setSubtitleFilePath(VideoPlayerConfig.getSubtitleFilePath(this));
        task.execute(timedTextObject);
    }

    /* ===============================================================
       UI
     =============================================================== */

    /* ============================
      Media Player Controller
     ============================ */

    private void initControllerView(){
        settingButton = findViewById(R.id.setting);
        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
                startActivity(intent);
            }
        });
        settingButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(getBaseContext(), getResources().getString(R.string.activity_settings), Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        rewindButton = findViewById(R.id.rew);
        rewindButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mMediaPlayer.getTime() - REWIND_FFWD_UNIT > 0) {
                    mMediaPlayer.setTime(mMediaPlayer.getTime() - REWIND_FFWD_UNIT);
                    showVideoSurfaceInfo(getResources().getString(R.string.video_surface_info_seek_rewind) +
                            REWIND_FFWD_UNIT/1000 + getResources().getString(R.string.video_surface_info_second_unit));
                }
            }
        });

        playPauseButton = findViewById(R.id.playpause);
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatePausePlay();
            }
        });

        fastForwardButton = findViewById(R.id.ffwd);
        fastForwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mMediaPlayer.getTime() + REWIND_FFWD_UNIT < mMediaPlayer.getLength()){
                    mMediaPlayer.setTime(mMediaPlayer.getTime() + REWIND_FFWD_UNIT);
                    showVideoSurfaceInfo(getResources().getString(R.string.video_surface_info_seek_fast_forward) +
                            REWIND_FFWD_UNIT/1000 + getResources().getString(R.string.video_surface_info_second_unit));
                }
            }
        });

        fileButton = findViewById(R.id.file);
        fileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                releasePlayer();
                VideoPlayerConfig.setVideoFilePath(getBaseContext(), null);
                Intent intent = new Intent(getBaseContext(), StartActivity.class);
                startActivity(intent);
                finish();
            }
        });
        fileButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(getBaseContext(), getResources().getString(R.string.media_player_controller_open_file), Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        subtitleFastForward = findViewById(R.id.subtitle_ffwd);
        subtitleFastForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                subtitleDelay = subtitleDelay + SUBTITLE_DELAY_UNIT;
                showSubtitleDelayController();
                showVideoSurfaceInfo(getResources().getString(R.string.video_surface_info_subtitle_delay) + ((float)subtitleDelay)/1000 + "s");
            }
        });

        subtitleRewind = findViewById(R.id.subtitle_rew);
        subtitleRewind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                subtitleDelay = subtitleDelay - SUBTITLE_DELAY_UNIT;
                showSubtitleDelayController();
                showVideoSurfaceInfo(getResources().getString(R.string.video_surface_info_subtitle_delay) + ((float)subtitleDelay)/1000 + "s");
            }
        });

        timeCurrentTextView = findViewById(R.id.time_current);
        progressSeekBar = findViewById(R.id.mediacontroller_progress);
        progressSeekBar.setOnSeekBarChangeListener(mSeekListener);

        timeTotalTextView = findViewById(R.id.time);

        subtitleTextView = findViewById(R.id.subtitleTextView);
        subtitleTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                isShowSubtitleDelayController = !isShowSubtitleDelayController;
                if(isShowSubtitleDelayController){
                    showSubtitleDelayController();
                    showVideoSurfaceInfo(getResources().getString(R.string.video_surface_info_delay_controller_enable));
                }else {
                    hideSubtitleDelayController(0);
                    showVideoSurfaceInfo(getResources().getString(R.string.video_surface_info_delay_controller_disable));
                }
                return true;
            }
        });

        videoSurfaceInfo = findViewById(R.id.videoSurfaceInfo);
    }

    private void updatePausePlay() {
        if (playPauseButton == null || mMediaPlayer == null) {
            return;
        }

        if (mMediaPlayer.isPlaying()) {
            playPauseButton.setImageResource(R.drawable.ic_media_play);
            mMediaPlayer.pause();
            showVideoSurfaceInfo(getResources().getString(R.string.video_surface_info_pause));
        } else {
            playPauseButton.setImageResource(R.drawable.ic_media_pause);
            mMediaPlayer.play();
            showVideoSurfaceInfo(getResources().getString(R.string.video_surface_info_play));
        }
    }

    private void updatePlay(){
        if (playPauseButton == null || mMediaPlayer == null || mMediaPlayer.isPlaying()) {
            return;
        }

        if(!mMediaPlayer.isPlaying()){
            playPauseButton.setImageResource(R.drawable.ic_media_pause);
            mMediaPlayer.play();
            showVideoSurfaceInfo(getResources().getString(R.string.video_surface_info_play));
        }
    }

    private void updatePause(){
        if (playPauseButton == null || mMediaPlayer == null) {
            return;
        }

        if(mMediaPlayer.isPlaying()){
            playPauseButton.setImageResource(R.drawable.ic_media_play);
            mMediaPlayer.pause();
            showVideoSurfaceInfo(getResources().getString(R.string.video_surface_info_pause));
        }
    }

    /**
     * handle changes of the seekbar (slicer)
     */
    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mDragging = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mDragging = false;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && mMediaPlayer.isSeekable()) {
                mMediaPlayer.setTime(progress);
                setOverlayProgress();
                timeCurrentTextView.setText(Strings.millisToString(progress));

                //showInfo(Strings.millisToString(progress));
            }

        }
    };

    /**
     * update the overlay
     */
    private void setOverlayProgress() {
        if (mMediaPlayer == null) {
            return;
        }
        int time = (int) mMediaPlayer.getTime();
        int length = (int) mMediaPlayer.getLength();
        progressSeekBar.setMax(length);
        progressSeekBar.setProgress(time);

        if (time >= 0) timeCurrentTextView.setText(Strings.millisToString(time));
        if (length >= 0) timeTotalTextView.setText(mDisplayRemainingTime && length > 0
                ? "- " + Strings.millisToString(length - time)
                : Strings.millisToString(length));
    }

    private void showSubtitleDelayController(){
        if(subtitleRewind == null || subtitleFastForward == null || mMediaPlayer == null || ! isShowSubtitleDelayController){
            return;
        }

        subtitleRewind.setVisibility(View.VISIBLE);
        subtitleFastForward.setVisibility(View.VISIBLE);

        hideSubtitleDelayController(SUBTITLE_DELAY_CONTROLLER_HIDE_TIME);
    }

    private void hideSubtitleDelayController(long delayMillis){
        subtitleDelayControllerHandler.removeCallbacksAndMessages(null);

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if(subtitleRewind != null && subtitleFastForward != null && mMediaPlayer != null){
                    subtitleRewind.setVisibility(View.INVISIBLE);
                    subtitleFastForward.setVisibility(View.INVISIBLE);
                }
            }
        };
        subtitleDelayControllerHandler.postDelayed(runnable, delayMillis);
    }

    private void showVideoSurfaceInfo(@NonNull String text){
        if(subtitleRewind == null || mMediaPlayer == null){
            return;
        }

        videoSurfaceInfo.setText(text);
        videoSurfaceInfo.setVisibility(View.VISIBLE);

        hideVideoSurfaceInfo();
    }

    private void hideVideoSurfaceInfo(){
        videoInfoHandler.removeCallbacksAndMessages(null);

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if(subtitleRewind != null && mMediaPlayer != null){
                    videoSurfaceInfo.setText("");
                    videoSurfaceInfo.setVisibility(View.INVISIBLE);
                }
            }
        };
        videoInfoHandler.postDelayed(runnable, VIDEO_INFO_HIDE_TIME);
    }


    /* ============================
      Subtitle
     ============================ */

    /**
     * Scroll subtitle to current video position.
     * Loop trigger has to be drawSubtitles method.
     */
    public void scrollSubtitle(@NonNull final TimedTextObject timedTextObject){
        final ListView listView = findViewById(R.id.subtitleListView);
  //      int size = timedTextObject.captions.size();
 //       final int lastTime = timedTextObject.captions.getValue(size - 1 ).end.getMseconds();

        scrollSubtitleRunnable = new Runnable() {

            List<Caption> subtitles = new ArrayList<>(timedTextObject.captions.values());

            @Override
            public void run() {
                if(mMediaPlayer != null && mMediaPlayer.isPlaying()){
                    float currentPos = mMediaPlayer.getTime();
                    int index = 0;

                    for (Caption caption : subtitles) {
                        if (currentPos >= caption.start.mseconds - subtitleDelay
                                && currentPos <= caption.end.mseconds - subtitleDelay) {
                            listView.setItemChecked(index,true);
                            int height = listView.getHeight();
                            listView.setSelectionFromTop(index, height/2);
                            setSubtitleTextView(caption);
                            break;
                        } else {
                            setSubtitleTextView(null);
                            if(currentPos < caption.end.mseconds - subtitleDelay){
                                break;
                            }
                        }
                        index++;
                    }
                }

                scrollSubtitleHandler.postDelayed(this, 300);
            }
        };
        scrollSubtitleHandler.post(scrollSubtitleRunnable);
    }

    @SuppressWarnings("deprecation")
    public void setSubtitleTextView(Caption caption) {
        if(subtitleTextView == null){
            return;
        }
        if (caption == null) {
            subtitleTextView.setText("");
            return;
        }
        String line = caption.content.replace("<br />", " ");
        if( ( line.contains("<b>") && line.contains("</b>") ) ||
                ( line.contains("<i>") && line.contains("</i>") ) ||
                ( line.contains("<font color=") && line.contains("</font>") ) ){
            Spanned durationSpanned;
            if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                durationSpanned = Html.fromHtml(line,Html.FROM_HTML_MODE_LEGACY);
            } else {
                durationSpanned = Html.fromHtml(line);
            }
            subtitleTextView.setText(durationSpanned);
        }else {
            subtitleTextView.setText(line);
        }
    }

    public String removeSubtitleTag(@NonNull String subtitle){
        //srt format tags are according to https://www.visualsubsync.org/help/srt
        if(subtitle.contains("<b>") && subtitle.contains("</b>")){
            subtitle = subtitle.replaceAll("<b>", "").replaceAll("</b>", "");
        }
        if(subtitle.contains("<i>") && subtitle.contains("</i>")){
            subtitle = subtitle.replaceAll("<i>", "").replaceAll("</i>", "");
        }
        if(subtitle.contains("<u>") && subtitle.contains("</u>")){
            subtitle = subtitle.replaceAll("<u>", "").replaceAll("</u>", "");
        }
        if(subtitle.contains("<font color=") && subtitle.contains("</font>")){
            subtitle = subtitle.replaceAll("<font color=\"#......\">", "").
                    replaceAll("</font>","").
                    replaceAll("<font color=#......>","").
                    replaceAll("<font color=\"red\">", "").
                    replaceAll("<font color=\"green\">", "").
                    replaceAll("<font color=\"blue\">", "");
        }
        return subtitle;
    }

    private void drawOverlayProgress(){
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if(mMediaPlayer != null && mMediaPlayer.isPlaying()){
                    if(! mDragging){
                        setOverlayProgress();
                    }
                }
                drawOrverlayProgressHandler.postDelayed(this, 300);
            }
        };
        drawOrverlayProgressHandler.post(runnable);

    }

    /**
     * draw subtitles on View.
     * @param timedTextObject subtitles object
     */
    public void drawSubtitles(@NonNull TimedTextObject timedTextObject){
        final ListView listView = findViewById(R.id.subtitleListView);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.subtitles_col);
        Collection<Caption> subtitles = timedTextObject.captions.values();
        final ArrayList<Integer> startTimeList = new ArrayList<>();
        for (Caption caption : subtitles) {
            String line =  caption.content.replace("<br />", " ");
            arrayAdapter.add(removeSubtitleTag(line));
            startTimeList.add(caption.start.mseconds);
        }
        if(arrayAdapter.getCount() > 0){
            listView.setAdapter(arrayAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    if(mMediaPlayer != null) {
                        int captionMiliSec = startTimeList.get(position);
                        mMediaPlayer.setTime(captionMiliSec);
                        updatePlay();
                    }
                }
            });

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id){
                    if(mMediaPlayer !=null) {
                        String content = (String) listView.getItemAtPosition(position);
                        updatePause();
                        // Request permission to access API if required
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (mAnkiDroidHelper.shouldRequestPermission()) {
                                mAnkiDroidHelper.requestPermission(VideoPlayerActivity.this, MY_PERMISSIONS_REQUEST_WRITE_ANKI_DECK);
                                return true;
                            }
                        }
                        showAnkiDialog(content);
                    }
                    return true;
                }
            });
        }
    }

    /* ============================
      Anki Dialog
     ============================ */

    private void showAnkiDialog(@NonNull  final String sentence){
        if(sentence.isEmpty()){
            return;
        }
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        final View layout = inflater.inflate(R.layout.show_anki_dialog,
                (ViewGroup) findViewById(R.id.show_anki_dialog));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.ankidialog_title));
        builder.setPositiveButton(getResources().getString(R.string.ankidialog_add_card), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //Do nothing here because we override this button later to change the close behaviour.
                //However, we still need this because on older versions of Android unless we
                //pass a handler the button doesn't get instantiated
            }
        });

        builder.setNegativeButton(getResources().getString(R.string.ankidialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                updatePlay();
            }
        });

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                updatePlay();
            }
        });

        builder.setView(layout);
        ankiDialog = builder.create();
        ankiDialog.show();
        ankiDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Boolean isAnkiDroidInstalled = ankiDroidInstalledOrNot();
                if( ! isAnkiDroidInstalled){
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_msg_anki_not_installed), Toast.LENGTH_LONG).show();
                    ankiDialog.dismiss();
                    updatePlay();
                }else{
                    EditText wordEditText = layout.findViewById(R.id.add_word);
                    EditText wordMeaningEditText = layout.findViewById(R.id.add_word_meaning);
                    EditText sentenceMeaningEditText = layout.findViewById(R.id.add_sentence_meaning);

                    String word  = wordEditText.getText().toString();
                    String wordMeaning  = wordMeaningEditText.getText().toString();
                    String sentenceMeaning = sentenceMeaningEditText.getText().toString();

                    if(!word.isEmpty() && !wordMeaning.isEmpty() && !sentenceMeaning.isEmpty()) {
                        // Add all data using AnkiDroid provider
                        List<Map<String, String>> cardContentsList = new ArrayList<>();
                        Map<String, String> cardContents = new HashMap<>();
                        cardContents.put(AnkiDroidController.FIELDS[0], word);
                        cardContents.put(AnkiDroidController.FIELDS[1], wordMeaning);
                        cardContents.put(AnkiDroidController.FIELDS[2], sentence);
                        cardContents.put(AnkiDroidController.FIELDS[3], sentenceMeaning);
                        cardContentsList.add(cardContents);

                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        String deckName = sharedPreferences.getString(getResources().getString(R.string.key_list_deck_name), getResources().getString(R.string.pref_deck_name_default));
                        if(deckName.equals(getResources().getString(R.string.pref_deck_name_default))){
                            String videoFileName = VideoPlayerConfig.getVideoFileName(getApplicationContext());
                            if(videoFileName != null){
                                deckName = videoFileName.substring(0, videoFileName.lastIndexOf("."));
                            }
                        }else {
                            deckName = "Tango Player";
                        }
                        mAnkiDroidController.addCardsToAnkiDroid(deckName, cardContentsList, getTranslationLanguage());
                        ankiDialog.dismiss();
                        updatePlay();
                    }else{
                        if(word.isEmpty())
                            wordEditText.setError(getResources().getString(R.string.warn_msg_empty_input));
                        if(wordMeaning.isEmpty())
                            wordMeaningEditText.setError(getResources().getString(R.string.warn_msg_empty_input));
                        if(sentenceMeaning.isEmpty())
                            sentenceMeaningEditText.setError(getResources().getString(R.string.warn_msg_empty_input));
                    }
                }
            }
        });

        ImageButton translateButton = layout.findViewById(R.id.translate);
        translateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText wordEditText = layout.findViewById(R.id.add_word);
                String word  = wordEditText.getText().toString();

                EditText editTextWordMeaning = layout.findViewById(R.id.add_word_meaning);

                String sentence = ((TextView) layout.findViewById(R.id.add_sentence)).getText().toString();
                EditText editTextSentenceMeaning = layout.findViewById(R.id.add_sentence_meaning);

                if(!word.isEmpty()){
                    startTranslateTask(editTextWordMeaning, word);
                    startTranslateTask(editTextSentenceMeaning, sentence);
                }else{
                    wordEditText.setError(getResources().getString(R.string.warn_msg_empty_input));
                }
            }
        });

        ImageButton searchButton = layout.findViewById(R.id.search);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String word = ((EditText)(layout.findViewById(R.id.add_word))).getText().toString();
                Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                intent.putExtra(SearchManager.QUERY, word);
                startActivity(intent);
            }
        });

        TextView sentenceTextView = layout.findViewById(R.id.add_sentence);
        sentenceTextView.setText(sentence);

        FlowLayout wordButtons = layout.findViewById(R.id.word_buttons);
        EditText wordsFrontEditText = layout.findViewById(R.id.add_word);
        addWordButton(wordButtons, sentence, wordsFrontEditText);
    }

    private void addWordButton(FlowLayout view, String words, final EditText frontWords){
        if(view == null || words.isEmpty() || frontWords == null){
            return;
        }

        for(String string: WORDS_REPLACE_LIST){
            words = words.replace(string, "");
        }
        String[] splitWords = words.split(" ");

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(10,5,10,5);

        for(String splitWord : splitWords){
            if(splitWord.isEmpty() || !splitWord.matches(".*[a-zA-Z].*"))
                continue;

            final TextView word = new TextView(this);
            word.setText(splitWord);
            word.setBackground(ContextCompat.getDrawable(this, R.drawable.frame_style));
            word.setPadding(15, 10, 15, 10);
            word.setLayoutParams(params);
            word.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(word.getText() != null){
                        String clickedWord = word.getText().toString();
                        view.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.frame_style_clicked));

                        String setWord;
                        if(frontWords.getText().toString().isEmpty()) {
                            setWord = clickedWord;
                        }else{
                            setWord = frontWords.getText().toString() + " " + clickedWord;
                        }
                        frontWords.setText(setWord);
                    }
                }
            });
            view.addView(word);
        }
    }

    private boolean ankiDroidInstalledOrNot() {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(ANKI_DROID_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private String getTranslationLanguage(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String languageCode = sharedPreferences.getString(getResources().getString(R.string.key_list_translation_language), getResources().getString(R.string.pref_translation_language_default));
        String[] languageCodes = getResources().getStringArray(R.array.pref_translation_language_code);
        String[] languages = getResources().getStringArray(R.array.pref_translation_language);
        for(int i=0; i<languageCodes.length ; i++){
            if(languageCode.compareTo(languageCodes[i]) == 0){
                return languages[i];
            }
        }
        return "";
    }

    /* ============================
       Other
     ============================ */
    private void showTutorial(){
        String SHOWCASE_ID = "videoPlayerActivity";
        String FIRST_RUN = "firstRun";

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean isFirstRun = sharedPreferences.getBoolean(FIRST_RUN, true);
        if (isFirstRun) {
            sharedPreferences.edit().putBoolean(FIRST_RUN, false).apply();
        }else {
            return;
        }

        ListView listView = findViewById(R.id.subtitleListView);
        SurfaceView videoSurface = findViewById(R.id.video_view);
        if(mMediaPlayer == null || listView == null || videoSurface == null){
            return;
        }

        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(200);

        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this, SHOWCASE_ID);

        sequence.setConfig(config);

        sequence.addSequenceItem(listView,
                getResources().getString(R.string.tutorial_subtitle_list_view), getResources().getString(R.string.button_next));

        sequence.addSequenceItem(subtitleTextView,
                getResources().getString(R.string.tutorial_video_dilay_controller), getResources().getString(R.string.button_next));

        sequence.addSequenceItem(videoSurface,
                getResources().getString(R.string.tutorial_video_surface), getResources().getString(R.string.button_ok));

        sequence.start();
    }

    /* ===============================================================
       Player
     =============================================================== */

    /* ============================
       Surface
     ============================ */

    private void setSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        if (mVideoWidth * mVideoHeight <= 1)
            return;

        if(mSurfaceHolder == null || mSurfaceView == null)
            return;

        // get screen size
        int w = getWindow().getDecorView().getWidth();
        int h = getWindow().getDecorView().getHeight();

        // getWindow().getDecorView() doesn't always take orientation into
        // account, we have to correct the values
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (w > h && isPortrait || w < h && !isPortrait) {
            int i = w;
            w = h;
            h = i;
        }

        float videoAR = (float) mVideoWidth / (float) mVideoHeight;
        float screenAR = (float) w / (float) h;

        if (screenAR < videoAR)
            h = (int) (w / videoAR);
        else
            w = (int) (h * videoAR);

        // force surface buffer size
        mSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);

        // set display size
        ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
        lp.width = w;
        lp.height = h;
        mSurfaceView.setLayoutParams(lp);
        mSurfaceView.invalidate();
    }

    /**
     * Play Video From uri and return result
     *
     * @param filePath filePath for video file.
     * @return true if uri is valid and playable. false if uri is null or invalid.
     */
    private boolean initPlayer(@NonNull String filePath) {
        //init UI
        mSurfaceView = findViewById(R.id.video_view);
        mSurfaceHolder = mSurfaceView.getHolder();

        Boolean isSeek = false;
        String videoFilePath = VideoPlayerConfig.getVideoFilePath(this);
        if(videoFilePath != null && filePath.compareTo(videoFilePath) == 0){
            isSeek = true;
        }

        if(createPlayer(filePath, isSeek)){
            VideoPlayerConfig.setVideoFilePath(this, filePath);
            VideoPlayerConfig.setAccessedVideoFilePath(getBaseContext(), filePath);
        }else {
            return false;
        }

        initControllerView();
        drawOverlayProgress();

        if (isSubtitleFileExist(filePath)) {
            startReadSrtFileTask();
        }else{
            ListView listView = findViewById(R.id.subtitleListView);
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.subtitles_col);
            arrayAdapter.add(getResources().getString(R.string.warn_msg_subtitle_not_found1));
            arrayAdapter.add(getResources().getString(R.string.warn_msg_subtitle_not_found2));
            listView.setAdapter(arrayAdapter);
        }
        showTutorial();

        return true;
    }

    private Boolean createPlayer(String media, Boolean isSeek) {
        releasePlayer();
        try {
            // Create LibVLC
            ArrayList<String> options = new ArrayList<>();
            //options.add("--subsdec-encoding <encoding>");
            options.add("--aout=opensles");
            options.add("--audio-time-stretch"); // time stretching
            options.add("--no-spu"); //no subtitle by LibVLC
            options.add("--play-and-pause"); //pause at last frame
            options.add("-vvv"); // verbosity
//            options.add("--http-reconnect");
//            options.add("--network-caching="+6*1000);
            libvlc = new LibVLC(options);
            libvlc.setOnHardwareAccelerationError(this);
            mSurfaceHolder.setKeepScreenOn(true);

            // Create media player
            mMediaPlayer = new MediaPlayer(libvlc);
            mMediaPlayer.setEventListener(mPlayerListener);

            // Set up video output
            final IVLCVout vout = mMediaPlayer.getVLCVout();
            vout.setVideoView(mSurfaceView);
            if(mSubtitlesSurface != null){
                vout.setSubtitlesView(mSubtitlesSurface);
            }
            vout.addCallback(this);
            vout.attachViews();

            mSurfaceView = findViewById(R.id.video_view);

            Media m = new Media(libvlc, Uri.parse("file://" + media));
            m.parse();
            //TODO wait until parse finished, then check if media is parsed
            if(m.getDuration() <= 0){
                m.release();
                return false;
            }
            m.setHWDecoderEnabled(true, false);
            mMediaPlayer.setMedia(m);
            m.release();

            if(ankiDialog == null || !ankiDialog.isShowing()){
                mMediaPlayer.play();
            }

            if(playPauseButton != null){
                playPauseButton.setImageResource(R.drawable.ic_media_pause);
            }

//            getLength() and getTime() always returns -1
//            if(mMediaPlayer.getTime() >= mMediaPlayer.getLength()){
//                isSeek = false;
//            }

            if(isSeek){
                //While return -1 from getTime(), you cannot seek video. It happen only right after load media.
                final Handler handler = new Handler();
                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        if(mMediaPlayer.getTime() > 0){
                            mMediaPlayer.setTime(VideoPlayerConfig.getVideoPosition(getBaseContext()));
                            subtitleDelay = VideoPlayerConfig.getVideoDelay(getBaseContext());
                        }else{
                            handler.postDelayed(this, 100);
                        }
                    }
                };
                handler.post(r);
            }
        } catch (Exception e) {
            Toast.makeText(this, getResources().getString(R.string.error_msg_exception_create_player) + e.toString(), Toast.LENGTH_LONG).show();
            return false;
        }

        mSurfaceView.getRootView().setOnTouchListener(new View.OnTouchListener() {
            long lastTouchedTime = 0;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    lastTouchedTime = System.currentTimeMillis();
                    Float posX = event.getX();
                    int width = view.getWidth();
                    if(posX < width/3){
                        if(mMediaPlayer.getTime() - REWIND_FFWD_UNIT > 0) {
                            showVideoSurfaceInfo("Seek: -5s");
                            mMediaPlayer.setTime(mMediaPlayer.getTime() - REWIND_FFWD_UNIT);
                        }
                    }else if(posX > width/3*2){
                        if(mMediaPlayer.getTime() + REWIND_FFWD_UNIT < mMediaPlayer.getLength()){
                            showVideoSurfaceInfo("Seek: +5s");
                            mMediaPlayer.setTime(mMediaPlayer.getTime() + REWIND_FFWD_UNIT);
                        }
                    }else{
                        updatePausePlay();
                    }

                    showSubtitleDelayController();
                }
                if(event.getActionMasked() == MotionEvent.ACTION_UP) {
                    view.performClick();
                }
                return true;
            }
        });
        return true;
    }

    private void releasePlayer() {
        if (libvlc == null){
            return;
        }
        long currentPosition = mMediaPlayer.getTime();
        VideoPlayerConfig.setVideoPosition(this, currentPosition);
        VideoPlayerConfig.setVideoDelay(this, subtitleDelay);
        setSubtitleTextView(null);

        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        mSurfaceHolder = null;
        libvlc.release();
        libvlc = null;
        //stop handler
        scrollSubtitleHandler.removeCallbacksAndMessages(null);
        subtitleDelayControllerHandler.removeCallbacksAndMessages(null);
        drawOrverlayProgressHandler.removeCallbacksAndMessages(null);
        videoInfoHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Check if subtitle file is exist or not
     *
     * @param filePath video file
     * @return return true if the file is exist. return false if the file is not exist.
     */
    private boolean isSubtitleFileExist(@NonNull String filePath) {
        int numExtension = filePath.lastIndexOf(".");
        if (numExtension < 0) {
            return false;
        }
        //Remove extension
        filePath = filePath.substring(0, numExtension + 1);
        for(String subtitleSupportExtension : SUPPORTED_SUBTITLE_EXTENSION){
            //extension removed filepath + subtitle extension = subtitleFilePath
            String subtitleFilePath = filePath + subtitleSupportExtension;
            File subtitleFile = new File(subtitleFilePath);
            if (subtitleFile.exists()) {
                VideoPlayerConfig.setSubtitleFilePath(this, subtitleFilePath);
                return true;
            }
        }
        return false;
    }



    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_ANKI_DECK) {
            if(grantResults.length <= 0
                    || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getResources().getString(R.string.error_msg_no_write_anki_db_permission), Toast.LENGTH_LONG).show();
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
            }
        }
    }


    /* ============================
     MediaPlayer Events
    ============================ */

    private MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);

    @Override
    public void onNewLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        if (width * height == 0)
            return;

        // store video size
        mVideoWidth = width;
        mVideoHeight = height;
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    public void onSurfacesCreated(IVLCVout vout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vout) {

    }

    @Override
    public void eventHardwareAccelerationError() {
        // Handle errors with hardware acceleration
        Utility.errorLog("Error with hardware acceleration");
        this.releasePlayer();
        Toast.makeText(this, getResources().getString(R.string.error_msg_hardware_accelaration), Toast.LENGTH_LONG).show();
    }

    private static class MyPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<VideoPlayerActivity> mOwner;

        MyPlayerListener(VideoPlayerActivity owner) {
            mOwner = new WeakReference<>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            VideoPlayerActivity player = mOwner.get();
            switch(event.type) {
                case MediaPlayer.Event.EndReached:
                    player.releasePlayer();
                    break;
                case MediaPlayer.Event.EncounteredError:
                    Utility.errorLog("EncounteredError Event at MediaPlayer Listener");
                    player.releasePlayer();
                    break;
                case MediaPlayer.Event.Playing:
                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:
                default:
                    break;
            }
        }
    }
}