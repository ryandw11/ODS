package me.ryandw11.ods.tests;

import me.ryandw11.ods.Tag;
import org.apache.commons.io.output.CountingOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * This is an example custom tag.
 */
public class CustomTag implements Tag<String> {

    /**
     * The name and value must be stored.
     */
    private String value;
    private String name;

    /**
     * This constructor MUST exist inorder for the custom tag to work.
     * The first parameter must be the name followed by the value. (name must be a string).
     */
    public CustomTag(String name, String value){
        this.name = name;
        this.value = value;
    }

    @Override
    public void setValue(String s) {
        this.value = s;
    }

    @Override
    public String getValue() {
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
        // Write out the custom tag ID
        // The id is not included in the size of the tag.
        dos.write(getID());
        //Create a new DataOutputStream that way the size of the tag
        // can be counted.
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        // This output stream is apart of the Apache Commons Library
        CountingOutputStream cos = new CountingOutputStream(os);
        DataOutputStream tempDos = new DataOutputStream(cos);

        // Write the size of the name (Must be a short)
        tempDos.writeShort(name.getBytes(StandardCharsets.UTF_8).length);
        // Write the name.
        tempDos.write(name.getBytes(StandardCharsets.UTF_8));
        // write the value.
        tempDos.write(value.getBytes(StandardCharsets.UTF_8));

        // write the size to the main output stream.
        dos.writeInt(cos.getCount());
        // write the temporary output stream to the main stream.
        dos.write(os.toByteArray());

        // Close all of the temporary streams. (NOT THE MAIN ONE)
        cos.close();
        os.close();
        tempDos.close();
    }

    @Override
    public Tag<String> createFromData(ByteBuffer value, int length) {
        // Only read [length] number of bytes. Reading any more will cause
        // an exception to occur.
        byte[] stringData = new byte[length];
        value.get(stringData);
        this.value = new String(stringData, StandardCharsets.UTF_8);
        return this;
    }

    @Override
    public byte getID() {
        return 20;
    }
}
