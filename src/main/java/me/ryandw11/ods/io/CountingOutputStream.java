package me.ryandw11.ods.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A custom output stream that keeps track of the amount of bytes being written.
 */
public class CountingOutputStream extends FilterOutputStream {

    private long count;

    public CountingOutputStream(OutputStream out) {
        super(out);
        count = 0;
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        count++;
    }

    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
        count += b.length;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        count += len;
    }

    /**
     * Get the count of the bytes.
     *
     * @return The count of the bytes.
     */
    public int getCount() {
        return (int) count;
    }

    /**
     * Get the number of bytes counted in a long.
     *
     * @return The number of bytes counted.
     */
    public long getByteCount() {
        return count;
    }

    /**
     * Reset the byte counter.
     */
    public void resetCount() {
        count = 0;
    }
}
