package me.ryandw11.ods;

import me.ryandw11.ods.tags.ObjectTag;
import org.apache.commons.io.input.CountingInputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The primary class of the ObjectDataStructure library.
 */
public class ObjectDataStructure {
    private File file;

    /**
     * The file that is to be saved to
     * @param file The file.
     */
    public ObjectDataStructure(File file){
        this.file = file;
    }

    /**
     * Grab a tag based upon the name.
     * <p>This will not work with object notation see {@link #getObject(String)} for that method.</p>
     * <p>Ensure you are storing this result with the correct data type.</p>
     * @param name The name of the tag.
     * @return The tag.
     *         <p>This will return null if no tag with the name specified is found, or if the file does not exist.</p>
     */
    public <T extends Tag<?>> T get(String name){
        try{
            if(!file.exists()) return null;
            FileInputStream fis = new FileInputStream(file);
            T out = (T) getSubData(fis.readAllBytes(), name);
            fis.close();
            return out;
        }catch(IOException ex){
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Grab a tag based upon an object key.
     * <p>This method allows you to directly get sub-objects with little overhead.</p>
     * <code>
     *     getObject("primary.firstsub.secondsub");
     * </code>
     * @param key The key to use.
     * @return The object Tag.
     *      <p>This will return null if the requested sub-object does not exist, or if the file does not exist.</p>
     */
    public <T> T getObject(String key){
        try{
            if(!file.exists()) return null;
            FileInputStream fis = new FileInputStream(file);
            T out = (T) getSubObjectData(fis.readAllBytes(), key);
            fis.close();
            return out;
        }catch(IOException ex){
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Get all of the tags in the file.
     * @return All of the tags.
     * <p>This will return null if there are no tags, or if the file does not exist.</p>
     */
    public List<Tag<?>> getAll(){
        try{
            if(!file.exists()) return null;
            FileInputStream fis = new FileInputStream(file);
            List<Tag<?>> output = getListData(fis.readAllBytes());
            fis.close();
            return output;
        }catch(IOException ex){
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Save tags to the file.
     * <p>This will overwrite the existing file. To append tags see {@link #append(Tag)} and {@link #appendAll(List)}</p>
     * @param tags The list of tags to save.
     */
    public void save(List<Tag<?>> tags){
        try{
            if(!file.exists()) file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(fos));
            for(Tag<?> tag : tags){
                tag.writeData(dos);
            }
            dos.close();
            fos.close();
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    /**
     * Append tags to the end of the file.
     * @param tag The tag to be appended.
     */
    public void append(Tag<?> tag){
        try{
            if(!file.exists()) file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(fos));

            FileInputStream fis = new FileInputStream(file);
            dos.write(fis.readAllBytes());
            fis.close();
            tag.writeData(dos);

            dos.close();
            fos.close();
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    /**
     * Append a list of tags to the end of the file.
     * @param tags The list of tags.
     */
    public void appendAll(List<Tag<?>> tags){
        try{
            if(!file.exists()) file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(fos));

            FileInputStream fis = new FileInputStream(file);
            dos.write(fis.readAllBytes());
            fis.close();
            for(Tag<?> tag : tags){
                tag.writeData(dos);
            }
            dos.close();
            fos.close();
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    /**
     * Get a tag based upon the name.
     * TODO remove byte[] from params
     * @param data The list of data.
     * @param name The name
     * @return The tag
     * @throws IOException This method does not handle the possible IO Exceptions.
     */
    private Tag<?> getSubData(byte[] data, String name) throws IOException {
        InputStream stream = new ByteArrayInputStream(data);
        BufferedInputStream bis = new BufferedInputStream(stream);
        final CountingInputStream cis = new CountingInputStream(bis);
        DataInputStream dis = new DataInputStream(cis);

        TagBuilder currentBuilder = new TagBuilder();
        int dataLength = data.length;
        while(dis.available() > 0){
            currentBuilder.setDataType(dis.readByte());
            currentBuilder.setDataSize(dis.readInt());
            currentBuilder.setStartingIndex(cis.getByteCount());
            currentBuilder.setNameSize(((Short) dis.readShort()).intValue());
            // If the name size isn't the same, then don't waste time reading the name.
            if(currentBuilder.getNameSize() != name.getBytes(StandardCharsets.UTF_8).length){
                dis.skipNBytes((currentBuilder.getStartingIndex() - cis.getByteCount()) + currentBuilder.getDataSize());
                currentBuilder = new TagBuilder();
                continue;
            }
            //TODO make sure the long math does not screw up this system.
            byte[] nameBytes = new byte[currentBuilder.getNameSize()];
            dis.readFully(nameBytes);
            String tagName = new String(nameBytes, StandardCharsets.UTF_8);
            currentBuilder.setName(tagName);
            // If the name is not correct, skip forward!
            if(!name.equals(tagName)){
                dis.skipNBytes((currentBuilder.getStartingIndex() - cis.getByteCount()) + currentBuilder.getDataSize());
                currentBuilder = new TagBuilder();
                continue;
            }

            byte[] value = new byte[((int) currentBuilder.getStartingIndex() - cis.getCount()) + currentBuilder.getDataSize()];
            dis.readFully(value);
            currentBuilder.setValueBytes(value);
            dis.close();
            return currentBuilder.process();
        }
        dis.close();
        return null;
    }

    /**
     * Get a tag based upon the name.
     * TODO remove byte[] from params
     * @param data The list of data.
     * @param key The key
     * @return The tag
     * @throws IOException This method does not handle the possible IO Exceptions.
     */
    private Tag<?> getSubObjectData(byte[] data, String key) throws IOException {
        InputStream stream = new ByteArrayInputStream(data);
        BufferedInputStream bis = new BufferedInputStream(stream);
        final CountingInputStream cis = new CountingInputStream(bis);
        DataInputStream dis = new DataInputStream(cis);
        String name = key.split("\\.")[0];
        String otherKey = getKey(key.split("\\."));

        TagBuilder currentBuilder = new TagBuilder();
        while(dis.available() > 0){
            currentBuilder.setDataType(dis.readByte());
            currentBuilder.setDataSize(dis.readInt());
            currentBuilder.setStartingIndex(cis.getByteCount());
            currentBuilder.setNameSize(((Short) dis.readShort()).intValue());
            // If the name size isn't the same, then don't waste time reading the name.
            if(currentBuilder.getNameSize() != name.getBytes(StandardCharsets.UTF_8).length){
                dis.skipNBytes((currentBuilder.getStartingIndex() - cis.getByteCount()) + currentBuilder.getDataSize());
                currentBuilder = new TagBuilder();
                continue;
            }
            //TODO make sure the long math does not screw up this system.
            byte[] nameBytes = new byte[currentBuilder.getNameSize()];
            dis.readFully(nameBytes);
            String tagName = new String(nameBytes, StandardCharsets.UTF_8);
            currentBuilder.setName(tagName);
            // If the name is not correct, skip forward!
            if(!name.equals(tagName)){
                dis.skipNBytes((currentBuilder.getStartingIndex() - cis.getByteCount()) + currentBuilder.getDataSize());
                currentBuilder = new TagBuilder();
                continue;
            }

            byte[] value = new byte[((int) currentBuilder.getStartingIndex() - cis.getCount()) + currentBuilder.getDataSize()];
            dis.readFully(value);
            currentBuilder.setValueBytes(value);
            dis.close();
            if(otherKey != null)
                return getSubObjectData(currentBuilder.getValueBytes(), otherKey);
            return currentBuilder.process();
        }
        dis.close();
        return null;
    }

    private String getKey(String[] s){
        List<String> list = new ArrayList<>(Arrays.asList(s));
        list.remove(0);
        if(list.size() == 1) return list.get(0);
        if(list.size() < 1) return null;

        return String.join(".", list);
    }

    /**
     * Get the list of tags based upon bytes.
     * <p>This is meant for internal use only.</p>
     * @param data the array of bytes
     * @return The List of tags.
     * @throws IOException This method does not handle any of the possible IO Exceptions.
     */
    public static List<Tag<?>> getListData(byte[] data) throws IOException {
        List<Tag<?>> output = new ArrayList<>();
        InputStream stream = new ByteArrayInputStream(data);
        BufferedInputStream bis = new BufferedInputStream(stream);
        final CountingInputStream cis = new CountingInputStream(bis);
        DataInputStream dis = new DataInputStream(cis);

        TagBuilder currentBuilder = new TagBuilder();
        while(dis.available() > 0){
            currentBuilder.setDataType(dis.readByte());
            currentBuilder.setDataSize(dis.readInt());
            currentBuilder.setStartingIndex(cis.getByteCount());
            currentBuilder.setNameSize(dis.readShort());
            //TODO make sure the long math does not screw up this system.
            byte[] nameBytes = new byte[currentBuilder.getNameSize()];
            dis.readFully(nameBytes);
            String tagName = new String(nameBytes, StandardCharsets.UTF_8);
            currentBuilder.setName(tagName);
            byte[] value = new byte[((int) currentBuilder.getStartingIndex() - cis.getCount()) + currentBuilder.getDataSize()];
            dis.readFully(value);
            currentBuilder.setValueBytes(value);
            output.add(currentBuilder.process());
        }
        dis.close();
        return output;
    }
}
