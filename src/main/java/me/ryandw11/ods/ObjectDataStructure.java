package me.ryandw11.ods;

import me.ryandw11.ods.exception.ODSException;
import me.ryandw11.ods.tags.ObjectTag;
import me.ryandw11.ods.util.KeyScout;
import me.ryandw11.ods.util.KeyScoutChild;

import java.io.*;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.*;

/**
 * The primary class of the ObjectDataStructure library.
 * <p>Most methods in ODS use what is called a key to reference objects within the file. A key allows you to grab specific
 * information within the file. For example: If you wanted a specific string inside of an object with lots of information,
 * you can get that specific string without any other data. If you have an object named Car and you wanted to get a string tag named
 * owner from the inside the object, then the key for that would be:</p>
 * <code>
 * Car.owner
 * </code>
 * <p>Let's say that the owner is an object called 'Owner' and you want to get the age of the owner, you could do:</p>
 * <code>
 * Car.Owner.age
 * </code>
 * <p>You can obtain any tag using the key system, including ObjectTags. So the key `Car.Owner` would be valid.</p>
 * <p>{@link ODSException} is thrown when an IOException is encountered or the file is not in the ODS format / is corrupted.</p>
 *
 * @version 1.0.1
 */
public class ObjectDataStructure {
    private File file;
    private Compression compression;

    /**
     * The file that is to be saved to
     *
     * @param file The file.
     */
    public ObjectDataStructure(File file) {
        this(file, Compression.GZIP);
    }

    /**
     * The file to be saved to
     *
     * @param file        The file.
     * @param compression What compression the file should use.
     */
    public ObjectDataStructure(File file, Compression compression) {
        this.file = file;
        this.compression = compression;
    }

