package com.vjache.cache;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Most Frequently Used strategy -- i.e. evicted least frequently used. This algorithm counts number of 'gets' for each
 * requested key. When cache size reaches max capacity it sorts keys by frequency and removes (evicts) first
 * amount = maxCapacity * (1 - 1/owerflowFactor) elements from cache.
 */
public class MemCacheMFU extends CacheLayer {

    private static class Slot {
        private int frequency = 1;
        private final Object val;
        Slot(Object val) {this.val = val;}
        final Object getValueAndIncFreq() {
            frequency ++;
            return val;
        }
    }

    private final ConcurrentHashMap<Object, Slot> data;
    private final int maxCapacity;
    private final double owerflowFactor;

    public MemCacheMFU(int maxCapacity, double owerflowFactor, Cache nextLevel) {
        super(nextLevel);
        this.maxCapacity = maxCapacity;
        data = new ConcurrentHashMap<>(maxCapacity);
        if(owerflowFactor <= 1)
            throw new IllegalArgumentException("Overflow factor must be greater than 1.");
        this.owerflowFactor = owerflowFactor;
    }

    @Override
    protected Object get_(Object key) {
        Slot slot = data.get(key);
        if (slot != null)
            return slot.getValueAndIncFreq();
        else
            return null;
    }

    @Override
    protected List<Entry<Object, Object>> put_(Object key, Object value) {
        ArrayList<Entry<Object, Object>> entriesEvict = null;
        if (data.size() >= maxCapacity)
        {
            // Do eviction
            synchronized (data) {
                if (data.size() >= maxCapacity) {
                    final ArrayList<Entry<Object, Slot>> entries = new ArrayList<>(data.entrySet());
                    Collections.sort(entries, (o1, o2) -> o1.getValue().frequency - o2.getValue().frequency);
                    final int evictSize = (int) (data.size() - data.size() / owerflowFactor);
                    entriesEvict = new ArrayList<>(evictSize);
                    for (int i = 0; i < evictSize; i++) {
                        final Entry<Object, Slot> e = entries.get(i);
                        data.remove(e.getKey());
                        entriesEvict.add(new SimpleEntry(e.getKey(), e.getValue()));
                    }
                }
            }
        }
        data.put(key, new Slot(value));
        return entriesEvict;
    }
}
