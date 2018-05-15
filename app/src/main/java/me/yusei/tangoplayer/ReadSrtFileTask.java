package me.yusei.tangoplayer;

import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Original source code:
 * https://github.com/JDaren/subtitleConverter/
 *
 * This class represents the .SRT subtitle format
 * <br><br>
 * Copyright (c) 2012 J. David Requejo <br>
 * j[dot]david[dot]requejo[at] Gmail
 * <br><br>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 * <br><br>
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 * <br><br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 * @author J. David Requejo
 *
 */
public class ReadSrtFileTask extends AsyncTask<TimedTextObject, Integer, TimedTextObject> {

    private AsyncCallback asyncCallback = null;
    private TimedTextObject mTimedTextObject;
    private String mSubtitleFilePath;

    public void setSubtitleFilePath(String subtitleFilePath) {
        this.mSubtitleFilePath = subtitleFilePath;
    }

    public ReadSrtFileTask(AsyncCallback asyncCallback){
        this.asyncCallback = asyncCallback;
    }

    public void setTimedTextObject(TimedTextObject timedTextObject){
        this.mTimedTextObject = timedTextObject;
    }

    public TimedTextObject getTimedTextObject(){
        return mTimedTextObject;
    }

    @Override
    protected TimedTextObject doInBackground(TimedTextObject... timedTextObjects) {
        try{
            parseFile();
            return mTimedTextObject;
        }catch (IOException ioe){
            mTimedTextObject.warnings += "Caught IOException in parseFile, ReadSrtFileTask";
            Utility.errorLog("Caught IOException in parseFile()");
        }
        return null;
    }

    @Override
    protected void onPreExecute(){
        super.onPreExecute();
        this.asyncCallback.onPreExecute();
        mTimedTextObject = new TimedTextObject();
    }

    @Override
    protected void onPostExecute(TimedTextObject result) {
        super.onPostExecute(result);
        this.asyncCallback.onPostExecute(result);
        //callbacktask.CallBack(mTimedTextObject);
        //listener.onTaskCompleted();
    }

    @Override
    protected void onProgressUpdate(Integer... values){
        super.onProgressUpdate(values);
        this.asyncCallback.onProgressUpdate(values[0]);
    }

    public void parseFile() throws IOException {

        Caption caption = new Caption();
        int captionNumber = 1;
        boolean allGood;

        String subtitleFilePath = Utility.nonNull(mSubtitleFilePath);

        //first lets load the file
        File subtitleFile = new File(subtitleFilePath);
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(subtitleFile));
            String line = br.readLine();
            line = line.replace("\uFEFF", ""); //remove BOM character
            int lineCounter = 0;
            while(line!=null){
                line = line.trim();
                lineCounter++;
                //if its a blank line, ignore it, otherwise...
                if (!line.isEmpty()){
                    allGood = false;
                    //the first thing should be an increasing number
                    try {
                        int num = Integer.parseInt(line);
                        if (num != captionNumber)
                            throw new Exception();
                        else {
                            captionNumber++;
                            allGood = true;
                        }
                    } catch (Exception e) {
                        mTimedTextObject.warnings+= captionNumber + " expected at line " + lineCounter;
                        mTimedTextObject.warnings+= "\n skipping to next line\n\n";
                    }
                    if (allGood){
                        //we go to next line, here the begin and end time should be found
                        try {
                            lineCounter++;
                            line = br.readLine().trim();
                            String start = line.substring(0, 12);
                            String end = line.substring(line.length()-12, line.length());
                            Time time = new Time("hh:mm:ss,ms",start);
                            caption.start = time;
                            time = new Time("hh:mm:ss,ms",end);
                            caption.end = time;
                        } catch (Exception e){
                            mTimedTextObject.warnings += "incorrect time format at line "+lineCounter;
                            allGood = false;
                        }
                    }
                    if (allGood){
                        //we go to next line where the caption text starts
                        lineCounter++;
                        line = br.readLine().trim();
                        String text = "";
                        while (!line.isEmpty()){
                            text+=line;
                            line = br.readLine().trim();
                            lineCounter++;
                        }
                        caption.content = text;
                        int key = caption.start.mseconds;
                        //in case the key is already there, we increase it by a millisecond, since no duplicates are allowed
                        while (mTimedTextObject.captions.containsKey(key)) key++;
                        if (key != caption.start.mseconds)
                            mTimedTextObject.warnings+= "caption with same start time found...\n\n";
                        //we add the caption.
                        mTimedTextObject.captions.put(key, caption);
                    }
                    //we go to next blank
                    while (!line.isEmpty()) {
                        line = br.readLine().trim();
                        lineCounter++;
                    }
                    caption = new Caption();
                }
                line = br.readLine();
            }

        }  catch (NullPointerException e){
            mTimedTextObject.warnings+= "unexpected end of file, maybe last caption is not complete.\n\n";
        } finally{
            //we close the reader
            if(br != null){
                br.close();
            }
        }
        mTimedTextObject.built = true;
    }

	/* PRIVATE METHODS */

    /**
     * This method cleans caption.content of XML and parses line breaks.
     *
     */
    private String[] cleanTextForSRT(Caption current) {
        String[] lines;
        String text = current.content;
        //add line breaks
        lines = text.split("<br />");
        //clean XML
        for (int i = 0; i < lines.length; i++){
            //this will destroy all remaining XML tags
            lines[i] = lines[i].replaceAll("\\<.*?\\>", "");
        }
        return lines;
    }


}