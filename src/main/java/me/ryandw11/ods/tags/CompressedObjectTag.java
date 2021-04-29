package me.ryandw11.ods.tags;

import me.ryandw11.ods.ODS;
import me.ryandw11.ods.Tag;
import me.ryandw11.ods.compression.Compressor;
import me.ryandw11.ods.compression.GZIPCompression;
import me.ryandw11.ods.exception.ODSException;
import me.ryandw11.ods.internal.InternalUtils;
import me.ryandw11.ods.io.CountingOutputStream;
import me.ryandw11.ods.io.ODSIOUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Any tags within the CompressedObject will be compressed using the specified compressor when being written to a file or memory.
 *
 * <p>Tags within a compressed object cannot be obtained using {@link me.ryandw11.ods.ObjectDataStructure#get(String)}
 * or similar methods. Attempting to will result in a {@link me.ryandw11.ods.exception.CompressedObjectException}.</p>
 */
public class CompressedObjectTag implements Tag<List<Tag<?>>> {
    private String name;
    private Compressor compressor;
    private List<Tag<?>> value;

    /**
     * Construct a compressed object tag with existing tags.
     *
     * @param name       The name of the object.
     * @param value      The list of tags to add.
     * @param compressor The compressor to use.
     */
    public CompressedObjectTag(String name, List<Tag<?>> value, Compressor compressor) {
        this.name = name;
        this.value = value;
        this.compressor = compressor;
    }

    /**
     * Construct a compressed object tag with no existing tags.
     *
     * @param name       The name of the object.
     * @param compressor The compressor to use.
     */
    public CompressedObjectTag(String name, Compressor compressor) {
        this.name = name;
        this.compressor = compressor;
        this.value = new ArrayList<>();
    }

    /**
     * Construct an compressed object tag with not existing tags.
     * <p>GZIP Compression is used by default.</p>
     *
     * @param name The name of the tag.
     */
    public CompressedObjectTag(String name) {
        this(name, new ArrayList<>(), new GZIPCompression());
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
     *
     * @param tag The tag to remove.
     */
    public void removeTag(Tag<?> tag) {
        value.remove(tag);
    }

    /**
     * Remove all tags that have the specified name.
     *
     * @param name The name to remove.
     */
    public void removeTag(String name) {
        value.removeIf(tag -> tag.getName().equals(name));
    }

    /**
     * Remove all tags from an object.
     */
    public void removeAllTags() {
        value.clear();
    }

    /**
     * If the object has a specified tag.
     *
     * @param name The tag's name.
     * @return The tag.
     */
    public boolean hasTag(String name) {
        return value.stream().anyMatch(tag -> tag.getName().equals(name));
    }

    /**
     * Get the compressor used.
     *
     * @return The compressor used.
     */
    public Compressor getCompressor() {
        return compressor;
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

        String compressorName = ODS.getCompressorName(compressor);
        if (compressorName == null)
            throw new ODSException("Unable to find compressor: " + compressor);
        // Write the name of the compressor so it can be removed.
        tempDos.writeShort(compressorName.getBytes(StandardCharsets.UTF_8).length);
        tempDos.write(compressorName.getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream osTemp = new ByteArrayOutputStream();
        OutputStream compressedStream = compressor.getOutputStream(osTemp);
        DataOutputStream tempDos2 = new DataOutputStream(compressedStream);
        // The size of the array.
        for (Tag<?> tag : this.value) {
            tag.writeData(tempDos2);
        }
        tempDos2.close();
        tempDos.write(osTemp.toByteArray());

        dos.writeInt(cos.getCount());
        dos.write(os.toByteArray());
        tempDos.close();
    }

    @Override
    public Tag<List<Tag<?>>> createFromData(ByteBuffer value, int length) {
        short compressorLength = value.getShort();
        length -= 2 + compressorLength;
        byte[] compressorBytes = new byte[compressorLength];
        value.get(compressorBytes);
        String compressionName = new String(compressorBytes);
        Compressor compressor = ODS.getCompressor(compressionName);

        if (compressor == null)
            throw new ODSException("Cannot find compressor: " + compressionName);

        this.compressor = compressor;
        byte[] bitData = new byte[length];
        value.get(bitData);

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bitData);
            InputStream compressedInputStream = compressor.getInputStream(bis);
            ByteBuffer buffer = ByteBuffer.wrap(ODSIOUtils.toByteArray(compressedInputStream));
            bis.close();

            List<Tag<?>> data;
            data = InternalUtils.getListData(buffer, buffer.capacity());
            this.value = data;
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new ODSException("An IO Error has occurred!", ex);
        }

        return this;
    }

    @Override
    public byte getID() {
        return 12;
    }
}
