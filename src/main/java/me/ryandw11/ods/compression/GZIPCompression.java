package me.ryandw11.ods.compression;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Compress the file using the GZIP compression format.
 */
public class GZIPCompression implements Compressor {
    @Override
    public InputStream getInputStream(InputStream file) throws IOException {
        return new GZIPInputStream(file);
    }

    @Override
    public OutputStream getOutputStream(OutputStream file) throws IOException {
        return new GZIPOutputStream(file);
    }
}
