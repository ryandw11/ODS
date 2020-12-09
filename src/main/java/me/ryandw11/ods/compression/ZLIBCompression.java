package me.ryandw11.ods.compression;

import java.io.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

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
