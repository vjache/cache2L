package com.vjache.cache;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.*;


public class CacheTest {

    private static final int AMOUNT = 10000;
    private static final String CACHE_DIR = "./cache";

    @Test
    public void twoLayerCacheTest() {
        // Create a two layer cache where first layer is a memory based MRU and may contain a 2 maximum elements and
        // second layer is a persistent cache with 200 bucket directories.
        final Cache cache = new MemCacheMRU(2, new FileCache(new File("./cache"), 200, new DummyCache()));

        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");

        assertEquals("value1", cache.get("key1"));
    }

    @Test
    public void twoLayerCacheTest2() throws IOException {
        // Create a two layer cache where first layer is a memory based MRU and may contain a 3 maximum elements and
        // second layer is a persistent cache based on memory mapped file with 1000 bucket segments in a file each 8 Mb in size.
        final Cache cache = new MemCacheMRU(3, new MMFileCache(new File("./cache/mmCache"), 1000, 8 * 1024 * 1024, new DummyCache()));

        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        cache.put("key4", "value4");
        cache.put("key5", "value5");

        assertEquals("value1", cache.get("key1"));
    }

    @Test
    public void fileCacheTest() {
        final Cache cache = new FileCache(new File(CACHE_DIR), 1000, new DummyCache());

        for(int i = 0; i< AMOUNT; i++) {
            cache.put("key" + i, "value" + i);
        }

        for(int i = 0; i< AMOUNT; i++) {
            assertEquals("value" + i, cache.get("key" + i));
        }
    }

    @Test
    public void mmFileCacheTest() throws IOException {
        final Cache cache = new MMFileCache(new File("./cache/mmCache"), new DummyCache());

        for(int i = 0; i< AMOUNT; i++) {
            cache.put("key" + i, "value" + i);
        }

        for(int i = 0; i< AMOUNT; i++) {
            assertEquals("value" + i, cache.get("key" + i));
        }

        for(int i = AMOUNT; i< 2*AMOUNT; i++) {
            cache.put("key" + i, "value" + i);
        }

        for(int i = AMOUNT; i< 2*AMOUNT; i++) {
            assertEquals("value" + i, cache.get("key" + i));
        }

    }

    @Test
    public void memCacheFreqBased() {
        final Cache cache = new MemCacheMFU((int) (AMOUNT*1.5), 1.5, new DummyCache());
        for(int i = 0; i< AMOUNT; i++) {
            cache.put("key" + i, "value" + i);
        }

        for(int i = 0; i< AMOUNT; i++) {
            assertEquals("value" + i, cache.get("key" + i));
        }

        for(int i = AMOUNT; i< 4*AMOUNT; i++) {
            cache.put("key" + i, "value" + i);
        }

        for(int i = 0; i< AMOUNT; i++) {
            assertEquals("value" + i, cache.get("key" + i));
        }

        boolean b = true;
        for(int i = AMOUNT; i< 4*AMOUNT; i++) {
            b &= "value" + i == cache.get("key" + i);
        }

        assertFalse(b);

    }

    @Test
    public void valueComputingCacheTest() {
        final Cache cache = new ValueComputer(o -> o.toString() + "_value");
        for(int i = 0; i< AMOUNT; i++) {
            assertEquals("key" + i + "_value", cache.get("key" + i));
        }
    }



    @Test
    public void complexTest3() {
        final Cache cache = new MemCacheMRU(100, new FileCache(new File("./cache"), 1000,
                new ValueComputer(o -> {
                    if(o.toString().startsWith("compute_")) {
                        return o.toString() + "_value";
                    }
                    else return null;
                })));

        for(int i = 0; i< AMOUNT; i++) {
            cache.put("key" + i, "value" + i);
        }

        for(int i = 0; i< AMOUNT; i++) {
            assertEquals("value" + i, cache.get("key" + i));
        }

        for(int i = 0; i< AMOUNT; i++) {
            assertEquals("compute_key" + i + "_value", cache.get("compute_key" + i));
        }
    }

    @Before
    public void cleanUp() throws IOException {
        try {delete(new File(CACHE_DIR));}
        catch (FileNotFoundException ignored) {}
    }

    private void delete(File f) throws IOException {
        if (f.isDirectory()) {
            //noinspection ConstantConditions
            for (File c : f.listFiles())
                delete(c);
        }
        if (!f.delete())
            throw new FileNotFoundException("Failed to delete file: " + f);
    }
}