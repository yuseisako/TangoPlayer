package me.yusei.tangoplayer;

import java.util.TreeMap;

/**
 * Created by yuseisako on 2018/03/03.
 *
 * The algorithm is referenced at
 * https://github.com/carsonip/Penguin-Subtitle-Player/
 */

public class ScrollSubtitle {

    private AsyncCallback asyncCallback = null;
    private TimedTextObject mTimedTextObject;
    private boolean doNotUpdate = false;

    public void setTimedTextObject(TimedTextObject timedTextObject){
        this.mTimedTextObject = timedTextObject;
    }

    public String currentSubtitle(int time, boolean sliderMoved) {
        int index = currentSubtitleIndex(time, sliderMoved);
        if (index != -1) {
            mTimedTextObject.lastIndex = index;
            return mTimedTextObject.captions.get(index).content;
        }
        return "";
    }

    public int currentSubtitleIndex(int time, boolean seekBarMoved) {
        // Fetch the suitable subtitle content for current time

        if (mTimedTextObject.captions.isEmpty())
            return -1;

        if (time >= getFinishTime())
            return mTimedTextObject.captions.size() - 1;

        TreeMap<Integer, Caption> item = mTimedTextObject.captions;

        if (mTimedTextObject.lastIndex != -1 && !seekBarMoved) {
            //  Linear search for next subtitle from last subtitle if slide bar is
            //  not manually set
            for (int i = mTimedTextObject.lastIndex, len = mTimedTextObject.captions.size(); i < len; i++) {

                if (time >= item.get(i).start.getMseconds() && time <= item.get(i).end.getMseconds())
                    return i;
            }
        } else {
            // Binary Search for initialization or if slide bar is manually set
            int lo = 0, hi = mTimedTextObject.captions.size() - 1;
            while (lo < hi) {
                int mid = lo + (hi - lo) / 2;

                if (time >= item.get(mid).start.getMseconds() && time <= item.get(mid).end.getMseconds()) {
                    lo = mid;
                    break;
                } else if (time > item.get(mid).end.getMseconds()) {
                    lo = mid + 1;
                } else {
                    hi = mid;
                }
            }
            if (time >= item.get(lo).start.getMseconds() && time <= item.get(lo).end.getMseconds()) {
                return lo;
            }
        }
        return -1;
    }

    private int getFinishTime() {
        // Fetch the end time of last subtitle
        if (mTimedTextObject.captions.isEmpty())
            return 0;

        //return last end time in subtitle file
        int size = mTimedTextObject.captions.size();
        Caption mCaption2 = mTimedTextObject.captions.get(size -2);
        Caption mCaption1 = mTimedTextObject.captions.get(size -1);
        Time time = mCaption1.end;
        return time.getMseconds();

        //return mTimedTextObject.captions.get(mTimedTextObject.captions.size() -1).end.getMseconds();
    }

}
