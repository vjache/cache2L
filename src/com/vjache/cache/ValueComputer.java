package com.vjache.cache;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * This is a special kind of cache which is not caches values but computes them.
 */
public class ValueComputer extends CacheLayer {

    private final Function<Object, Object> func;

    protected ValueComputer(Function<Object,Object> func) {
        super(null);
        this.func = func;
    }

    @Override
    protected Object get_(Object key) {
        return func.apply(key);
    }

    @Override
    protected List<Map.Entry<Object, Object>> put_(Object key, Object value) {
        return null;
    }
}
