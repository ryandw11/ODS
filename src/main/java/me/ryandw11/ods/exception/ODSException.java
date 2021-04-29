package me.ryandw11.ods.exception;

import java.io.IOException;

/**
 * The general exception to handle IO errors.
 */
public class ODSException extends RuntimeException {
    private IOException ex;

    public ODSException(String s, IOException ex) {
        super(s);
        this.ex = ex;
    }

    public ODSException(String s) {
        super(s);
    }

    /**
     * Get the IOException that occurred.
     *
     * @return The IOException. Null if no IOException happened.
     */
    public IOException getIOException() {
        return ex;
    }

    @Override
    public void printStackTrace() {
        super.printStackTrace();
        ex.printStackTrace();
    }
}
