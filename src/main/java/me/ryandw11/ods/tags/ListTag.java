package me.ryandw11.ods.tags;

import me.ryandw11.ods.ObjectDataStructure;
import me.ryandw11.ods.Tag;
import org.apache.commons.io.output.CountingOutputStream;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class ListTag<T extends Tag<?>> implements Tag<List<T>> {
    private String name;
    private List<T> value;

    public ListTag(String name, List<T> value){
        this.name = name;
        this.value = value;
    }

    @Override
    public void setValue(List<T> s) {
        this.value = s;
    }

    @Override
    public List<T> getValue() {
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
        // The size of the array.
        for(T tag : this.value){
            tag.setName("");
            tag.writeData(tempDos);
        }

        dos.writeInt(cos.getCount());
        dos.write(os.toByteArray());
        tempDos.close();
    }

    @Override
    public Tag<List<T>> createFromData(byte[] value) {
        List<Tag<?>> data;
        try{
            data = ObjectDataStructure.getListData(value);
        }catch(IOException ex){
            ex.printStackTrace();
            return null;
        }
        this.value = data.stream().map(d -> (T) d).collect(Collectors.toList());
        return this;
    }

    @Override
    public byte getID() {
        return 9;
    }
}
