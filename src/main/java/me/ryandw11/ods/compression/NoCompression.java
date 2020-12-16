package me.ryandw11.ods.compression;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * This will not compress the file.
 */
public class NoCompression implements Compressor {
    @Override
    public InputStream getInputStream(InputStream file) {
        return file;
    }

    @Override
    public OutputStream getOutputStream(OutputStream file) {
        return file;
    }
}
