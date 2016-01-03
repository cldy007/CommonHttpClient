package com.android.common.CommonHttpClient;

import android.os.SystemClock;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by cl on 12/28/15.
 */
public class DiskBasedCache implements Cache {

    private final Map<String , CacheHeader> mEntries =
            new LinkedHashMap<String , CacheHeader>(16,.75f,true); //LRU

    private long mTotalSize = 0;
    private final File mRootDirectory;
    private final int mMaxCacheSizeInBytes;
    private static final int DEFAULT_DISK_USAGE_BYTES = 5 * 1024 * 1024;
    private static final float HYSTERESIS_FACTOR = 0.9f;
    private static final int CACHE_MAGIC = 0x20151228;

    public DiskBasedCache(File rootDir , int maxCacheSize){
        mRootDirectory = rootDir;
        mMaxCacheSizeInBytes = maxCacheSize;
    }

    public DiskBasedCache(File rootDir){
        this(rootDir, DEFAULT_DISK_USAGE_BYTES);
    }

    @Override
    public Entry get(String key) {
        CacheHeader header = mEntries.get(key);
        if(header == null){
            return null;
        }
        File file = getFileForKey(key);
        CountingInputStream cis = null;
        try {
            cis = new CountingInputStream(new BufferedInputStream(new FileInputStream(file)));
            CacheHeader.readHeader(cis); // good
            byte[] data = streamToBytes(cis , (int)(file.length() - cis.bytesRead));
            return header.toCacheEntry(data);
        } catch (IOException e) {
            remove(key);
            return null;
        } finally {
            if(cis != null){
                try {
                    cis.close();
                } catch (IOException e) {
                    return null;
                }
            }
        }
    }

    @Override
    public void put(String key, Entry entry) {
        pruneIfNeed(entry.data.length);
        File file = getFileForKey(key);

        try {
            BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
            CacheHeader e = new CacheHeader(key,entry);
            boolean success = e.writeHeader(os);
            if(!success){
                os.close();
                throw new IOException();
            }
            os.write(entry.data);
            os.close();
            putEntry(key,e);
            return;
        } catch (IOException e) {
        }
        file.delete();
    }

    @Override
    public void initialize() {
        if(!mRootDirectory.exists()){
            if(!mRootDirectory.mkdirs()){

            }
            return;
        }
        File[] files = mRootDirectory.listFiles();
        if(files == null){
            return;
        }
        for(File file : files){
            BufferedInputStream fis = null;
            try {
                fis = new BufferedInputStream(new FileInputStream(file));
                CacheHeader header = CacheHeader.readHeader(fis);
                header.size = file.length();
                putEntry(header.key , header);
            } catch (IOException e) {
                if(file != null){
                    file.delete();
                }
            } finally {
                if(fis != null){
                    try {
                        fis.close();
                    } catch (IOException e) {
                    }
                }
            }
        }


    }

    private void putEntry(String key, CacheHeader header){
        if(!mEntries.containsKey(key)){
            mTotalSize += header.size;
        } else {
            CacheHeader old = mEntries.get(key);
            mTotalSize += (header.size - old.size);
        }

        mEntries.put(key , header);
    }

    private void removeEntry(String key){
        CacheHeader header = mEntries.get(key);
        if(header != null){
            mTotalSize -= header.size;
            mEntries.remove(key);
        }
    }

    @Override
    public synchronized void invalidate(String key, boolean fullExpire) {
        Entry entry = get(key);
        if(entry != null){
            entry.softTtl = 0;
            if(fullExpire){
                entry.ttl = 0;
            }
            put(key , entry);
        }

    }

    @Override
    public void remove(String key) {
        boolean deleted = getFileForKey(key).delete();
        removeEntry(key);
        if(!deleted){
            // TODO log
        }
    }

    @Override
    public void clear() {
        File[] files = mRootDirectory.listFiles();
        if(files != null){
            for(File file : files){
                file.delete();
            }
        }
        mEntries.clear();
        mTotalSize = 0;
    }

    private static class CountingInputStream extends FilterInputStream {
        private int bytesRead = 0 ;
        private CountingInputStream(InputStream in){
            super(in);
        }

        @Override
        public int read() throws IOException {
            int result = super.read();
            if(result != -1){
                bytesRead ++;
            }
            return result;
        }

