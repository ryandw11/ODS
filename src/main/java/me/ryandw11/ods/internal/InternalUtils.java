package me.ryandw11.ods.internal;

import me.ryandw11.ods.Tag;
import me.ryandw11.ods.TagBuilder;
import me.ryandw11.ods.exception.CompressedObjectException;
import me.ryandw11.ods.exception.ODSException;
import me.ryandw11.ods.util.KeyScout;
import me.ryandw11.ods.util.KeyScoutChild;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class contains the internal methods that all storage types use.
 */
public class InternalUtils {
    private InternalUtils() {
    }

    /**
     * Get a tag based upon the name.
     * This method is used by the {@link me.ryandw11.ods.ObjectDataStructure#get(String)}, this is a recursive method.
     *
     * @param data The list of data.
     * @param key  The key
     * @return The tag
     */
    protected static Tag<?> getSubObjectData(ByteBuffer data, String key) {
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
            if (otherKey != null) {
                validateNotCompressed(currentBuilder);
                return getSubObjectData(data, otherKey);
            }
            return currentBuilder.process();
        }
        return null;
    }

    /**
     * Check to see if a key exists within a file.
     * This method is used by the {@link me.ryandw11.ods.ObjectDataStructure#find(String)} method.
     *
     * @param data The array of bytes.
     * @param key  The key to find
     * @return If the key could be found.
     */
    protected static boolean findSubObjectData(ByteBuffer data, String key) {
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
            if (otherKey != null) {
                validateNotCompressed(currentBuilder);
                return findSubObjectData(data, otherKey);
            }
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
    private static String getKey(String[] s) {
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
    protected static byte[] deleteSubObjectData(byte[] data, KeyScout counter) {
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
    protected static byte[] replaceSubObjectData(byte[] data, KeyScout counter, byte[] dataToReplace) {
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
    protected static byte[] setSubObjectData(byte[] data, KeyScout counter, byte[] dataToReplace) {
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
    protected static KeyScout scoutObjectData(ByteBuffer data, String key, KeyScout counter) {
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
                // Check if the object is compressed
                validateNotCompressed(currentBuilder);

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

    /**
     * Validate if an object is compressed by throwing an exception if it is.
     *
     * @param builder The builder to check.
     */
    private static void validateNotCompressed(TagBuilder builder){
        if(builder.getDataType() == 12)
            throw new CompressedObjectException("Unable to traverse a Compressed Object. Consider decompressing the object '" + builder.getName() + "' first.");
    }
}
