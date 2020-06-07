package me.ryandw11.ods.tags;

import me.ryandw11.ods.ObjectDataStructure;
import me.ryandw11.ods.Tag;
import org.apache.commons.io.output.CountingOutputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The list tag.
 * <p>This tag holds a list of a single type of tag. Example:</p>
 * <code>
 *     ListTag<StringTag> listTag = new ListTag<>("MyTag", new ArrayList());
 *     listTag.addTag(new StringTag("", "My String"));
 * </code>
 * <p>The name of all tags in the list are set to "" to conserve space.</p>
 * <p>To get the java.util.List use the getValue() method.</p>
 * @param <T> The tag to list.
 */
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

    /**
     * Add a tag to the list tag.
     * @param tag The tag to add.
     */
    public void addTag(T tag){
        value.add(tag);
    }

    /**
     * Get a tag from the list of tags.
     * @param i The index of the desired tag.
     * @return The tag.
     */
    public T getTag(int i){
        return value.get(i);
    }

    /**
     * Remove a specific tag from the list.
     * @param tag The tag.
     */
    public void removeTag(T tag){
        value.remove(tag);
    }

    /**
     * Remove a specific tag from the list.
     * @param i The index of the tag.
     */
    public void removeTag(int i){
        value.remove(i);
    }

    /**
     * Remove all tags from the list.
     */
    public void removeAllTags(){
        value.clear();
    }

    /**
     * Get the index of a certain tag.
     * @param tag The tag to find.
     * @return The index of the desired tag.
     */
    public int indexOf(T tag){
        return value.indexOf(tag);
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
