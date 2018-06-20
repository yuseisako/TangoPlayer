package me.yusei.tangoplayer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
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
import java.util.Arrays;
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

public class VideoPlayerActivity extends AppCompatActivity implements IVLCVout.Callback , LibVLC.HardwareAccelerationError{
    private static final int MY_PERMISSIONS_REQUEST_WRITE_ANKI_DECK = 2;
    private static final int CONTROL_DURATION = 5000;
    //display surface
    private SurfaceHolder mSurfaceHolder;
    private SurfaceView mSurfaceView;
    private SurfaceView mSubtitlesSurface = null;

    // media player
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private Handler scrollSubtitleHandler = new Handler();
    private int mVideoWidth;
    private int mVideoHeight;
    private TextView subtitleTextView;

    //Overlay Video Controller
    ImageButton settingButton;
    ImageButton rewindButton;
    ImageButton playPauseButton;
    ImageButton fastForwardButton;
    ImageButton fileButton;
    TextView timeCurrentTextView;
    SeekBar progressSeekBar;
    TextView timeTotalTextView;
    boolean mDisplayRemainingTime = false;
    boolean mDragging;


    private TimedTextObject timedTextObject = new TimedTextObject();
    Runnable scrollSubtitleRunnable;
    private boolean mBackPressed;
    private AnkiDroidHelper mAnkiDroidHelper;
    private AnkiDroidController mAnkiDroidController;
    private static final String[] WORDS_REPLACE_LIST = {",", "- ", ".", "[", "]", "\"" };

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

    private void startTranslateTask(final EditText editTextWordMeaning, String word){
        TranslateTask task = new TranslateTask(new TranslateTaskCallback() {
            @Override
            public void onPreExecute() {
                //do nothing.
            }

            @Override
            public void onPostExecute(String result) {
                if (editTextWordMeaning != null && result != null && !result.isEmpty()) {
                    editTextWordMeaning.setText(result);
                }else{
                    Toast.makeText(getApplicationContext(), "Oops, Checking the translation is failed.", Toast.LENGTH_SHORT).show();
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

        rewindButton = findViewById(R.id.rew);
        rewindButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //mMediaPlayer.setTime(mMediaPlayer.getTime() - CONTROL_DURATION);
                mMediaPlayer.setTime(mMediaPlayer.getTime() - CONTROL_DURATION);
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
                //mMediaPlayer.setTime(mMediaPlayer.getTime() + CONTROL_DURATION);
                mMediaPlayer.setTime(mMediaPlayer.getTime() + CONTROL_DURATION);
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

        timeCurrentTextView = findViewById(R.id.time_current);
        progressSeekBar = findViewById(R.id.mediacontroller_progress);
        progressSeekBar.setOnSeekBarChangeListener(mSeekListener);

        timeTotalTextView = findViewById(R.id.time);

        subtitleTextView = findViewById(R.id.subtitleTextView);
    }

    private void updatePausePlay() {
        if (playPauseButton == null || mMediaPlayer == null) {
            return;
        }

        if (mMediaPlayer.isPlaying()) {
            playPauseButton.setImageResource(R.drawable.ic_media_play);
            mMediaPlayer.pause();
        } else {
            playPauseButton.setImageResource(R.drawable.ic_media_pause);
            mMediaPlayer.play();
        }
    }

    private void updatePlay(){
        if (playPauseButton == null || mMediaPlayer == null || mMediaPlayer.isPlaying()) {
            return;
        }

        playPauseButton.setImageResource(R.drawable.ic_media_pause);
        mMediaPlayer.play();
    }

    private void updatePause(){
        if (playPauseButton == null || mMediaPlayer == null) {
            return;
        }

        playPauseButton.setImageResource(R.drawable.ic_media_play);
        mMediaPlayer.pause();
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

    /* ============================
      Subtitle
     ============================ */

    /**
     * Scroll subtitle to current video position.
     * Loop trigger has to be drawSubtitles method.
     */
    public void scrollSubtitle(final TimedTextObject timedTextObject){
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
                        if (currentPos >= caption.start.mseconds
                                && currentPos <= caption.end.mseconds) {
                            listView.setItemChecked(index,true);
                            int height = listView.getHeight();
                            listView.setSelectionFromTop(index, height/2);
                            onTimedText(caption);
                            break;
                        } else {
                            onTimedText(null);
                            if(currentPos < caption.end.mseconds){
                                break;
                            }
                        }
                        index++;
                    }
                }

                if(! mDragging){
                    setOverlayProgress();
                }

                scrollSubtitleHandler.postDelayed(this, 300);
            }
        };
        scrollSubtitleHandler.post(scrollSubtitleRunnable);
    }


    public void onTimedText(Caption caption) {
        if(subtitleTextView == null){
            return;
        }
        if (caption == null) {
            subtitleTextView.setText("");
            return;
        }
        subtitleTextView.setText(caption.content);
    }
    /**
     * draw subtitles on View.
     * @param timedTextObject subtitles object
     */
    public void drawSubtitles(TimedTextObject timedTextObject){
        final ListView listView = findViewById(R.id.subtitleListView);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.subtitles_col);
        Collection<Caption> subtitles = timedTextObject.captions.values();
        final ArrayList<Integer> startTimeList = new ArrayList<>();
        for (Caption caption : subtitles) {
            arrayAdapter.add(caption.content);
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
                        if(!mMediaPlayer.isPlaying()){
                            mMediaPlayer.play();
                            playPauseButton.setImageResource(R.drawable.ic_media_pause);
                        }
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
                        if (mAnkiDroidHelper.shouldRequestPermission()) {
                            mAnkiDroidHelper.requestPermission(VideoPlayerActivity.this, MY_PERMISSIONS_REQUEST_WRITE_ANKI_DECK);
                            return true;
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

    private void showAnkiDialog(@NonNull  String sentence){
        if(sentence.isEmpty()){
            return;
        }
        if(mMediaPlayer.isPlaying()){
            updatePause();
        }
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        final View layout = inflater.inflate(R.layout.show_anki_dialog,
                (ViewGroup) findViewById(R.id.show_anki_dialog));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add words to Anki");
        builder.setPositiveButton("Add this card", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String word  = ((EditText)(layout.findViewById(R.id.add_word))).getText().toString();
                String wordMeaning  = ((EditText)(layout.findViewById(R.id.add_word_meaning))).getText().toString();
                String sentence = ((TextView) layout.findViewById(R.id.add_sentence)).getText().toString();
                String sentenceMeaning = ((EditText)(layout.findViewById(R.id.add_sentence_meaning))).getText().toString();

                if(!word.isEmpty() && !wordMeaning.isEmpty()) {
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
                        deckName = "Tango Player";
                    }else{
                        String videoFileName = VideoPlayerConfig.getVideoFileName(getApplicationContext());
                        if(videoFileName != null){
                            deckName = videoFileName;
                        }
                    }
                    mAnkiDroidController.addCardsToAnkiDroid(deckName, cardContentsList);
                }
                updatePlay();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                updatePlay();
            }
        });

        builder.setView(layout);
        builder.create().show();

        ImageButton translateButton = layout.findViewById(R.id.translate);
        translateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    String word  = ((EditText)(layout.findViewById(R.id.add_word))).getText().toString();
                    EditText editTextWordMeaning = layout.findViewById(R.id.add_word_meaning);

                    String sentence = ((TextView) layout.findViewById(R.id.add_sentence)).getText().toString();
                    EditText editTextSentenceMeaning = layout.findViewById(R.id.add_sentence_meaning);

                    if(!word.isEmpty()){
                        startTranslateTask(editTextWordMeaning, word);
                        startTranslateTask(editTextSentenceMeaning, sentence);
                    }
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
        if(mSurfaceView == null){
            Toast.makeText(this, "mSurfaceView is null", Toast.LENGTH_SHORT).show();
        }
        //mMediaController = new AndroidMediaController(this, false);

        // Inflate the layout for this fragment
//        final View fv = inflater.inflate(R.layout.fragment_hogehogeplayer, container, false);
//        mVideo = (FrameLayout)fv.findViewById(R.id.video_view);
//        mCustomMediaController = new CustomMediaController(this) {
//
//            @Override
//            public void hide() {
//                //Do not hide.
//            }
//        };

        Boolean isSeek = false;
        String videoFilePath = VideoPlayerConfig.getVideoFilePath(this);
        if(videoFilePath != null && filePath.compareTo(videoFilePath) == 0){
            isSeek = true;
        }

        if(createPlayer(filePath, isSeek)){
            VideoPlayerConfig.setVideoFilePath(this, filePath);
        }else {
            return false;
        }

        initControllerView();

        if (isSubtitleFileExist(filePath)) {
            startReadSrtFileTask();
        }else{
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.subtitles_col);
            arrayAdapter.add("Subtitles NOT FOUND.");
            arrayAdapter.add("It should be same file name as video file plus extension.");
        }

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
            m.setHWDecoderEnabled(true, false);
            mMediaPlayer.setMedia(m);

