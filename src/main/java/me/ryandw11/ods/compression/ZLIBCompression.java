package me.ryandw11.ods.compression;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Compress the file using the ZLIB compression format.
 */
public class ZLIBCompression implements Compressor {
    @Override
    public InputStream getInputStream(FileInputStream file) {
        return new InflaterInputStream(file);
    }

    @Override
    public OutputStream getOutputStream(FileOutputStream file) {
        return new DeflaterOutputStream(file);
    }
}
