package com.vjache.cache;

/**
 * Created by vj on 12.11.16.
 */
public interface Cache {

    Object get(Object key);
    void   put(Object key, Object value);
}
