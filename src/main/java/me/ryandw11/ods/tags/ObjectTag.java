package me.ryandw11.ods.tags;

import me.ryandw11.ods.ObjectDataStructure;
import me.ryandw11.ods.Tag;
import org.apache.commons.io.output.CountingOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ObjectTag implements Tag<List<Tag<?>>> {
    private String name;
    private List<Tag<?>> value;

    public ObjectTag(String name, List<Tag<?>> value){
        this.name = name;
        this.value = value;
    }

    public ObjectTag(String name){
        this.name = name;
        this.value = new ArrayList<>();
    }

    public void addTag(Tag<?> t){
        value.add(t);
    }

    public Tag<?> getTag(String name){
        return value.stream().filter(tag -> tag.getName().equals(name)).collect(Collectors.toList()).get(0);
    }

    @Override
    public void setValue(List<Tag<?>> s) {
        this.value = s;
    }

    @Override
    public List<Tag<?>> getValue() {
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
        for(Tag<?> tag : this.value){
            tag.writeData(tempDos);
        }

        dos.writeInt(cos.getCount());
        dos.write(os.toByteArray());
        tempDos.close();
    }

    @Override
    public Tag<List<Tag<?>>> createFromData(byte[] value) {
        List<Tag<?>> data;
        try{
            data = ObjectDataStructure.getListData(value);
        }catch(IOException ex){
            ex.printStackTrace();
            return null;
        }
        this.value = data;
        return this;
    }

    @Override
    public byte getID() {
        return 11;
    }
}