        @Override
        public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
            int result = super.read(buffer, byteOffset, byteCount);
            if(result != -1){
                bytesRead += result;
            }
            return result;
        }
    }

    static class CacheHeader{
        public long size;
        public String key;
        public String etag;//Etag
        public long serverDate; //Date
        public long lastModified; // Last-Modified
        public long ttl;
        public long softTtl;
        public Map<String ,String > responseHeader;
        private CacheHeader(){}

        public CacheHeader(String key,Entry entry){
            this.key = key;
            this.size = entry.data.length;
            this.etag = entry.etag;
            this.serverDate = entry.serverDate;
            this.lastModified = entry.lastModified;
            this.ttl = entry.ttl;
            this.softTtl = entry.softTtl;
            this.responseHeader = entry.responseHeaders;
        }

        public static CacheHeader readHeader(InputStream is) throws IOException{
            CacheHeader header = new CacheHeader();
            int magic = readInt(is);
            if(magic != CACHE_MAGIC ){
                throw new IOException();
            }
            header.key = readString(is);
            header.etag = readString(is);
            if(header.etag.equals("")){
                header.etag = null;
            }
            header.serverDate = readLong(is);
            header.lastModified = readLong(is);
            header.ttl = readLong(is);
            header.softTtl = readLong(is);
            header.responseHeader = readStringStringMap(is);

            return header;
        }

        public Entry toCacheEntry(byte[] data){
            Entry entry = new Entry();
            entry.data = data;
            entry.etag = etag;
            entry.serverDate = serverDate;
            entry.lastModified = lastModified;
            entry.ttl = ttl;
            entry.softTtl = softTtl;
            entry.responseHeaders = responseHeader;
            return entry;
        }

        public boolean writeHeader(OutputStream os){
            try {
                writeInt(os,CACHE_MAGIC);
                writeString(os,key);
                writeString(os, etag == null ? "" : etag);
                writeLong(os,serverDate);
                writeLong(os,lastModified);
                writeLong(os,ttl);
                writeLong(os,softTtl);
                writeStringStringMap(responseHeader,os);
                os.flush();
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    private void pruneIfNeed(int neededSpace){
        if((mTotalSize + neededSpace) < mMaxCacheSizeInBytes){
            return;
        }

        long before = mTotalSize;
        int prunedFiles = 0;
        long startTime = SystemClock.elapsedRealtime();

        Iterator<Map.Entry<String , CacheHeader>> iterator = mEntries.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String ,CacheHeader> entry = iterator.next();
            CacheHeader e = entry.getValue();
            boolean deleted = getFileForKey(e.key).delete();
            if(deleted){
                mTotalSize -= e.size;
            } else {

            }
            iterator.remove();
            prunedFiles ++;
            if((mTotalSize + neededSpace) < mMaxCacheSizeInBytes * HYSTERESIS_FACTOR){
                break;
            }
        }
    }
    static void writeInt(OutputStream os, int n) throws IOException {
        os.write((n >> 0) & 0xff);
        os.write((n >> 8) & 0xff);
        os.write((n >> 16) & 0xff);
        os.write((n >> 24) & 0xff);
    }

    static void writeLong(OutputStream os, long n) throws IOException {
        os.write((byte)(n >>> 0));
        os.write((byte)(n >>> 8));
        os.write((byte)(n >>> 16));
        os.write((byte)(n >>> 24));
        os.write((byte)(n >>> 32));
        os.write((byte)(n >>> 40));
        os.write((byte)(n >>> 48));
        os.write((byte)(n >>> 56));
    }

    static void writeString(OutputStream os, String s) throws IOException {
        byte[] b = s.getBytes("UTF-8");
        writeLong(os, b.length);
        os.write(b, 0, b.length);
    }
    static void writeStringStringMap(Map<String, String> map, OutputStream os) throws IOException {
        if (map != null) {
            writeInt(os, map.size());
            for (Map.Entry<String, String> entry : map.entrySet()) {
                writeString(os, entry.getKey());
                writeString(os, entry.getValue());
            }
        } else {
            writeInt(os, 0);
        }
    }


    private static int read(InputStream is) throws IOException{
        int b = is.read(); // read single byte
        if(b == -1){
            throw new EOFException();
        }
        return b;
    }

    private static byte[] streamToBytes(InputStream in, int length) throws IOException {
        byte[] bytes = new byte[length];
        int count;
        int pos = 0;
        while (pos < length && ((count = in.read(bytes, pos, length - pos)) != -1)) {
            pos += count;
        }
        if (pos != length) {
            throw new IOException("Expected " + length + " bytes, read " + pos + " bytes");
        }
        return bytes;
    }

    static int readInt(InputStream is) throws IOException{
        int n = 0;
        n |= (read(is) << 0);
        n |= (read(is) << 8);
        n |= (read(is) << 16);
        n |= (read(is) << 24);
        return n;
    }
    static long readLong(InputStream is) throws IOException {
        long n = 0;
        n |= ((read(is) & 0xFFL) << 0);
        n |= ((read(is) & 0xFFL) << 8);
        n |= ((read(is) & 0xFFL) << 16);
        n |= ((read(is) & 0xFFL) << 24);
        n |= ((read(is) & 0xFFL) << 32);
        n |= ((read(is) & 0xFFL) << 40);
        n |= ((read(is) & 0xFFL) << 48);
        n |= ((read(is) & 0xFFL) << 56);
        return n;
    }
    static String readString(InputStream is) throws IOException {
        int n = (int) readLong(is);
        byte[] b = streamToBytes(is, n);
        return new String(b, "UTF-8");
    }
    static Map<String ,String > readStringStringMap(InputStream is) throws IOException{
        int size = readInt(is);
        Map<String ,String > result = (size == 0) ? Collections.<String ,String>emptyMap():new HashMap<String ,String >(size);
        for(int i = 0; i< size ; i++){
            String key = readString(is).intern();
            String value = readString(is).intern();
            result.put(key,value);
        }
        return result;
    }

    public File getFileForKey(String key){
        return new File(mRootDirectory , getFileNameForKey(key));
    }

    private String getFileNameForKey(String key){
        int firstHalfLength = key.length() /2 ;
        String localFilename = String.valueOf(key.substring(0,firstHalfLength).hashCode());
        localFilename += String.valueOf(key.substring(firstHalfLength).hashCode());
        return localFilename;
    }

}
