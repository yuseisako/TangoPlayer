package me.yusei.tangoplayer;

/**
 * Created by yuseisako on 2017/11/11.
 */

import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.TreeMap;

/**
 * These objects can (should) only be created through the implementations of parseFile() in the interface
 * They are an object representation of a subtitle file and contain all the captions and associated styles.
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
public class TimedTextObject {

    /*
     * Attributes
     *
     */
    //meta info
    public String title = "";
    public String description = "";
    public String copyrigth = "";
    public String author = "";
    public String fileName = "";
    public String language = "";
    int lastIndex = -1;

    //list of captions (begin time, reference)
    //represented by a tree map to maintain order
    MyLinkedMap<Integer, Caption> captions;

    //to store non fatal errors produced during parsing
    String warnings;

    //to delay or advance the subtitles, parsed into +/- milliseconds
    public int offset = 0;

    //to know if a parsing method has been applied
    boolean built = false;


    /**
     * Protected constructor so it can't be created from outside
     */
    protected TimedTextObject(){

        captions = new MyLinkedMap<>();

        warnings = "List of non fatal errors produced during parsing:\n\n";

    }

}