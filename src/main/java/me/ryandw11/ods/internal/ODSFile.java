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
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterOutputStream;

/**
 * The internal code for using a file.
 */
public class ODSFile implements ODSInternal {
    private final File file;
    private final Compressor compression;

    /**
     * The file to be saved to
     *
     * @param file        The file.
     * @param compression What compression the file should use.
     */
    public ODSFile(File file, Compressor compression) {
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
        return compression.getOutputStream(fos);
    }

    /**
     * Grab the input stream that is used based upon the compression format.
     *
     * @return The input stream that should be used.
     */
    private InputStream getInputStream() throws IOException {
        FileInputStream fis = new FileInputStream(file);
        return compression.getInputStream(fis);
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
            try (FileInputStream fis = (FileInputStream) stream) {
                FileChannel channel = fis.getChannel();
                return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            }
        } else
            return ByteBuffer.wrap(ODSIOUtils.toByteArray(stream));
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
        try {
            if (!file.exists()) return null;
            InputStream is = getInputStream();
            ByteBuffer buffer = getInputBuffer(is);
            T out = (T) InternalUtils.getSubObjectData(buffer, key);
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
            List<Tag<?>> output = InternalUtils.getListData(buffer, buffer.limit());
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
                data = ODSIOUtils.toByteArray(is);
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
                data = ODSIOUtils.toByteArray(is);
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
            return InternalUtils.findSubObjectData(buffer, key);
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
            KeyScout counter = InternalUtils.scoutObjectData(buffer, key, new KeyScout());
            if (counter == null) {
                return false;
            }
            byte[] deleteReturn = InternalUtils.deleteSubObjectData(buffer.array(), counter);
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

            KeyScout counter = InternalUtils.scoutObjectData(buffer, key, new KeyScout());
            if (counter.getEnd() == null) {
                return false;
            }

            ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(byteArrayOut);
            replacement.writeData(dos);

            byte[] replaceReturn = InternalUtils.replaceSubObjectData(buffer.array(), counter, byteArrayOut.toByteArray());
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
                InputStream stream = getInputStream();
                byte[] output = InternalUtils.setSubObjectData(ODSIOUtils.toByteArray(stream), counter, data);
                stream.close();
                // Write the new bytes to the file.
                OutputStream out = getOutputStream();
                out.write(output);
                out.close();
            } else {
                ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(byteArrayOut);
                value.writeData(dos);

                byte[] replaceReturn = InternalUtils.replaceSubObjectData(buffer.array(), counter, byteArrayOut.toByteArray());
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

    @Override
    public byte[] export(Compressor compressor) {
        try {
            InputStream io = getInputStream();
            byte[] data = ODSIOUtils.toByteArray(io);
            io.close();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            OutputStream os = compressor.getOutputStream(bos);
            os.write(data);
            byte[] output = bos.toByteArray();
            os.close();

            return output;
        } catch (IOException ex) {
            throw new ODSException("Unable to export data from file.", ex);
        }
    }

    /**
     * Import a file into this file.
     * <p>This basically copies one file to another.</p>
     * <p>This <b>will</b> overwrite the current file.</p>
     *
     * @param file       The file to copy from.
     * @param compressor The compression of the other file.
     */
    @Override
    public void importFile(File file, Compressor compressor) {
        try (InputStream is = compressor.getInputStream(new FileInputStream(file))) {
            byte[] data = ODSIOUtils.toByteArray(is);
            try (OutputStream fos = this.compression.getOutputStream(new FileOutputStream(this.file))) {
                fos.write(data);
            } catch (IOException ex) {
                throw new ODSException("Unable to export bytes to file.", ex);
            }
        } catch (IOException ex) {
            throw new ODSException("Unable to import bytes from files.", ex);
        }
    }

    /**
     * Export to another file.
     * <p>This basically copies the current file into another one.</p>
     *
     * @param file       The other file to copy to.
     * @param compressor The desired compression of the copy file.
     */
    @Override
    public void saveToFile(File file, Compressor compressor) {
        try (InputStream is = this.compression.getInputStream(new FileInputStream(this.file))) {
            byte[] data = ODSIOUtils.toByteArray(is);
            try (OutputStream fos = compressor.getOutputStream(new FileOutputStream(file))) {
                fos.write(data);
            } catch (IOException ex) {
                throw new ODSException("Unable to export bytes to file.", ex);
            }
        } catch (IOException ex) {
            throw new ODSException("Unable to import bytes from files.", ex);
        }
    }

    /**
     * Clears all the data from a file.
     * <p>This works internally by overwriting a file.</p>
     */
    @Override
    public void clear() {
        try {
            if (!this.file.createNewFile())
                throw new ODSException("Unable to clear file. Does the file have the correct permission?");
        } catch (IOException ex) {
            throw new ODSException("IO error occurred when clearing the file.", ex);
        }
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
