package me.yusei.tangoplayer;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * Created by yuseisako on 2018/05/19.
 *
 * original: https://stackoverflow.com/questions/20240408/how-to-get-element-position-from-java-map
 */

public class MyLinkedMap<K, V> extends LinkedHashMap<K, V>
{

    V getValue(int i) {
        Map.Entry<K, V>entry = this.getKey(i);
        if(entry == null) return null;

        return entry.getValue();
    }

    Map.Entry<K, V> getKey(int i) {
        // check if negetive index provided
        Set<Entry<K,V>> entries = entrySet();
        int j = 0;

        for(Map.Entry<K, V>entry : entries)
            if(j++ == i)return entry;

        return null;
    }

}
