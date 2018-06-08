package me.yusei.tangoplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nex3z.flowlayout.FlowLayout;
import com.nononsenseapps.filepicker.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ijk.media.IjkVideoView;
import ijk.media.VideoControllerView;
import me.yusei.tangoplayer.anki.AnkiDroidController;
import me.yusei.tangoplayer.anki.AnkiDroidHelper;
import me.yusei.tangoplayer.filepicker.FilteredFilePickerActivity;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class MainActivity extends AppCompatActivity {
    //    public class MainActivity extends AppCompatActivity  implements Runnable, View.OnClickListener, MediaPlayer.OnTimedTextListener {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 1;
    private static final String VIDEO_FILE_PATH = "VIDEO_FILE_PATH";
    private static final String SUBTITLE_FILE_PATH = "SUBTITLE_FILE_PATH";
    private static final String VIDEO_DURATION = "VIDEO_DURATION";
    private static final int FILE_CODE = 1;
    private IjkVideoView mVideoView;
    private VideoControllerView mVideoControllerView;

    MyLinkedMap<Integer, Caption> captionTreeMap = new MyLinkedMap<>();
    private Handler scrollSubtitleHandler = new Handler();
    Runnable scrollSubtitleRunnable;

    private static MainActivity instance = null;
    private boolean mBackPressed;
    private AnkiDroidHelper mAnkiDroidHelper;
    private AnkiDroidController mAnkiDroidController;
    private static final int AD_PERM_REQUEST = 0;
    //TODO: User can choose skip or not.
    //private static final String[] WORDS_REPLACE_LIST = {",", "-", "a", "an", "the", "\"" };
    private static final String[] WORDS_REPLACE_LIST = {",", "- ", ".", "[", "]", "\"" };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAnkiDroidHelper = new AnkiDroidHelper(this);
        mAnkiDroidController = new AnkiDroidController(this, mAnkiDroidHelper);
        checkPermission();
        instance = this;
    }

    public static MainActivity getInstance() {
        return instance;
    }

    private void initializePlayer(){
        if (getVideoFilePath() == null || !playVideoFromFilePath(getVideoFilePath())) {
            releasePlayer();
            launchFilePicker();
        }else{
            // when video file path is invalid or no stored video file path => no seek.
            // otherwise (video file path is valid) => seek video to stored duration.
            mVideoView.seekTo(getVideoDuration());
        }
    }

    private void releasePlayer() {
        if(mVideoView != null) {
            int currentPosition = mVideoView.getCurrentPosition();
            setVideoDuration(currentPosition);
            mVideoView.stopPlayback();
            mVideoView.release(true);
            mVideoView.stopBackgroundPlay();
            mVideoView.setSubtitleText("");
            mVideoControllerView.hide();
            //stop scrollSubtitle handler

            scrollSubtitleHandler.removeCallbacksAndMessages(null);
        }
    }

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
        ReadSrtFileTask task = new ReadSrtFileTask(new ReadSrtFileTaskCallback() {
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
        task.setSubtitleFilePath(getSubtitleFilePath());
        TimedTextObject timedTextObject = new TimedTextObject();
        task.execute(timedTextObject);
    }

    /**
     * Scroll subtitle to current video position.
     * Loop trigger has to be drawSubtitles method.
     */
    public void scrollSubtitle(final TimedTextObject timedTextObject){
        final ListView listView = findViewById(R.id.subtitles);
        int size = timedTextObject.captions.size();
        final int lastTime = timedTextObject.captions.getValue(size - 1 ).end.getMseconds();

        scrollSubtitleRunnable = new Runnable() {
            @Override
            public void run() {
                int currentVideoTime = 0;
                int nextTime = 0;
                //TODO: fix delay
                if(mVideoView != null || mVideoView.isPlaying()){
                    currentVideoTime = mVideoView.getCurrentPosition();

                    while(mVideoView.isPlaying() && currentVideoTime <= lastTime){
                        if(timedTextObject.captions.containsKey(currentVideoTime)){
                            int index = timedTextObject.captions.get(currentVideoTime).index;
                            String subtitleContent = timedTextObject.captions.get(currentVideoTime).content;
                            listView.setItemChecked(index,true);
                            int height = listView.getHeight();
                            listView.setSelectionFromTop(index, height/2);
                            mVideoView.setSubtitleText(subtitleContent);
                            nextTime = currentVideoTime;
                            while(true){
                                nextTime++;
                                if(timedTextObject.captions.containsKey(nextTime)){
                                    break;
                                }
                                if(nextTime > lastTime){
                                    break;
                                }
                            }
                            break;
                        }
                        currentVideoTime++;
                    }
                }
                if(nextTime == 0 || currentVideoTime == 0){
                    scrollSubtitleHandler.postDelayed(this, 1000);
                }else{
                    scrollSubtitleHandler.postDelayed(this, nextTime - currentVideoTime - 10);
                }
            }
        };
        scrollSubtitleHandler.post(scrollSubtitleRunnable);
    }

    /**
     * draw subtitles on View.
     * @param timedTextObject subtitles object
     */
    public void drawSubtitles(final TimedTextObject timedTextObject){
        captionTreeMap = timedTextObject.captions;
        Iterator<Integer> it = captionTreeMap.keySet().iterator();
        final ListView listView = findViewById(R.id.subtitles);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
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
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    if(mVideoView !=null) {
                        Caption caption = timedTextObject.captions.getValue(position);
                        Toast.makeText(getApplicationContext(), caption.content + " (" + caption.start + ")", Toast.LENGTH_SHORT).show();
                        int captionMiliSec = caption.start.getMseconds();
                        if( captionMiliSec > 500){
                            mVideoView.seekTo(captionMiliSec);
                        }else{
                            mVideoView.seekTo(0);
                        }
                        mVideoView.setSubtitleText(caption.content);
                        mVideoView.start();
                        mVideoControllerView.updatePausePlay();
                    }
                }
            });

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id){
                    if(mVideoView !=null) {
                        Caption caption = timedTextObject.captions.getValue(position);
                        //Toast.makeText(getApplicationContext(),caption.content,Toast.LENGTH_SHORT).show();
                        mVideoView.pause();
                        mVideoControllerView.updatePausePlay();
                        // Request permission to access API if required
                        if (mAnkiDroidHelper.shouldRequestPermission()) {
                            mAnkiDroidHelper.requestPermission(MainActivity.this, AD_PERM_REQUEST);
                            return true;
                        }
                        showAnkiDialog(caption.content);
                    }
                    return true;
                }
            });
        }
    }


    private boolean addWordButton(FlowLayout view, String words, final EditText frontWords){
        if(view == null || words.isEmpty() || frontWords == null){
            return false;
        }

        for(String string: WORDS_REPLACE_LIST){
            words = words.replace(string, "");
        }
        String[] splitWords = words.split(" ");

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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


            word.setOnTouchListener(new View.OnTouchListener(){
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            view.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.frame_style_clicked));
                            return true;
                        case MotionEvent.ACTION_UP:
                            view.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.frame_style));
                            view.performClick();
                            return true;
                    }
                    return false;
                }
            });
            view.addView(word);
        }

        return true;
    }

    private void showAnkiDialog(@NonNull  String sentence){
        if(mVideoView != null){
            mVideoView.pause();
            mVideoControllerView.updatePausePlay();
        }
        if(sentence.isEmpty()){
            return;
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
                    //TODO:user can change deckName
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
                        if(getVideoFileName() != null)
                            deckName = getVideoFileName();
                    }
                    mAnkiDroidController.addCardsToAnkiDroid(deckName, cardContentsList);
                }
                if(mVideoView != null){
                    mVideoView.start();
                    mVideoControllerView.updatePausePlay();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mVideoView.start();
                mVideoControllerView.updatePausePlay();
            }
        });

        builder.setView(layout);
        builder.create().show();

        ImageButton translateButton = layout.findViewById(R.id.translate);
        translateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkNetwork()){
                    String word  = ((EditText)(layout.findViewById(R.id.add_word))).getText().toString();
                    EditText editTextWordMeaning = layout.findViewById(R.id.add_word_meaning);

                    String sentence = ((TextView) layout.findViewById(R.id.add_sentence)).getText().toString();
                    EditText editTextSentenceMeaning = layout.findViewById(R.id.add_sentence_meaning);

                    if(!word.isEmpty()){
                        startTranslateTask(editTextWordMeaning, word);
                        startTranslateTask(editTextSentenceMeaning, sentence);
                        Toast.makeText(getApplicationContext(), "checking translation...", Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Toast.makeText(getApplicationContext(), "no network ...", Toast.LENGTH_SHORT).show();
                }
            }
        });
        TextView sentenceTextView = layout.findViewById(R.id.add_sentence);
        sentenceTextView.setText(sentence);

        FlowLayout wordButtons = layout.findViewById(R.id.word_buttons);
        EditText wordsFrontEditText = layout.findViewById(R.id.add_word);
        addWordButton(wordButtons, sentence, wordsFrontEditText);
    }

    private boolean checkNetwork() {
        ConnectivityManager internetManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = internetManager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected() && networkInfo.isAvailable() && networkInfo.isConnectedOrConnecting());
    }

    /**
     * Play Video From uri and return result
     *
     * @param filePath filePath for video file.
     * @return true if uri is valid and playable. false if uri is null or invalid.
     */
    private boolean playVideoFromFilePath(@NonNull String filePath) {
        //init UI
        //mMediaController = new AndroidMediaController(this, false);

        mVideoControllerView = new VideoControllerView(this);

        // init player
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");

        mVideoView = findViewById(R.id.video_view);
        mVideoView.setMediaController(mVideoControllerView);
        mVideoView.setVideoPath(filePath);

        if (isSubtitleFileExist(filePath)) {
            startReadSrtFileTask();
        }else{
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.subtitles_col);
            arrayAdapter.add("Subtitles NOT FOUND.");
            arrayAdapter.add("It should be same file name as video file plus extension.");
        }

        mVideoView.start();
        mVideoControllerView.showWaitForPlayOnView();

        return true;
    }

    //TODO:delete
    public void releaseAndLanchFilePicker(){
        releasePlayer();
        launchFilePicker();
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
        //called this method when resume app
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        mBackPressed = true;

        super.onBackPressed();
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(this, "Permission NOT granted", Toast.LENGTH_SHORT).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    finish();
                }
            }
        }
    }

    public String getVideoFileName(){
        String videoFilePath = getVideoFilePath();
        File videoFile = new File(videoFilePath);
        return videoFile.getName();
    }

    /**1
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
     * get subtitles file path from SP.
     * @return subtitles filePath or null if nothing.
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
     * set video file path to SP
     * @param mFilePath video file
     */
    private void setSubtitleFilePath(String mFilePath) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SUBTITLE_FILE_PATH, mFilePath);
        editor.apply();
    }

    /**
     * Get duration from SP
     * @return long duration
     */
    public int getVideoDuration() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getInt(VIDEO_DURATION, 0);
    }

    /**
     * Save duration to SP
     * @param duration video duration
     */
    public void setVideoDuration(int duration) {
        if (duration >= 0) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(VIDEO_DURATION, duration);
            editor.apply();
        }
    }

    public void launchFilePicker() {
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
            setVideoDuration(0);

            if (playVideoFromFilePath(file.toString())) {
                setVideoFilePath(file.toString());
            } else {
                Toast.makeText(this, "VideoFilePath is invalid. Chose Video file again.[onActivityResult]", Toast.LENGTH_SHORT).show();
                launchFilePicker();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        initializePlayer();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mVideoView != null) {
            if (mBackPressed || !mVideoView.isBackgroundPlayEnabled()) {
                releasePlayer();
            } else {
                mVideoView.enterBackground();
            }
            IjkMediaPlayer.native_profileEnd();
        }
    }

}