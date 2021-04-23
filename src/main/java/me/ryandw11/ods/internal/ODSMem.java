package me.ryandw11.ods.internal;

import me.ryandw11.ods.Tag;
import me.ryandw11.ods.compression.Compressor;
import me.ryandw11.ods.exception.ODSException;
import me.ryandw11.ods.io.ODSIOUtils;
import me.ryandw11.ods.tags.ObjectTag;
import me.ryandw11.ods.util.KeyScout;
import me.ryandw11.ods.util.KeyScoutChild;

import java.io.*;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The internal class for memory.
 */
public class ODSMem implements ODSInternal {
    private ByteBuffer memBuffer;

    /**
     * Import in pre-existing data.
     *
     * @param data       The array of data.
     * @param compressor The compression used.
     */
    public ODSMem(byte[] data, Compressor compressor) {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        try {
            InputStream is = compressor.getInputStream(bis);
            this.memBuffer = ByteBuffer.wrap(ODSIOUtils.toByteArray(is));
        } catch (IOException ex) {
            throw new ODSException("Cannot decompress data.", ex);
        }
    }

    /**
     * Import in pre-existing data.
     *
     * @param buffer     The buffer containing the data.
     * @param compressor The compression used.
     */
    public ODSMem(ByteBuffer buffer, Compressor compressor) {
        ByteArrayInputStream bis = new ByteArrayInputStream(buffer.array());
        try {
            InputStream is = compressor.getInputStream(bis);
            this.memBuffer = ByteBuffer.wrap(ODSIOUtils.toByteArray(is));
        } catch (IOException ex) {
            throw new ODSException("Cannot decompress data.", ex);
        }
    }

    /**
     * Does not pre-populate.
     */
    public ODSMem() {
        this.memBuffer = ByteBuffer.allocate(1);
    }

    /**
     * Sets the position of the buffer to 0 and returns the buffer.
     *
     * @return The corresponding byte buffer.
     */
    private ByteBuffer getInputBuffer() {
        memBuffer.position(0);
        return memBuffer;
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
     * <p>This will return null if the requested sub-object does not exist.</p>
     */
    public <T extends Tag<?>> T get(String key) {
        try {
            memBuffer.position(0);
            if (memBuffer.limit() == 1)
                return null;
            ByteBuffer buffer = getInputBuffer();
            T out = (T) InternalUtils.getSubObjectData(buffer, key);
            return out;
        } catch (BufferOverflowException | BufferUnderflowException ex) {
            throw new ODSException("Invalid format or the buffer has been tampered with / corrupted.");
        }
    }

    /**
     * Get all of the tags in the buffer.
     *
     * @return All of the tags.
     * <p>This will return null if there are no tags.</p>
     */
    public List<Tag<?>> getAll() {
        try {
            if (memBuffer.limit() == 1)
                return null;
            ByteBuffer buffer = getInputBuffer();
            List<Tag<?>> output = InternalUtils.getListData(buffer, buffer.limit());
            return output;
        } catch (BufferOverflowException | BufferUnderflowException ex) {
            throw new ODSException("Invalid format or the buffer has been tampered with / corrupted.");
        }
    }

    /**
     * Save tags to the buffer.
     * <p>This will overwrite the existing buffer. To append tags see {@link #append(Tag)} and {@link #appendAll(List)}</p>
     *
     * @param tags The list of tags to save.
     */
    public void save(List<? extends Tag<?>> tags) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));
            for (Tag<?> tag : tags) {
                tag.writeData(dos);
            }
            dos.close();
            os.close();
            this.memBuffer = ByteBuffer.wrap(os.toByteArray());
        } catch (IOException ex) {
            throw new ODSException("Error when saving information to the buffer", ex);
        }
    }

    /**
     * Append tags to the end of the buffer.
     *
     * @param tag The tag to be appended.
     */
    public void append(Tag<?> tag) {
        try {
            byte[] data = this.memBuffer.array();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));

            dos.write(data);
            tag.writeData(dos);

            dos.close();
            os.close();
            this.memBuffer = ByteBuffer.wrap(data);
        } catch (IOException ex) {
            throw new ODSException("Error when saving information to the buffer", ex);
        }
    }

    /**
     * Append a list of tags to the end of the buffer.
     *
     * @param tags The list of tags.
     */
    public void appendAll(List<Tag<?>> tags) {
        try {
            byte[] data = this.memBuffer.array();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));

            dos.write(data);
            for (Tag<?> tag : tags) {
                tag.writeData(dos);
            }
            dos.close();
            this.memBuffer = ByteBuffer.wrap(os.toByteArray());
        } catch (IOException ex) {
            throw new ODSException("Error when saving information to the buffer", ex);
        }
    }

    /**
     * Find if a key exists within the buffer.
     *
     * @param key They key to find
     * @return If the key exists.
     */
    public boolean find(String key) {
        try {
            ByteBuffer buffer = getInputBuffer();
            return InternalUtils.findSubObjectData(buffer, key);
        } catch (BufferOverflowException | BufferUnderflowException ex) {
            throw new ODSException("Invalid format or the buffer has been tampered with / corrupted.");
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
            ByteBuffer buffer = getInputBuffer();
            KeyScout counter = InternalUtils.scoutObjectData(buffer, key, new KeyScout());
            if (counter == null) {
                return false;
            }
            byte[] deleteReturn = InternalUtils.deleteSubObjectData(buffer.array(), counter);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(deleteReturn);
            this.memBuffer = ByteBuffer.wrap(out.toByteArray());
            out.close();
            return true;
        } catch (IOException ex) {
            return false;
        } catch (BufferOverflowException | BufferUnderflowException ex) {
            throw new ODSException("Invalid format or the buffer has been tampered with / corrupted.");
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
            ByteBuffer buffer = getInputBuffer();

            KeyScout counter = InternalUtils.scoutObjectData(buffer, key, new KeyScout());
            if (counter.getEnd() == null) {
                return false;
            }

            ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(byteArrayOut);
            replacement.writeData(dos);

            byte[] replaceReturn = InternalUtils.replaceSubObjectData(buffer.array(), counter, byteArrayOut.toByteArray());
            this.memBuffer = ByteBuffer.wrap(replaceReturn);
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
     *              <p>When the key is set to "" (An empty string) than it is assumed you want to append to the parent buffer.</p>
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
            ByteBuffer buffer = getInputBuffer();

            KeyScout counter = InternalUtils.scoutObjectData(buffer, key, new KeyScout());
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
                byte[] output = InternalUtils.setSubObjectData(this.memBuffer.array(), counter, data);
                // Write the new bytes to the file.
                this.memBuffer = ByteBuffer.wrap(output);
            } else {
                ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(byteArrayOut);
                value.writeData(dos);

                byte[] replaceReturn = InternalUtils.replaceSubObjectData(buffer.array(), counter, byteArrayOut.toByteArray());
                this.memBuffer = ByteBuffer.wrap(replaceReturn);
                dos.close();
                byteArrayOut.close();
            }
        } catch (IOException ex) {
            throw new ODSException("An error had occurred when trying to set data. Does that key exist?", ex);
        } catch (BufferOverflowException | BufferUnderflowException ex) {
            throw new ODSException("Invalid format or the buffer has been tampered with / corrupted.");
        }
    }

    @Override
    public byte[] export(Compressor compressor) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            OutputStream os = compressor.getOutputStream(bos);
            os.write(this.memBuffer.array());
            byte[] output = bos.toByteArray();
            os.close();
            return output;
        } catch (IOException ex) {
            throw new ODSException("Unable to export bytes from memory.", ex);
        }
    }
}
