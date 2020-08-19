package me.ryandw11.ods;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The main tag interface.
 *
 * @param <T> The object that the tag is representing.
 */
public interface Tag<T> {
    /**
     * Get the value of the tag.
     *
     * @return The value
     */
    T getValue();

    /**
     * Set the value of the tag
     *
     * @param t The value
     */
    void setValue(T t);

    /**
     * Get the name of the tag.
     *
     * @return The name of the tag.
     */
    String getName();

    /**
     * Set the name of the tag
     *
     * @param name The name
     */
    void setName(String name);

    /**
     * Write the data of the tag to an output stream.
     *
     * @param dos The output stream.
     * @throws IOException If something goes wrong.
     */
    void writeData(DataOutputStream dos) throws IOException;

    /**
     * Turn an array of bytes into the Tag.
     *
     * @param value  The array of bytes
     * @param length The size of the value in bytes.
     * @return The instance of the tag.
     */
    Tag<T> createFromData(ByteBuffer value, int length);

    /**
     * Get the ID of the tag.
     *
     * @return The ID.
     */
    byte getID();
}
