package me.yusei.tangoplayer.task;

import android.os.AsyncTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import me.yusei.tangoplayer.Utility;
import me.yusei.tangoplayer.subtitle.FatalParsingException;
import me.yusei.tangoplayer.subtitle.FormatASS;
import me.yusei.tangoplayer.subtitle.FormatSCC;
import me.yusei.tangoplayer.subtitle.FormatSRT;
import me.yusei.tangoplayer.subtitle.FormatTTML;
import me.yusei.tangoplayer.subtitle.TimedTextObject;

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
public class ReadSubtitleFileTask extends AsyncTask<TimedTextObject, Integer, TimedTextObject> {

    private ReadSubtitleFileTaskCallback readSrtFileTaskCallback = null;
    private String mSubtitleFilePath;

    public void setSubtitleFilePath(String subtitleFilePath) {
        this.mSubtitleFilePath = subtitleFilePath;
    }

    public ReadSubtitleFileTask(ReadSubtitleFileTaskCallback readSrtFileTaskCallback){
        this.readSrtFileTaskCallback = readSrtFileTaskCallback;
    }

    @Override
    protected TimedTextObject doInBackground(TimedTextObject... tto) {
        if(tto != null){
            try{
                if(mSubtitleFilePath != null && !mSubtitleFilePath.isEmpty()){
                    int i = mSubtitleFilePath.lastIndexOf('.');
                    if (i > 0) {
                        String extension = mSubtitleFilePath.substring(i+1);
                        File subtitleFile = new File(mSubtitleFilePath);
                        InputStream subtitleInputStream = new FileInputStream(subtitleFile);
                        switch (extension) {
                            case "srt":
                                FormatSRT formatSRT = new FormatSRT();
                                return formatSRT.parseFile(mSubtitleFilePath, subtitleInputStream);
                            case "ass":
                                FormatASS formatASS = new FormatASS();
                                return formatASS.parseFile(mSubtitleFilePath, subtitleInputStream);
                            case "scc":
                                FormatSCC formatSCC = new FormatSCC();
                                return formatSCC.parseFile(mSubtitleFilePath, subtitleInputStream);
                            case "ttml":
                                FormatTTML formatTTML = new FormatTTML();
                                return formatTTML.parseFile(mSubtitleFilePath, subtitleInputStream);
                        }

                    }

                }
            }catch (IOException ioe){
                tto[0].warnings += "Caught IOException in parseFile, ReadSubtitleFileTask";
                Utility.errorLog("Caught IOException in parseFile()");
                ioe.printStackTrace();
            }catch (FatalParsingException fpe) {
                tto[0].warnings += "Caught FatalParsingException in parseFile, ReadSubtitleFileTask";
                Utility.errorLog("Caught IOException in parseFile()");
                fpe.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onPreExecute(){
        super.onPreExecute();
        this.readSrtFileTaskCallback.onPreExecute();
    }

    @Override
    protected void onPostExecute(TimedTextObject timedTextObjects) {
        super.onPostExecute(timedTextObjects);
        this.readSrtFileTaskCallback.onPostExecute(timedTextObjects);
        //callbacktask.CallBack(mTimedTextObject);
        //listener.onTaskCompleted();
    }

    @Override
    protected void onProgressUpdate(Integer... values){
        super.onProgressUpdate(values);
        this.readSrtFileTaskCallback.onProgressUpdate(values[0]);
    }

}