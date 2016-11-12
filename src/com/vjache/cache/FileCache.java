package com.vjache.cache;


import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * This is a simple file based cache. Cache store data in a key-value files i.e. one file per key-value pair. Such a
 * files distributed over a set of directories called bucket directories. Name of a bucket directory computed based on
 * a hash of a key.
 *
 * Currently this cache implementation:
 *  * is not restricted by allocating memory on disk
 *  * uses primitive concurrency serialization mechanism
 */
public class FileCache extends CacheLayer {

    private final File rootDir;
    private final int bucketsNumber;

    public FileCache(File rootDir,
                     int bucketsNumber,
                     Cache next) {
        super(next);
        this.bucketsNumber = bucketsNumber;
        this.rootDir = new File(rootDir, "_" + bucketsNumber);
        //noinspection ResultOfMethodCallIgnored
        this.rootDir.mkdirs();
    }

    @Override
    protected synchronized Object get_(Object key) {
        File[] files = listBucketFiles(key);
        for (File f : files) {
            try (FileInputStream fis = new FileInputStream(f)) {
                final ObjectInputStream stream = new ObjectInputStream(fis);
                final Object key1 = stream.readObject();
                if (key1.equals(key)) {
                    return stream.readObject();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private File[] listBucketFiles(Object key) {
        final File bucketDir = getBucketDir(key);
        bucketDir.mkdirs();
        return bucketDir.listFiles();
    }

    @Override
    protected synchronized List<Map.Entry<Object, Object>> put_(Object key, Object value) {
        File[] files = listBucketFiles(key);
        for (File f : files) {
            try (FileInputStream fis = new FileInputStream(f)) {
                final ObjectInputStream stream = new ObjectInputStream(fis);
                final Object key1 = stream.readObject();
                if (key1.equals(key)) {
                    fis.close();
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                    writeKeyValueFile(key, value, f);
                    return null;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        final File bucketDir = getBucketDir(key);
        final File f = new File(bucketDir, "_"+files.length);
        try {
            writeKeyValueFile(key, value, f);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    private void writeKeyValueFile(Object key, Object value, File f) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            final ObjectOutputStream stream1 = new ObjectOutputStream(fos);
            stream1.writeObject(key);
            stream1.writeObject(value);
            stream1.flush();
        }
    }

    private File getBucketDir(Object key) {
        int hc = Math.abs(key.hashCode()) % bucketsNumber;
        return new File(rootDir, "" + hc);
    }
}
