package com.vjache.cache;


public class DummyCache implements Cache {

    public DummyCache() {}

    @Override
    public Object get(Object key) {
        return null;
    }

    @Override
    public void put(Object key, Object value) {}

}
