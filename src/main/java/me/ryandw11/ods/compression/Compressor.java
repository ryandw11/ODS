package me.ryandw11.ods.compression;

import java.io.*;

public interface Compressor {
    InputStream getInputStream(FileInputStream file) throws IOException;
    OutputStream getOutputStream(FileOutputStream file) throws IOException;
}
