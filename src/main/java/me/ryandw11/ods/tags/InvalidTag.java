package me.ryandw11.ods.tags;

import me.ryandw11.ods.Tag;
import me.ryandw11.ods.exception.ODSException;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This tag represents a CustomTag that has not been defined using {@link me.ryandw11.ods.ODS#registerCustomTag(Tag)}.
 * <p>ODS will only return this tag when the {@link me.ryandw11.ods.ODS#allowUndefinedTags(boolean)} is enabled.</p>
 * <p><b>This class is not to be constructed.</b></p>
 */
public class InvalidTag implements Tag<byte[]> {
    private String name;
    private byte[] value;

    /**
     * Construct an invalid tag.
     *
     * @param name The name.
     * @deprecated Do not use this constructor.
     */
    public InvalidTag(String name) {
        this.name = name;
    }

    @Override
    public byte[] getValue() {
        return value;
    }

    @Override
    public void setValue(byte[] bytes) {
        this.value = bytes;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void writeData(DataOutputStream dos) throws IOException {
        throw new ODSException("Cannot write an InvalidTag.");
    }

    @Override
    public Tag<byte[]> createFromData(ByteBuffer value, int length) {
        byte[] data = new byte[length];
        value.get(data);
        this.value = data;

        return this;
    }

    @Override
    public byte getID() {
        return 0;
    }
}
