package me.ryandw11.ods.tags;

import me.ryandw11.ods.Tag;
import me.ryandw11.ods.internal.InternalUtils;
import me.ryandw11.ods.io.CountingOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The object tag.
 */
public class ObjectTag implements Tag<List<Tag<?>>> {
    private String name;
    private List<Tag<?>> value;

    /**
     * Construct an object tag with existing tags.
     * @param name The name of the object.
     * @param value The list of tags to add.
     */
    public ObjectTag(String name, List<Tag<?>> value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Construct an object tag with no existing tags.
     * @param name The name of the object.
     */
    public ObjectTag(String name) {
        this.name = name;
        this.value = new ArrayList<>();
    }

    /**
     * Add a tag to the object.
     *
     * @param t The tag to add.
     */
    public void addTag(Tag<?> t) {
        value.add(t);
    }

    /**
     * Get a tag from the object.
     *
     * @param name The name of the tag.
     * @return The tag. (Returns null if not found).
     */
    public Tag<?> getTag(String name) {
        List<Tag<?>> results = value.stream().filter(tag -> tag.getName().equals(name)).collect(Collectors.toList());
        if (results.size() < 1)
            return null;

        return results.get(0);
    }

    /**
     * Remove the specified tag from the object.
     * @param tag The tag to remove.
     */
    public void removeTag(Tag<?> tag){
        value.remove(tag);
    }

    /**
     * Remove all tags that have the specified name.
     * @param name The name to remove.
     */
    public void removeTag(String name){
        value.removeIf(tag -> tag.getName().equals(name));
    }

    /**
     * Remove all tags from an object.
     */
    public void removeAllTags(){
        value.clear();
    }

    /**
     * If the object has a specified tag.
     *
     * @param name The tag's name.
     * @return The tag.
     */
    public boolean hasTag(String name) {
        return value.stream().filter(tag -> tag.getName().equals(name)).collect(Collectors.toList()).size() > 0;
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
        for (Tag<?> tag : this.value) {
            tag.writeData(tempDos);
        }

        dos.writeInt(cos.getCount());
        dos.write(os.toByteArray());
        tempDos.close();
    }

    @Override
    public Tag<List<Tag<?>>> createFromData(ByteBuffer value, int length) {
        List<Tag<?>> data;
        data = InternalUtils.getListData(value, length);
        this.value = data;
        return this;
    }

    @Override
    public byte getID() {
        return 11;
    }
}
