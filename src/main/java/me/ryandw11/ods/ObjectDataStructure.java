package me.ryandw11.ods;

import me.ryandw11.ods.exception.ODSException;
import me.ryandw11.ods.util.KeyScout;
import me.ryandw11.ods.util.KeyScoutChild;
import org.apache.commons.io.input.CountingInputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.*;

/**
 * The primary class of the ObjectDataStructure library.
 * <p>Most methods in ODS use what is called a key to reference objects within the file. A key allows you to grab specific
 * information within the file. For example: If you wanted a specific string inside of an object with lots of information,
 * you can get that specific string without any other data. If you have an object named Car and you wanted to get a string tag named
 * owner from the inside the object, then the key for that would be:</p>
 * <code>
 *     Car.owner
 * </code>
 * <p>Let's say that the owner is an object called 'Owner' and you want to get the age of the owner, you could do:</p>
 * <code>
 *     Car.Owner.age
 * </code>
 * <p>You can obtain any tag using the key system, including ObjectTags. So the key `Car.Owner` would be valid.</p>
 * <p>{@link me.ryandw11.ods.exception.ODSException} is thrown when an IOException is encountered.</p>
 */
public class ObjectDataStructure {
    private File file;
    private Compression compression;

    /**
     * The file that is to be saved to
     * @param file The file.
     */
    public ObjectDataStructure(File file){
        this(file, Compression.GZIP);
    }

    /**
     * The file to be saved to
     * @param file The file.
     * @param compression What compression the file should use.
     */
    public ObjectDataStructure(File file, Compression compression){
        this.file = file;
        this.compression = compression;
    }

