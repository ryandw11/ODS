package me.ryandw11.ods.compression;

import java.io.*;

/**
 * This interface allows different compression algorithms to be used.
 * <p>
 * An input and output stream is required. You must create an input stream and output stream on top of this interface
 * in order to use a custom algorithm. If the algorithm you are using already has an input and output stream
 * implementation, then you can just simply return them here.
 *
 * @since 1.0.3
 */
public interface Compressor {
    /**
     * Get the input stream for the compression.
     *
     * @param file The file input stream for the file.
     * @return The compression input stream.
     * @throws IOException An IOException if one occurs.
     */
    InputStream getInputStream(FileInputStream file) throws IOException;

    /**
     * Get the output stream from the compression.
     *
     * @param file The file output stream for the file.
     * @return The compression output stream.
     * @throws IOException An IOException if one occurs.
     */
    OutputStream getOutputStream(FileOutputStream file) throws IOException;
}