    /**
     * Grab the output stream that is used based upon the compression format.
     *
     * @return The output stream that should be used.
     */
    private OutputStream getOutputStream() throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        if (compression == Compression.GZIP)
            return new GZIPOutputStream(fos);
        if (compression == Compression.ZLIB)
            return new DeflaterOutputStream(fos);
        return fos;
    }

    /**
     * Grab the input stream that is used based upon the compression format.
     *
     * @return The input stream that should be used.
     */
    private InputStream getInputStream() throws IOException {
        FileInputStream fis = new FileInputStream(file);
        if (compression == Compression.GZIP)
            return new GZIPInputStream(fis);
        if (compression == Compression.ZLIB)
            return new InflaterInputStream(fis);
        return fis;
    }

    /**
     * Returns the best byte buffer for memory usage depending on the compression type.
     *
     * @param stream The input stream.
     * @return The corresponding byte buffer.
     * @throws IOException If an error is encountered reading all of the bytes. (This will never occur
     *                     if a file is never compressed).
     */
    private ByteBuffer getInputBuffer(InputStream stream) throws IOException {
        /*
                If the file is not compressed than the file can be directly
                streamed using channels saving memory.
        */
        if (stream instanceof FileInputStream) {
            FileInputStream fis = (FileInputStream) stream;
            FileChannel channel = fis.getChannel();
            return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        } else
            return ByteBuffer.wrap(stream.readAllBytes());
    }

    /**
     * Grab a tag based upon an object key.
     * <p>This method allows you to directly get sub-objects with little overhead.</p>
     * <code>
     * get("Object1.Object2.valuetag");
     * </code>
     *
     * @param key The key to use.
     * @param <T> The tag type.
     * @return The object Tag.
     * <p>This will return null if the requested sub-object does not exist, or if the file does not exist.</p>
     */
    public <T> T get(String key) {
        try {
            if (!file.exists()) return null;
            InputStream is = getInputStream();
            ByteBuffer buffer = getInputBuffer(is);
            T out = (T) getSubObjectData(buffer, key);
            buffer.clear();
            return out;
        } catch (IOException ex) {
            throw new ODSException("Error when receiving information from a file.", ex);
        } catch (BufferOverflowException | BufferUnderflowException ex) {
            throw new ODSException("Invalid file format or the file has been tampered with / corrupted.");
        }
    }

    /**
     * Get all of the tags in the file.
     *
     * @return All of the tags.
     * <p>This will return null if there are no tags, or if the file does not exist.</p>
     */
    public List<Tag<?>> getAll() {
        try {
            if (!file.exists()) return null;
            InputStream is = getInputStream();
            ByteBuffer buffer = getInputBuffer(is);
            List<Tag<?>> output = getListData(buffer, buffer.limit());
            is.close();
            return output;
        } catch (IOException ex) {
            throw new ODSException("Error when receiving information from a file.", ex);
        } catch (BufferOverflowException | BufferUnderflowException ex) {
            throw new ODSException("Invalid file format or the file has been tampered with / corrupted.");
        }
    }

    /**
     * Save tags to the file.
     * <p>This will overwrite the existing file. To append tags see {@link #append(Tag)} and {@link #appendAll(List)}</p>
     *
     * @param tags The list of tags to save.
     */
    public void save(List<? extends Tag<?>> tags) {
        try {
            OutputStream os = getOutputStream();
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));
            for (Tag<?> tag : tags) {
                tag.writeData(dos);
            }
            dos.close();
            os.close();
            finish(os);
        } catch (IOException ex) {
            throw new ODSException("Error when saving information to the file", ex);
        }
    }

    /**
     * Append tags to the end of the file.
     *
     * @param tag The tag to be appended.
     */
    public void append(Tag<?> tag) {
        try {
            byte[] data = new byte[0];
            if (!file.exists()) {
                if (!file.createNewFile())
                    throw new ODSException("Unable to create file when appending tag.");
            } else {
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
        } catch (IOException ex) {
            throw new ODSException("Error when saving information to the file", ex);
        }
    }

    /**
     * Append a list of tags to the end of the file.
     *
     * @param tags The list of tags.
     */
    public void appendAll(List<Tag<?>> tags) {
        try {
            byte[] data = new byte[0];
            if (!file.exists()) {
                if (!file.createNewFile())
                    throw new ODSException("Unable to create file when appending all tags.");
            } else {
                InputStream is = getInputStream();
                data = is.readAllBytes();
                is.close();
            }

            OutputStream os = getOutputStream();
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));

            dos.write(data);
            for (Tag<?> tag : tags) {
                tag.writeData(dos);
            }
            dos.close();
            finish(os);
        } catch (IOException ex) {
            throw new ODSException("Error when saving information to the file", ex);
        }
    }

    /**
     * Find if a key exists within the file.
     * <p>This will not throw ODSException if an IOException occurs.</p>
     *
     * @param key They key to find
     * @return If the key exists.
     */
    public boolean find(String key) {
        try {
            InputStream is = getInputStream();
            ByteBuffer buffer = getInputBuffer(is);
            return findSubObjectData(buffer, key);
        } catch (IOException ex) {
            return false;
        } catch (BufferOverflowException | BufferUnderflowException ex) {
            throw new ODSException("Invalid file format or the file has been tampered with / corrupted.");
        }
    }

    /**
     * Remove a tag from the list.
     *
     * @param key The key to remove.
     * @return If the deletion was successfully done.
     */
    public boolean delete(String key) {
        try {
            InputStream is = getInputStream();
            ByteBuffer buffer = getInputBuffer(is);
            is.close();
            KeyScout counter = scoutObjectData(buffer, key, new KeyScout());
            if (counter == null) {
                return false;
            }
            byte[] deleteReturn = deleteSubObjectData(buffer.array(), counter);
            OutputStream out = getOutputStream();
            out.write(deleteReturn);
            out.close();
            return true;
        } catch (IOException ex) {
            return false;
        } catch (BufferOverflowException | BufferUnderflowException ex) {
            throw new ODSException("Invalid file format or the file has been tampered with / corrupted.");
        }
    }

    /**
     * Replace a key with another tag.
     *
     * @param key         The key
     * @param replacement The data to replace
     * @return If the replacement was successful. ODSException is not thrown.
     */
    public boolean replaceData(String key, Tag<?> replacement) {
        try {
            InputStream is = getInputStream();
            ByteBuffer buffer = getInputBuffer(is);
            is.close();

            KeyScout counter = scoutObjectData(buffer, key, new KeyScout());
            if (counter.getEnd() == null) {
                return false;
            }

            ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(byteArrayOut);
            replacement.writeData(dos);

            byte[] replaceReturn = replaceSubObjectData(buffer.array(), counter, byteArrayOut.toByteArray());
            OutputStream out = getOutputStream();
            out.write(replaceReturn);
            out.close();
            dos.close();
            byteArrayOut.close();
            return true;
        } catch (IOException ex) {
            return false;
        } catch (BufferOverflowException | BufferUnderflowException ex) {
            throw new ODSException("Invalid file format or the file has been tampered with / corrupted.");
        }
    }

    /**
     * This method can append, delete, and set tags.
     * <p>A note on keys when appending: <br><code>ObjectOne.ObjectTwo.tagName</code><br> When appending data <b>tagName</b> will not be the actual tag name.
     * The tag name written to the file is the name of the specified tag in the value parameter. Any parent objects that do not exist will be created. For example:
     * <br><code>ObjectOne.ObjectTwo.NewObject.tagName</code><br> If in the example above <b>NewObject</b> does not exist, than the object will be created with the value tag inside
     * of it. Please see the wiki for a more detailed explanation on this.</p>
     * <p>When value is null, the specified key is deleted. <b>The key MUST exist or an {@link ODSException} will be thrown.</b></p>
     *
     * @param key   The key of the tag to append, delete, or set.
     *              <p>When appending the key does not need to exist. ObjectTags that don't exist will be created automatically.</p>
     *              <p>When the key is set to "" (An empty string) than it is assumed you want to append to the parent file.</p>
     *              <p>Valid Tags:</p>
     *              <code>
     *              ObjectOne.tagToDelete<br>
     *              ObjectOne.NewObject.tagToAppend<br>
     *              ObjectOne.tagToSet<br>
     *              </code>
     * @param value The tag to append or replace the key with. <p>If this parameter is null than the key is deleted.</p>
     */
    public void set(String key, Tag<?> value) {
        if (value == null) {
            boolean output = delete(key);
            if (!output)
                throw new ODSException("The key " + key + " does not exist!");
            return;
        }
        if (key.equals("")) {
            save(Collections.singletonList(value));
            return;
        }
        try {
            InputStream is = getInputStream();
            ByteBuffer buffer = getInputBuffer(is);
            is.close();

            KeyScout counter = scoutObjectData(buffer, key, new KeyScout());
            if (counter.getEnd() == null) {
                if (counter.getChildren().size() < 1) {
                    append(value);
                    return;
                }
                StringBuilder existingKey = new StringBuilder();
                for (KeyScoutChild child : counter.getChildren()) {
                    if (existingKey.length() != 0)
                        existingKey.append(".");
                    existingKey.append(child.getName());
                }
                String newKey = key.replace(existingKey + ".", "");
                Tag<?> currentData;
                if (newKey.split("\\.").length > 1) {
                    ObjectTag output = null;
                    ObjectTag curTag = null;
                    List<String> keys = new ArrayList<>(Arrays.asList(newKey.split("\\.")));
                    int i = 0;
                    for (String s : keys) {
                        if (i == 0) {
                            output = new ObjectTag(s);
                            curTag = output;
                        } else if (i == keys.size() - 1) {
                            curTag.addTag(value);
                        } else {
                            ObjectTag tag = new ObjectTag(s);
                            curTag.addTag(tag);
                            curTag = tag;
                        }
                        i++;
                    }
                    currentData = output;
                    // Handle creating new objects
                } else {
                    currentData = value;
                }
                // Get the byte data of the new objects.
                ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(byteArrayOut);
                assert currentData != null;
                currentData.writeData(dos);
                byte[] data = byteArrayOut.toByteArray();
                dos.close();
                byteArrayOut.close();
                // Insert the data and read all of the existing bytes
                InputStream stream = getInputStream();
                byte[] output = setSubObjectData(stream.readAllBytes(), counter, data);
                stream.close();
                // Write the new bytes to the file.
                OutputStream out = getOutputStream();
                out.write(output);
                out.close();
            } else {
                ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(byteArrayOut);
                value.writeData(dos);

                byte[] replaceReturn = replaceSubObjectData(buffer.array(), counter, byteArrayOut.toByteArray());
                OutputStream out = getOutputStream();
                out.write(replaceReturn);
                out.close();
                dos.close();
                byteArrayOut.close();
            }
        } catch (IOException ex) {
            throw new ODSException("An error had occurred when trying to set data. Does that key exist?", ex);
        } catch (BufferOverflowException | BufferUnderflowException ex) {
            throw new ODSException("Invalid file format or the file has been tampered with / corrupted.");
        }
    }

    /**
     * Get a tag based upon the name.
     * This method is used by the {@link #get(String)}, this is a recursive method.
     *
     * @param data The list of data.
     * @param key  The key
     * @return The tag
     */
    private Tag<?> getSubObjectData(ByteBuffer data, String key) {
        String name = key.split("\\.")[0];
        String otherKey = getKey(key.split("\\."));

        TagBuilder currentBuilder = new TagBuilder();
        while (data.hasRemaining()) {
            currentBuilder.setDataType(data.get());
            currentBuilder.setDataSize(data.getInt());
            currentBuilder.setStartingIndex(data.position());
            currentBuilder.setNameSize(((Short) data.getShort()).intValue());
            // If the name size isn't the same, then don't waste time reading the name.
            if (currentBuilder.getNameSize() != name.getBytes(StandardCharsets.UTF_8).length) {
                data.position(((Long) currentBuilder.getStartingIndex()).intValue() + currentBuilder.getDataSize());
                currentBuilder = new TagBuilder();
                continue;
            }

            byte[] nameBytes = new byte[currentBuilder.getNameSize()];
            data.get(nameBytes);
            String tagName = new String(nameBytes, StandardCharsets.UTF_8);
            currentBuilder.setName(tagName);
            // If the name is not correct, skip forward!
            if (!name.equals(tagName)) {
                data.position(((Long) currentBuilder.getStartingIndex()).intValue() + currentBuilder.getDataSize());
                currentBuilder = new TagBuilder();
                continue;
            }

            currentBuilder.setValueLength(((int) currentBuilder.getStartingIndex() - data.position()) + currentBuilder.getDataSize());
            currentBuilder.setValueBytes(data);
            if (otherKey != null)
                return getSubObjectData(data, otherKey);
            return currentBuilder.process();
        }
        return null;
    }

    /**
     * Check to see if a key exists within a file.
     * This method is used by the {@link #find(String)} method.
     *
     * @param data The array of bytes.
     * @param key  The key to find
     * @return If the key could be found.
     */
    private boolean findSubObjectData(ByteBuffer data, String key) {
        String name = key.split("\\.")[0];
        String otherKey = getKey(key.split("\\."));

        TagBuilder currentBuilder = new TagBuilder();
        while (data.hasRemaining()) {
            currentBuilder.setDataType(data.get());
            currentBuilder.setDataSize(data.getInt());
            currentBuilder.setStartingIndex(data.position());
            currentBuilder.setNameSize(((Short) data.getShort()).intValue());
            // If the name size isn't the same, then don't waste time reading the name.
            if (currentBuilder.getNameSize() != name.getBytes(StandardCharsets.UTF_8).length) {
                data.position(((Long) currentBuilder.getStartingIndex()).intValue() + currentBuilder.getDataSize());
                currentBuilder = new TagBuilder();
                continue;
            }

            byte[] nameBytes = new byte[currentBuilder.getNameSize()];
            data.get(nameBytes);
            String tagName = new String(nameBytes, StandardCharsets.UTF_8);
            currentBuilder.setName(tagName);
            // If the name is not correct, skip forward!
            if (!name.equals(tagName)) {
                data.position(((Long) currentBuilder.getStartingIndex()).intValue() + currentBuilder.getDataSize());
                currentBuilder = new TagBuilder();
                continue;
            }

            currentBuilder.setValueLength(((int) currentBuilder.getStartingIndex() - data.position()) + currentBuilder.getDataSize());
            currentBuilder.setValueBytes(data);
            if (otherKey != null)
                return findSubObjectData(data, otherKey);
            return true;
        }
        return false;
    }

    /**
     * Get the next key from the current key.
     * So for example: An input of ['Object1', 'Object2', 'value']
     * would output "Object2.value"
     *
     * @param s The current key in array form.
     * @return The next key in string form.
     */
    private String getKey(String[] s) {
        List<String> list = new ArrayList<>(Arrays.asList(s));
        list.remove(0);
        if (list.size() == 1) return list.get(0);
        if (list.size() < 1) return null;

        return String.join(".", list);
    }

    /**
     * Delete a key from the file.
     *
     * @param data    The input from the data.
     * @param counter The scout information.
     * @return The output array of bytes.
     */
    private byte[] deleteSubObjectData(byte[] data, KeyScout counter) {
        counter.removeAmount(counter.getEnd().getSize() + 5);

        KeyScoutChild end = counter.getEnd();

        byte[] array1 = new byte[data.length - (5 + end.getSize())];
        //Copy all of the information before the removed data.
        System.arraycopy(data, 0, array1, 0, (end.getStartingIndex() - 1));
        // copy all of the information after the removed data.
        System.arraycopy(data, end.getStartingIndex() + 4 + end.getSize(),
                array1, end.getStartingIndex() - 1,
                data.length - (end.getStartingIndex() + 4 + end.getSize()));

        for (KeyScoutChild child : counter.getChildren()) {
            int index = child.getStartingIndex();
            int size = child.getSize();
            array1[index] = (byte) (size >> 24);
            array1[index + 1] = (byte) (size >> 16);
            array1[index + 2] = (byte) (size >> 8);
            array1[index + 3] = (byte) (size);
        }

        return array1;
    }

    /**
     * Replace a tag with another tag.
     *
     * @param data          The input array of bytes
     * @param counter       The scout object
     * @param dataToReplace The bytes of the replacement data.
     * @return The output bytes.
     */
    private byte[] replaceSubObjectData(byte[] data, KeyScout counter, byte[] dataToReplace) {
        counter.removeAmount(counter.getEnd().getSize() + 5);
        counter.addAmount(dataToReplace.length);

        KeyScoutChild end = counter.getEnd();

        byte[] array1 = new byte[data.length - (5 + end.getSize()) + dataToReplace.length];
        //Copy all of the information before the removed data.
        System.arraycopy(data, 0, array1, 0, (end.getStartingIndex() - 1));
        System.arraycopy(dataToReplace, 0, array1, end.getStartingIndex() - 1, dataToReplace.length);
        // copy all of the information after the removed data.
        System.arraycopy(data, end.getStartingIndex() + 4 + end.getSize(),
                array1, end.getStartingIndex() - 1 + dataToReplace.length,
                data.length - (end.getStartingIndex() + 4 + end.getSize()));

        for (KeyScoutChild child : counter.getChildren()) {
            int index = child.getStartingIndex();
            int size = child.getSize();
            array1[index] = (byte) (size >> 24);
            array1[index + 1] = (byte) (size >> 16);
            array1[index + 2] = (byte) (size >> 8);
            array1[index + 3] = (byte) (size);
        }

        return array1;
    }

    /**
     * Insert in a new tag.
     *
     * @param data          The input array of bytes
     * @param counter       The scout object
     * @param dataToReplace The bytes of the replacement data.
     * @return The output bytes.
     */
    private byte[] setSubObjectData(byte[] data, KeyScout counter, byte[] dataToReplace) {
        // Out of bounds is checked by parent method.
        KeyScoutChild child = counter.getChildren().get(counter.getChildren().size() - 1);
//        counter.removeAmount(child.getSize() + 5);


        byte[] array1 = new byte[data.length + dataToReplace.length];
        System.arraycopy(data, 0, array1, 0, child.getStartingIndex() + 4 + child.getSize());
        System.arraycopy(dataToReplace, 0, array1, child.getStartingIndex() + 4 + child.getSize(), dataToReplace.length);
        System.arraycopy(data, (child.getStartingIndex() + 4 + child.getSize()), array1, (child.getStartingIndex() + 4 + child.getSize()) + dataToReplace.length,
                data.length - (child.getStartingIndex() + 4 + child.getSize()));

        counter.addAmount(dataToReplace.length);

        for (KeyScoutChild childs : counter.getChildren()) {
            int index = childs.getStartingIndex();
            int size = childs.getSize();
            array1[index] = (byte) (size >> 24);
            array1[index + 1] = (byte) (size >> 16);
            array1[index + 2] = (byte) (size >> 8);
            array1[index + 3] = (byte) (size);
        }

        return array1;
    }

    /**
     * This object goes through the data and scouts out the information from the given key.
     * This method is recursive, which is why the parameter offset exists.
     *
     * @param data    The input array of bytes
     * @param key     The key
     * @param counter The Scout object.
     * @return The key scout.
     */
    private KeyScout scoutObjectData(ByteBuffer data, String key, KeyScout counter) {
        String name = key.split("\\.")[0];
        String otherKey = getKey(key.split("\\."));

        TagBuilder currentBuilder = new TagBuilder();
        while (data.hasRemaining()) {
            KeyScoutChild child = new KeyScoutChild();
            currentBuilder.setDataType(data.get());
            // Set the starting index of the byte count.
            child.setStartingIndex(data.position());
            currentBuilder.setDataSize(data.getInt());
            currentBuilder.setStartingIndex(data.position());
            currentBuilder.setNameSize(((Short) data.getShort()).intValue());
            // If the name size isn't the same, then don't waste time reading the name.
            if (currentBuilder.getNameSize() != name.getBytes(StandardCharsets.UTF_8).length) {
                data.position(((Long) currentBuilder.getStartingIndex()).intValue() + currentBuilder.getDataSize());
                currentBuilder = new TagBuilder();
                continue;
            }
            byte[] nameBytes = new byte[currentBuilder.getNameSize()];
            data.get(nameBytes);
            String tagName = new String(nameBytes, StandardCharsets.UTF_8);
            currentBuilder.setName(tagName);
            // If the name is not correct, skip forward!
            if (!name.equals(tagName)) {
                data.position(((Long) currentBuilder.getStartingIndex()).intValue() + currentBuilder.getDataSize());
                currentBuilder = new TagBuilder();
                continue;
            }
            currentBuilder.setValueBytes(data);
            if (otherKey != null) {
                child.setSize(currentBuilder.getDataSize());
                child.setName(currentBuilder.getName());
                counter.addChild(child);
                return scoutObjectData(currentBuilder.getValueBytes(), otherKey, counter);
            }
            child.setName(currentBuilder.getName());
            child.setSize(currentBuilder.getDataSize());
            counter.setEnd(child);
            return counter;
        }
        return counter;
    }

    /**
     * Get the list of tags based upon bytes.
     * <p>This is meant for internal use only.</p>
     *
     * @param data  the array of bytes
     * @param limit the number of bytes that should be read.
     * @return The List of tags.
     */
    public static List<Tag<?>> getListData(ByteBuffer data, int limit) {
        List<Tag<?>> output = new ArrayList<>();

        TagBuilder currentBuilder = new TagBuilder();
        int initialPos = data.position();
        while (data.position() < initialPos + limit) {
            currentBuilder.setDataType(data.get());
            currentBuilder.setDataSize(data.getInt());
            currentBuilder.setStartingIndex(data.position());
            currentBuilder.setNameSize(data.getShort());

            byte[] nameBytes = new byte[currentBuilder.getNameSize()];
            data.get(nameBytes);
            String tagName = new String(nameBytes, StandardCharsets.UTF_8);
            currentBuilder.setName(tagName);
            currentBuilder.setValueLength(((int) currentBuilder.getStartingIndex() - data.position()) + currentBuilder.getDataSize());
            currentBuilder.setValueBytes(data);
            output.add(currentBuilder.process());
        }
        return output;
    }

    private void finish(OutputStream stream) {
        try {
            if (stream instanceof GZIPOutputStream) {
                ((GZIPOutputStream) stream).finish();
            }
            if (stream instanceof InflaterOutputStream) {
                ((InflaterOutputStream) stream).finish();
            }
        } catch (IOException ex) {
            throw new ODSException("An error has occurred while attempting to close the output stream of the file.", ex);
        }
    }
}