    /**
     * Grab the output stream that is used based upon the compression format.
     * @return The output stream that should be used.
     */
    private OutputStream getOutputStream() throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        if(compression == Compression.GZIP)
            return new GZIPOutputStream(fos);
        if(compression == Compression.ZLIB)
            return new DeflaterOutputStream(fos);
        return fos;
    }

    /**
     * Grab the input stream that is used based upon the compression format.
     * @return The input stream that should be used.
     */
    private InputStream getInputStream() throws IOException {
        FileInputStream fis = new FileInputStream(file);
        if(compression == Compression.GZIP)
            return new GZIPInputStream(fis);
        if(compression == Compression.ZLIB)
            return new InflaterInputStream(fis);
        return fis;
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
            InputStream is = getInputStream();
            T out = (T) getSubData(is.readAllBytes(), name);
            is.close();
            return out;
        }catch(IOException ex){
            throw new ODSException("Error when receiving information from a file.", ex);
        }
    }

    /**
     * Grab a tag based upon an object key.
     * <p>This method allows you to directly get sub-objects with little overhead.</p>
     * <code>
     *     getObject("Object1.Object2.valuetag");
     * </code>
     * @param key The key to use.
     * @return The object Tag.
     *      <p>This will return null if the requested sub-object does not exist, or if the file does not exist.</p>
     */
    public <T> T getObject(String key){
        try{
            if(!file.exists()) return null;
            InputStream is = getInputStream();
            T out = (T) getSubObjectData(is.readAllBytes(), key);
            is.close();
            return out;
        }catch(IOException ex){
            throw new ODSException("Error when receiving information from a file.", ex);
        }
    }

    /**
     * Get all of the tags in the file.
     * @return All of the tags.
     * <p>This will return null if there are no tags, or if the file does not exist.</p>
     */
    public List<Tag<?>> getAll(){
        try{
            if(!file.exists()) return null;
            InputStream is = getInputStream();
            List<Tag<?>> output = getListData(is.readAllBytes());
            is.close();
            return output;
        }catch(IOException ex){
            throw new ODSException("Error when receiving information from a file.", ex);
        }
    }

    /**
     * Save tags to the file.
     * <p>This will overwrite the existing file. To append tags see {@link #append(Tag)} and {@link #appendAll(List)}</p>
     * @param tags The list of tags to save.
     */
    public void save(List<? extends Tag<?>> tags){
        try{
            OutputStream os = getOutputStream();
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));
            for(Tag<?> tag : tags){
                tag.writeData(dos);
            }
            dos.close();
            os.close();
            finish(os);
        }catch(IOException ex){
            throw new ODSException("Error when saving information to the file", ex);
        }
    }

    /**
     * Append tags to the end of the file.
     * @param tag The tag to be appended.
     */
    public void append(Tag<?> tag){
        try{
            byte[] data = new byte[0];
            if(!file.exists()){
                if(!file.createNewFile())
                    throw new ODSException("Unable to create file when appending tag.");
            }else{
                InputStream is = getInputStream();
                data = is.readAllBytes();
                is.close();
            }
            OutputStream os = getOutputStream();
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));

            dos.write(data);
            tag.writeData(dos);

            dos.close();
            os.close();
            finish(os);
        }catch(IOException ex){
            throw new ODSException("Error when saving information to the file", ex);
        }
    }

    /**
     * Append a list of tags to the end of the file.
     * @param tags The list of tags.
     */
    public void appendAll(List<Tag<?>> tags){
        try{
            byte[] data = new byte[0];
            if (!file.exists()){
                if(!file.createNewFile())
                    throw new ODSException("Unable to create file when appending all tags.");
            }
            else{
                InputStream is = getInputStream();
                data = is.readAllBytes();
                is.close();
            }

            OutputStream os = getOutputStream();
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));

            dos.write(data);
            for(Tag<?> tag : tags){
                tag.writeData(dos);
            }
            dos.close();
            finish(os);
        }catch(IOException ex){
            throw new ODSException("Error when saving information to the file", ex);
        }
    }

    /**
     * Find if a key exists within the file.
     * <p>This will not throw ODSException if an IOException occurs.</p>
     * @param key They key to find
     * @return If the key exists.
     */
    public boolean find(String key){
        try{
            InputStream is = getInputStream();
            return findSubObjectData(is.readAllBytes(), key);
        }catch(IOException ex){
            return false;
        }
    }

    /**
     * Remove a tag from the list.
     * @param key The key to remove.
     * @return If the deletion was successfully done.
     */
    public boolean delete(String key){
        try{
            InputStream is = getInputStream();
            byte[] data = is.readAllBytes();
            is.close();
            KeyScout counter = scoutObjectData(data, key, 0, new KeyScout());
            if(counter == null){
                return false;
            }
            byte[] deleteReturn = deleteSubObjectData(data, counter);
            OutputStream out = getOutputStream();
            out.write(deleteReturn);
            out.close();
            return true;
        }catch(IOException ex){
            return false;
        }
    }

    /**
     * Replace a key with another tag.
     * @param key The key
     * @param replacement The data to replace
     * @return If the replacement was successful. ODSException is not thrown.
     */
    public boolean replaceData(String key, Tag<?> replacement){
        try{
            InputStream is = getInputStream();
            byte[] data = is.readAllBytes();
            is.close();

            KeyScout counter = scoutObjectData(data, key, 0, new KeyScout());
            if(counter == null){
                return false;
            }

            ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(byteArrayOut);
            replacement.writeData(dos);

            byte[] replaceReturn = replaceSubObjectData(data, counter,byteArrayOut.toByteArray());
            OutputStream out = getOutputStream();
            out.write(replaceReturn);
            out.close();
            dos.close();
            byteArrayOut.close();
            return true;
        }catch(IOException ex){
            return false;
        }
    }

    /**
     * Get a tag based upon the name.
     * This is used byte the {@link #get(String)} method.
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
        // Loop until the file is done being read.
        while(dis.available() > 0){
            currentBuilder.setDataType(dis.readByte());
            currentBuilder.setDataSize(dis.readInt());
            currentBuilder.setStartingIndex(cis.getByteCount());
            currentBuilder.setNameSize(((Short) dis.readShort()).intValue());
            // If the name size isn't the same, then don't waste time reading the name.
            if(currentBuilder.getNameSize() != name.getBytes(StandardCharsets.UTF_8).length){
                dis.skip((currentBuilder.getStartingIndex() - cis.getByteCount()) + currentBuilder.getDataSize());
                currentBuilder = new TagBuilder();
                continue;
            }
            byte[] nameBytes = new byte[currentBuilder.getNameSize()];
            dis.readFully(nameBytes);
            String tagName = new String(nameBytes, StandardCharsets.UTF_8);
            currentBuilder.setName(tagName);
            // If the name is not correct, skip forward!
            if(!name.equals(tagName)){
                dis.skip((currentBuilder.getStartingIndex() - cis.getByteCount()) + currentBuilder.getDataSize());
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
     * This method is used by the {@link #getObject(String)}, this is a recursive method.
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
                dis.skip((currentBuilder.getStartingIndex() - cis.getByteCount()) + currentBuilder.getDataSize());
                currentBuilder = new TagBuilder();
                continue;
            }

            byte[] nameBytes = new byte[currentBuilder.getNameSize()];
            dis.readFully(nameBytes);
            String tagName = new String(nameBytes, StandardCharsets.UTF_8);
            currentBuilder.setName(tagName);
            // If the name is not correct, skip forward!
            if(!name.equals(tagName)){
                dis.skip((currentBuilder.getStartingIndex() - cis.getByteCount()) + currentBuilder.getDataSize());
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

    /**
     * Check to see if a key exists within a file.
     * This method is used by the {@link #find(String)} method.
     * @param data The array of bytes.
     * @param key The key to find
     * @return If the key could be found.
     * @throws IOException If the end of the file is reached
     */
    private boolean findSubObjectData(byte[] data, String key) throws IOException {
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
                dis.skip((currentBuilder.getStartingIndex() - cis.getByteCount()) + currentBuilder.getDataSize());
                currentBuilder = new TagBuilder();
                continue;
            }
            byte[] nameBytes = new byte[currentBuilder.getNameSize()];
            dis.readFully(nameBytes);
            String tagName = new String(nameBytes, StandardCharsets.UTF_8);
            currentBuilder.setName(tagName);
            // If the name is not correct, skip forward!
            if(!name.equals(tagName)){
                dis.skip((currentBuilder.getStartingIndex() - cis.getByteCount()) + currentBuilder.getDataSize());
                currentBuilder = new TagBuilder();
                continue;
            }

            byte[] value = new byte[((int) currentBuilder.getStartingIndex() - cis.getCount()) + currentBuilder.getDataSize()];
            dis.readFully(value);
            currentBuilder.setValueBytes(value);
            dis.close();
            if(otherKey != null)
                return findSubObjectData(currentBuilder.getValueBytes(), otherKey);
            return true;
        }
        dis.close();
        return false;
    }

    /**
     * Get the next key from the current key.
     * So for example: An input of ['Object1', 'Object2', 'value']
     * would output "Object2.value"
     * @param s The current key in array form.
     * @return The next key in string form.
     */
    private String getKey(String[] s){
        List<String> list = new ArrayList<>(Arrays.asList(s));
        list.remove(0);
        if(list.size() == 1) return list.get(0);
        if(list.size() < 1) return null;

        return String.join(".", list);
    }

    /**
     * Delete a key from the file.
     * @param data The input from the data.
     * @param counter The scout information.
     * @return The output array of bytes.
     */
    private byte[] deleteSubObjectData(byte[] data, KeyScout counter){
        counter.removeAmount(counter.getEnd().getSize() + 5);

        KeyScoutChild end = counter.getEnd();

        byte[] array1 = new byte[data.length - (5 + end.getSize())];
        //Copy all of the information before the removed data.
        System.arraycopy(data, 0, array1, 0, (end.getStartingIndex() - 1));
        // copy all of the information after the removed data.
        System.arraycopy(data, end.getStartingIndex() + 4 + end.getSize(),
                array1, end.getStartingIndex()-1,
                data.length - (end.getStartingIndex() + 4 + end.getSize()));

        for(KeyScoutChild child : counter.getChildren()){
            int index = child.getStartingIndex();
            int size = child.getSize();
            array1[index] = (byte)(size >> 24);
            array1[index + 1] = (byte)(size >> 16);
            array1[index + 2] = (byte)(size >> 8);
            array1[index + 3] = (byte)(size);
        }

        return array1;
    }

    /**
     * Replace a tag with another tag.
     * @param data The input array of bytes
     * @param counter The scout object
     * @param dataToReplace The bytes of the replacement data.
     * @return The output bytes.
     */
    private byte[] replaceSubObjectData(byte[] data, KeyScout counter, byte[] dataToReplace){
        counter.removeAmount(counter.getEnd().getSize() + 5);
        counter.addAmount(dataToReplace.length);

        KeyScoutChild end = counter.getEnd();

        byte[] array1 = new byte[data.length - (5 + end.getSize()) + dataToReplace.length];
        //Copy all of the information before the removed data.
        System.arraycopy(data, 0, array1, 0, (end.getStartingIndex() - 1));
        System.arraycopy(dataToReplace, 0, array1, end.getStartingIndex()-1, dataToReplace.length);
        // copy all of the information after the removed data.
        System.arraycopy(data, end.getStartingIndex() + 4 + end.getSize(),
                array1, end.getStartingIndex()-1 + dataToReplace.length,
                data.length - (end.getStartingIndex() + 4 + end.getSize()));

        for(KeyScoutChild child : counter.getChildren()){
            int index = child.getStartingIndex();
            int size = child.getSize();
            array1[index] = (byte)(size >> 24);
            array1[index + 1] = (byte)(size >> 16);
            array1[index + 2] = (byte)(size >> 8);
            array1[index + 3] = (byte)(size);
        }

        return array1;
    }

    /**
     * This object goes through the data and scouts out the information from the given key.
     * This method is recursive, which is why the parameter offset exists.
     * @param data The input array of bytes
     * @param key The key
     * @param offset The byte counter offset. (How many bytes were read previously).
     * @param counter The Scout object.
     * @return The key scout.
     * @throws IOException If the file of an invalid type.
     */
    private KeyScout scoutObjectData(byte[] data, String key, int offset, KeyScout counter) throws IOException {
        InputStream stream = new ByteArrayInputStream(data);
        BufferedInputStream bis = new BufferedInputStream(stream);
        final CountingInputStream cis = new CountingInputStream(bis);
        DataInputStream dis = new DataInputStream(cis);
        String name = key.split("\\.")[0];
        String otherKey = getKey(key.split("\\."));

        TagBuilder currentBuilder = new TagBuilder();
        while(dis.available() > 0){
            KeyScoutChild child = new KeyScoutChild();
            currentBuilder.setDataType(dis.readByte());
            // Set the starting index of the byte count.
            child.setStartingIndex(offset + (int)cis.getByteCount());
            currentBuilder.setDataSize(dis.readInt());
            currentBuilder.setStartingIndex(cis.getByteCount());
            currentBuilder.setNameSize(((Short) dis.readShort()).intValue());
            // If the name size isn't the same, then don't waste time reading the name.
            if(currentBuilder.getNameSize() != name.getBytes(StandardCharsets.UTF_8).length){
                dis.skip((currentBuilder.getStartingIndex() - cis.getByteCount()) + currentBuilder.getDataSize());
                currentBuilder = new TagBuilder();
                continue;
            }
            byte[] nameBytes = new byte[currentBuilder.getNameSize()];
            dis.readFully(nameBytes);
            String tagName = new String(nameBytes, StandardCharsets.UTF_8);
            currentBuilder.setName(tagName);
            // If the name is not correct, skip forward!
            if(!name.equals(tagName)){
                dis.skip((currentBuilder.getStartingIndex() - cis.getByteCount()) + currentBuilder.getDataSize());
                currentBuilder = new TagBuilder();
                continue;
            }

            // This is done so that way the counter does not count the value bytes.
            int count = (int) cis.getByteCount();

            byte[] value = new byte[((int) currentBuilder.getStartingIndex() - cis.getCount()) + currentBuilder.getDataSize()];
            dis.readFully(value);
            currentBuilder.setValueBytes(value);
            dis.close();
            if(otherKey != null) {
                child.setSize(currentBuilder.getDataSize());
                child.setName(currentBuilder.getName());
                counter.addChild(child);
                return scoutObjectData(currentBuilder.getValueBytes(), otherKey, offset + count, counter);
            }
            child.setName(currentBuilder.getName());
            child.setSize(currentBuilder.getDataSize());
            counter.setEnd(child);
            return counter;
        }
        dis.close();
        return null;
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

    private void finish(OutputStream stream) {
        try{
            if(stream instanceof GZIPOutputStream){
                ((GZIPOutputStream) stream).finish();
            }
            if(stream instanceof InflaterOutputStream){
                ((InflaterOutputStream) stream).finish();
            }
        }catch(IOException ex){
            throw new ODSException("An error has occured while attempting to close the output stream of the file.", ex);
        }
    }
}