            mMediaPlayer.play();
            if(playPauseButton != null){
                playPauseButton.setImageResource(R.drawable.ic_media_pause);
            }

            if(isSeek){
                //While return -1 from getTime(), you cannot seek video. It happen only right after load media.
                final Handler handler = new Handler();
                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        if(mMediaPlayer.getTime() > 0){
                            mMediaPlayer.setTime(VideoPlayerConfig.getVideoPosition(getBaseContext()));
                        }else{
                            handler.postDelayed(this, 100);
                        }
                    }
                };
                handler.post(r);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error creating player!" + e.toString(), Toast.LENGTH_LONG).show();
            return false;
        }

        mSurfaceView.getRootView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    Float posX = event.getX();
                    int width = view.getWidth();
                    if(posX < width/3){
                        mMediaPlayer.setTime(mMediaPlayer.getTime() - CONTROL_DURATION);
                    }else if(posX > width/3*2){
                        mMediaPlayer.setTime(mMediaPlayer.getTime() + CONTROL_DURATION);
                    }else{
                        updatePausePlay();
                    }
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
        onTimedText(null);

        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        mSurfaceHolder = null;
        libvlc.release();
        libvlc = null;
        //stop scrollSubtitle handler
        scrollSubtitleHandler.removeCallbacksAndMessages(null);
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

        List<String> subtitleSupportExtensionList = new ArrayList<>(Arrays.asList("srt", "vtt", "acc"));
        int numExtension = filePath.lastIndexOf(".");
        if (numExtension < 0) {
            return false;
        }
        //Remove extension
        filePath = filePath.substring(0, numExtension + 1);
        for(String subtitleSupportExtension : subtitleSupportExtensionList){
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
        Toast.makeText(this, "Error with hardware acceleration", Toast.LENGTH_LONG).show();
    }

    private static class MyPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<VideoPlayerActivity> mOwner;

        MyPlayerListener(VideoPlayerActivity owner) {
            mOwner = new WeakReference<>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            VideoPlayerActivity player = mOwner.get();

            Utility.debugLog("Player EVENT");
            switch(event.type) {
                case MediaPlayer.Event.EndReached:
                    Utility.debugLog("MediaPlayerEndReached");
                    player.releasePlayer();
                    break;
                case MediaPlayer.Event.EncounteredError:
                    Utility.debugLog("Media Player Error, re-try");
                    //player.releasePlayer();
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