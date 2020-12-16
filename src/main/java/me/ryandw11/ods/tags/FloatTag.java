package me.ryandw11.ods.tags;

import me.ryandw11.ods.Tag;
import me.ryandw11.ods.io.CountingOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * The float tag
 */
public class FloatTag implements Tag<Float> {
    private String name;
    private Float value;

    /**
     * Construct a float tag.
     *
     * @param name  The name of the tag.
     * @param value The value of the tag.
     */
    public FloatTag(String name, float value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public void setValue(Float s) {
        this.value = s;
    }

    @Override
    public Float getValue() {
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
        tempDos.writeFloat(value);

        dos.writeInt(cos.getCount());
        dos.write(os.toByteArray());
        tempDos.close();
    }

    @Override
    public Tag<Float> createFromData(ByteBuffer value, int length) {
        this.value = value.getFloat();
        return this;
    }

    @Override
    public byte getID() {
        return 3;
    }
}
