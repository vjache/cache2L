package com.vjache.cache;

import java.io.Serializable;
import java.util.Map;


public class SimpleEntry implements Map.Entry<Object, Object>, Serializable {

    private final Object key;
    private Object val;

    public SimpleEntry(Object k, Object v) {
        key = k;
        val = v;
    }

    @Override
    public final Object getKey() {
        return key;
    }

    @Override
    public final Object getValue() {
        return val;
    }

    @Override
    public Object setValue(Object value) {
        Object oldVal = val;
        val = value;
        return oldVal;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object o) {
        Map.Entry<Object, Object> e = (Map.Entry<Object, Object>)o;
        return key.equals(e.getKey()) && val.equals(e.getValue());
    }

    @Override
    public int hashCode() {
        return key.hashCode() + val.hashCode();
    }
}
