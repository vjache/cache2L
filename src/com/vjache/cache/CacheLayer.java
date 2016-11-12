package com.vjache.cache;


import java.util.List;
import java.util.Map.Entry;

/**
 * Abstract cache framework.
 */
public abstract class CacheLayer implements Cache {

    protected Cache nextLevel;

    protected CacheLayer(Cache nextLevel) {
        this.nextLevel = nextLevel;
    }

    /**
     * Underlying implementation of get value by key from cache.
     * @param key - key
     * @return - value
     */
    protected abstract Object get_(Object key);

    /**
     * Underlying implementation of put (K,V) pair into cache.
     * @param key - a key
     * @param value - a value
     * @return - evicted (K,V) pairs.
     */
    protected abstract List<Entry<Object, Object>> put_(Object key, Object value);

    public Object get(Object key) {
        final Object val = get_(key);
        if (val == null) {
            final Object val2 = nextLevel.get(key);
            if(val2 != null)
                put(key, val2);
            return val2;
        }
        else
            return val;
    }

    public void put(Object key, Object value) {
        final List<Entry<Object,Object>> evicted = put_(key, value);
        if(evicted != null && evicted.size() > 0)
        {
            for (Entry<Object, Object> e: evicted){
                nextLevel.put(e.getKey(), e.getValue());
            }
        }
    }
}
