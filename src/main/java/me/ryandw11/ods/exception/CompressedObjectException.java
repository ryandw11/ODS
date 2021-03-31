package me.ryandw11.ods.exception;

/**
 * This exception is thrown when you try and transverse a compressed object using get(), find(), set(), etc.
 * <p>To prevent this exception, first obtain the compress object to decompress it, then get the tags inside.</p>
 */
public class CompressedObjectException extends RuntimeException {
    public CompressedObjectException(String s) {
        super(s);
    }
}
