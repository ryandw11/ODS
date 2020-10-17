package me.ryandw11.ods.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ODSIOUtils {

    /**
     * Convert an input stream to byte array.
     * <p>This is based on the function in the apache io commons IOUtil class.</p>
     *
     * @param input The input stream.
     * @return The byte array.
     * @throws IOException Throws IO Exception if a read error occurs.
     */
    public static byte[] toByteArray(final InputStream input) throws IOException {
        try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            copy(input, output, new byte[8192]);
            return output.toByteArray();
        }
    }

    /**
     * Copy the input stream to the output stream.
     * <p>This function is based on the function in the apache io commons IOUtil class.</p>
     *
     * @param input  The input stream.
     * @param output The output stream.
     * @param buffer The buffer to use.
     * @throws IOException Throws IO Exception if a read error occurs.
     */
    private static void copy(final InputStream input, final OutputStream output, final byte[] buffer) throws IOException {
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
    }
}
