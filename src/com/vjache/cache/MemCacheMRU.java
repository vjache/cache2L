package com.vjache.cache;

import java.util.*;
import java.util.Map.Entry;

/**
 * Most Recently Used strategy -- i.e. evicted least recently used. This straight algorithm, when key is requested
 * (and value not null) moves it to the top of a list.
 */
public class MemCacheMRU extends CacheLayer {

    private LinkedList<SimpleEntry> data = new LinkedList<>();
    private int dataSize = 0;
    private final int maxSize;

    public MemCacheMRU(int maxSize, Cache next) {
        super(next);
        this.maxSize = maxSize;
    }

    @Override
    protected Object get_(Object key) {
        final Iterator<SimpleEntry> it = data.iterator();
        while (it.hasNext()) {
            final SimpleEntry entry = it.next();
            if(entry.getKey().equals(key))
            {
                it.remove();
                data.addFirst(entry);
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    protected List<Entry<Object, Object>> put_(Object key, Object value) {
        List<Entry<Object, Object>> evict = null;
        if (dataSize >= maxSize) {
            evict = Collections.singletonList(data.removeLast());
            dataSize --;
        }

        data.addLast(new SimpleEntry(key, value));
        dataSize ++;

        return evict;
    }
}
