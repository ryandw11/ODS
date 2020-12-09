package me.ryandw11.ods.compression;

import java.io.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

public class GZIPCompression implements Compressor {
    @Override
    public InputStream getInputStream(FileInputStream file) throws IOException {
        return new GZIPInputStream(file);
    }

    @Override
    public OutputStream getOutputStream(FileOutputStream file) throws IOException {
        return new GZIPOutputStream(file);
    }
}
