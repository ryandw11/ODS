package me.ryandw11.ods.tags;

import me.ryandw11.ods.Tag;
import me.ryandw11.ods.io.CountingOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * The int tag.
 */
public class IntTag implements Tag<Integer> {
    private int value;
    private String name;

    /**
     * Construct an integer tag.
     *
     * @param name  The name of the tag.
     * @param value The value of the tag.
     */
    public IntTag(String name, int value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public void setValue(Integer s) {
        this.value = s;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void writeData(DataOutputStream dos) throws IOException {
        // Indicates the string
        dos.write(getID());
        //Create a new DataOutputStream
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        CountingOutputStream cos = new CountingOutputStream(os);
        DataOutputStream tempDos = new DataOutputStream(cos);

        tempDos.writeShort(name.getBytes(StandardCharsets.UTF_8).length);
        tempDos.write(name.getBytes(StandardCharsets.UTF_8));
        tempDos.writeInt(value);

        dos.writeInt(cos.getCount());
        dos.write(os.toByteArray());
        tempDos.close();
    }

    @Override
    public Tag<Integer> createFromData(ByteBuffer value, int length) {
        this.value = value.getInt();
        return this;
    }

    @Override
    public byte getID() {
        return 2;
    }
}
