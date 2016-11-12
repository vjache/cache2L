package com.vjache.cache;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Memory Mapped File Cache. Whole file subdivided on fixed number of equal sized regions(buckets). Each region
 * memory mapped on to separate byte buffer.
 *
 * Current solution is:
 *  * not very efficient for concurrency
 *  * not fast when rewriting an existing key (more effective compaction algorithms required)
 *  * would be great to make 'put(...)' to have internal queue for each bucket for async write
 *    (to make eviction operation of prev levels faster).
 */
public class MMFileCache extends CacheLayer {

    private final File file;
    private final MappedByteBuffer[] buckectsRw;
    private final ByteBuffer[] buckectsRo;
    private final long bufferSize;


    public MMFileCache(File rootDir, Cache next) throws IOException {
        this(rootDir, 1000, 8 * 1024 * 1024, next);
    }
    public MMFileCache(File rootDir, int bucketsNumber, int bucketSize, Cache next) throws IOException {
        super(next);
        buckectsRw = new MappedByteBuffer[bucketsNumber];
        buckectsRo = new ByteBuffer[buckectsRw.length];
        bufferSize = bucketSize;
        rootDir.mkdirs();
        this.file = new File(rootDir, "_" + buckectsRw.length + "_" + bufferSize);
        final FileChannel fc = new RandomAccessFile(file, "rw").getChannel();

        for(int i = 0; i < buckectsRw.length; i++) {
            buckectsRw[i] = fc.map(FileChannel.MapMode.READ_WRITE, i * bufferSize, bufferSize);
            jumpEnd(buckectsRw[i]);
            buckectsRo[i] = buckectsRw[i].asReadOnlyBuffer();
        }
    }

    @Override
    protected synchronized Object get_(Object key) {
        final byte[] keyBytes = objectToBytes(key);
        final int hc = Arrays.hashCode(keyBytes);
        final MappedByteBuffer buff = getByteBufferByKeyRw(hc);

        byte[] valBytes = seek(hc, keyBytes, buff);

        return objectFromBytes(valBytes);
    }


    @Override
    protected synchronized List<Map.Entry<Object, Object>> put_(Object key, Object value) {
        final byte[] keyBytes = objectToBytes(key);
        final byte[] valBytes = objectToBytes(value);
        final int hc = Arrays.hashCode(keyBytes);
        final MappedByteBuffer buff = getByteBufferByKeyRw(hc);

        seekAndRemove(hc, keyBytes, buff);

        // hc:Int, prio:long, key_len:Int, val_len:Int, key:Bytes, val:Bytes
        buff.put((byte)1).putInt(hc).putLong(0).putInt(keyBytes.length).putInt(valBytes.length).put(keyBytes).put(valBytes);
//        buff.force();

        return null;
    }

    private void seekAndRemove(int hc, byte[] keyBytes, ByteBuffer buff0) {
        ByteBuffer buff = buff0.duplicate();
        buff.position(0);

        while(buff.hasRemaining())
        {
            // hc:Int, prio:long, key_len:Int, val_len:Int, key:Bytes,  val:Bytes
            byte x = buff.get();
            if(x == 0) return;
            int keyHC = buff.getInt();
            buff.getLong(); // prio
            int keyLen = buff.getInt();
            int valLen = buff.getInt();
            if(keyHC != hc)
            {
                buff.position(buff.position() + keyLen + valLen);
                continue;
            }
            byte[] keyBytes1 = new byte[keyLen];
            buff.get(keyBytes1);
            buff.position(buff.position() + valLen);
            if(Arrays.equals(keyBytes, keyBytes1)) {
                buff0.position(buff0.position() - buff.position());
                buff.compact();
                break;
            }
            buff = buff.slice();
        }
    }

    private void jumpEnd(ByteBuffer buff) {
        while(buff.hasRemaining()) {
            // hc:Int, prio:long, key_len:Int, val_len:Int, key:Bytes,  val:Bytes
            byte x = buff.get();
            if (x == 0) {
                buff.position(buff.position() - 1);
                return;
            }
            int keyHC = buff.getInt();
            buff.getLong(); // prio
            int keyLen = buff.getInt();
            int valLen = buff.getInt();
            buff.position(buff.position() + keyLen + valLen);
        }
    }

    private byte[] seek(int hc, byte[] keyBytes, ByteBuffer buff0) {
        ByteBuffer buff = buff0.duplicate();
        buff.position(0);

        while(buff.hasRemaining())
        {
            // hc:Int, prio:long, key_len:Int, val_len:Int, key:Bytes,  val:Bytes
            byte x = buff.get();
            if(x == 0) return null;
            int keyHC = buff.getInt();
            buff.getLong(); // prio
            int keyLen = buff.getInt();
            int valLen = buff.getInt();
            if(keyHC != hc)
            {
                buff.position(buff.position() + keyLen + valLen);
                continue;
            }
            byte[] keyBytes1 = new byte[keyLen];
            buff.get(keyBytes1);
            if(Arrays.equals(keyBytes, keyBytes1)) {
                byte[] valBytes = new byte[valLen];
                buff.get(valBytes);
                return valBytes;
            }
            else
                buff.position(buff.position() + valLen);
        }
        return null;
    }

    private byte[] objectToBytes(Object key) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ObjectOutputStream stream = new ObjectOutputStream(baos);
            stream.writeObject(key);
            stream.flush();
            return baos.toByteArray();
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }


    private Object objectFromBytes(byte[] valBytes) {
        try {
            ByteArrayInputStream baos = new ByteArrayInputStream(valBytes);
            final ObjectInputStream stream = new ObjectInputStream(baos);
            return stream.readObject();
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MappedByteBuffer getByteBufferByKeyRw(int keyHc) {
        int hc = Math.abs(keyHc) % buckectsRw.length;
        return buckectsRw[hc];
    }
}
