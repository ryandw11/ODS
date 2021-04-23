package me.ryandw11.ods;

import me.ryandw11.ods.compression.Compressor;
import me.ryandw11.ods.compression.GZIPCompression;
import me.ryandw11.ods.exception.ODSException;
import me.ryandw11.ods.internal.ODSFile;
import me.ryandw11.ods.internal.ODSInternal;
import me.ryandw11.ods.internal.ODSMem;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * The primary class of the ObjectDataStructure library.
 * <p>
 * ObjectDataStructure has two storage types: File and Memory. File reads from a file while memory deals with
 * an array (or buffer) of bytes. This class deals with both types. The type is dependent on the constructor.
 * <p>
 * <p>Most methods in ODS use what is called a key to reference objects within the file/buffer. A key allows you to grab specific
 * information within the file/buffer. For example: If you wanted a specific string inside of an object with lots of information,
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
 * <p>{@link ODSException} is thrown when an IOException is encountered or the file/buffer is not in the ODS format / is corrupted.</p>
 * <p>
 * For exact information on the methods depending on storage type (viz. File or Memory) please visit the respective internal classes.
 *
 * @version NewLayout-SNAPSHOT
 */
public class ObjectDataStructure {
    private ODSInternal internal;

    /**
     * Create ODS using the file storage type. Data will be written to and read from a file.
     *
     * <p>This uses the GZIP compression format by default.</p>
     *
     * @param file The file.
     */
    public ObjectDataStructure(File file) {
        this(file, new GZIPCompression());
    }

    /**
     * Create ODS using the file storage type. Data will be written to and read from a file.
     *
     * @param file        The file.
     * @param compression What compression the file should use.
     */
    public ObjectDataStructure(File file, Compressor compression) {
        this.internal = new ODSFile(file, compression);
    }

    /**
     * Create ODS using the memory storage type. Data will be written to and read from a buffer.
     *
     * @param data       Pre-existing data that should be inserted into the buffer.
     * @param compressor The compression format the data uses.
     */
    public ObjectDataStructure(byte[] data, Compressor compressor) {
        this.internal = new ODSMem(data, compressor);
    }

    /**
     * Create ODS using the memory storage type. Data will be written to and read from a buffer.
     *
     * @param data       Pre-existing data should be inserted into the buffer.
     * @param compressor The compression format the data uses.
     */
    public ObjectDataStructure(ByteBuffer data, Compressor compressor) {
        this.internal = new ODSMem(data, compressor);
    }

    /**
     * Create ODS using the memory storage type. Data will be written to and read from a buffer.
     * <p>
     * An empty buffer is created.
     */
    public ObjectDataStructure() {
        this.internal = new ODSMem();
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
    public <T extends Tag<?>> T get(String key) {
        return internal.get(key);
    }

    /**
     * Get all of the tags in the file/buffer.
     *
     * @return All of the tags.
     * <p>This will return null if there are no tags, or if the file does not exist.</p>
     */
    public List<Tag<?>> getAll() {
        return internal.getAll();
    }

    /**
     * Save tags to the file/buffer.
     * <p>This will overwrite the existing data. To append tags see {@link #append(Tag)} and {@link #appendAll(List)}</p>
     *
     * @param tags The list of tags to save.
     */
    public void save(List<? extends Tag<?>> tags) {
        internal.save(tags);
    }

    /**
     * Append tags to the end of the data.
     *
     * @param tag The tag to be appended.
     */
    public void append(Tag<?> tag) {
        internal.append(tag);
    }

    /**
     * Append a list of tags to the end of the data.
     *
     * @param tags The list of tags.
     */
    public void appendAll(List<Tag<?>> tags) {
        internal.appendAll(tags);
    }

    /**
     * Find if a key exists within the data.
     * <p>This will not throw ODSException if an IOException occurs.</p>
     *
     * @param key They key to find
     * @return If the key exists.
     */
    public boolean find(String key) {
        return internal.find(key);
    }

    /**
     * Remove a tag from the list.
     *
     * @param key The key to remove.
     * @return If the deletion was successfully done.
     */
    public boolean delete(String key) {
        return internal.delete(key);
    }

    /**
     * Replace a key with another tag.
     *
     * @param key         The key
     * @param replacement The data to replace
     * @return If the replacement was successful. ODSException is not thrown.
     */
    public boolean replaceData(String key, Tag<?> replacement) {
        return internal.replaceData(key, replacement);
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
     *              <p>When the key is set to "" (An empty string) than it is assumed you want to append to the parent file/buffer.</p>
     *              <p>Valid Tags:</p>
     *              <code>
     *              ObjectOne.tagToDelete<br>
     *              ObjectOne.NewObject.tagToAppend<br>
     *              ObjectOne.tagToSet<br>
     *              </code>
     * @param value The tag to append or replace the key with. <p>If this parameter is null than the key is deleted.</p>
     */
    public void set(String key, Tag<?> value) {
        internal.set(key, value);
    }

    /**
     * Get the array of bytes in any compression format.
     *
     * @param compressor The compression format.
     * @return The array of bytes.
     */
    public byte[] export(Compressor compressor) {
        return internal.export(compressor);
    }

}