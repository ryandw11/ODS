package me.ryandw11.ods.tags;

import me.ryandw11.ods.Tag;
import me.ryandw11.ods.internal.InternalUtils;
import me.ryandw11.ods.io.CountingOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The map tag. It is assumed that the key of the map is a String.
 *
 * @param <T> The tag to have in a map.
 */
public class MapTag<T extends Tag<?>> implements Tag<Map<String, T>> {
    private String name;
    private Map<String, T> value;

    /**
     * Construct a map tag from a map.
     *
     * @param name  The name of the tag.
     * @param value The map to construct from.
     */
    public MapTag(String name, Map<String, T> value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public void setValue(Map<String, T> s) {
        this.value = s;
    }

    @Override
    public Map<String, T> getValue() {
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
        for (Map.Entry<String, T> entry : this.value.entrySet()) {
            entry.getValue().setName(entry.getKey());
            entry.getValue().writeData(tempDos);

        }

        dos.writeInt(cos.getCount());
        dos.write(os.toByteArray());
        tempDos.close();
    }

    @Override
    public Tag<Map<String, T>> createFromData(ByteBuffer value, int length) {
        List<Tag<?>> data;
        data = InternalUtils.getListData(value, length);
        Map<String, T> output = new HashMap<>();
        for (Tag<?> tag : data) {
            output.put(tag.getName(), (T) tag);
            tag.setName("");
        }
        this.value = output;
        return this;
    }

    @Override
    public byte getID() {
        return 10;
    }
}
