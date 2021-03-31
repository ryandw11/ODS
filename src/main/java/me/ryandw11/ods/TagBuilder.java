package me.ryandw11.ods;

import me.ryandw11.ods.exception.ODSException;
import me.ryandw11.ods.tags.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.nio.ByteBuffer;

/**
 * Builds the tag.
 * Internal Class.
 */
public class TagBuilder {
    private int dataType;
    private int dataSize;
    private long startingIndex;
    private String name;
    private int nameSize;
    private ByteBuffer valueBytes;
    private int valueLength;

    public TagBuilder() {
        this.dataSize = -1;
        this.dataType = -1;
        this.startingIndex = -1;
        this.name = "";
        this.valueBytes = null;
        this.valueLength = -1;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public int getDataType() {
        return dataType;
    }

    public void setDataSize(int size) {
        this.dataSize = size;
    }

    public int getDataSize() {
        return dataSize;
    }

    public void setStartingIndex(long startingIndex) {
        this.startingIndex = startingIndex;
    }

    public long getStartingIndex() {
        return startingIndex;
    }

    public void setName(String s) {
        this.name = s;
    }

    public String getName() {
        return this.name;
    }

    public void setNameSize(int size) {
        this.nameSize = size;
    }

    public int getNameSize() {
        return this.nameSize;
    }

    public void setValueBytes(ByteBuffer valueBytes) {
        this.valueBytes = valueBytes;
    }

    public ByteBuffer getValueBytes() {
        return this.valueBytes;
    }

    public void setValueLength(int length) {
        this.valueLength = length;
    }

    public int getValueLength() {
        return valueLength;
    }

    public Tag<?> process() {
        switch (getDataType()) {
            case 1:
                return new StringTag(this.name, null).createFromData(this.valueBytes, this.valueLength);
            case 2:
                return new IntTag(this.name, -1).createFromData(this.valueBytes, this.valueLength);
            case 3:
                return new FloatTag(this.name, -1).createFromData(this.valueBytes, this.valueLength);
            case 4:
                return new DoubleTag(this.name, -1).createFromData(this.valueBytes, this.valueLength);
            case 5:
                return new ShortTag(this.name, (short) -1).createFromData(this.valueBytes, this.valueLength);
            case 6:
                return new LongTag(this.name, -1).createFromData(this.valueBytes, this.valueLength);
            case 7:
                return new CharTag(this.name, ' ').createFromData(this.valueBytes, this.valueLength);
            case 8:
                return new ByteTag(this.name, (byte) 0).createFromData(this.valueBytes, this.valueLength);
            case 9:
                return new ListTag<>(this.name, null).createFromData(this.valueBytes, this.valueLength);
            case 10:
                return new MapTag<>(this.name, null).createFromData(this.valueBytes, this.valueLength);
            case 11:
                return new ObjectTag(this.name).createFromData(this.valueBytes, this.valueLength);
            case 12:
                return new CompressedObjectTag(this.name).createFromData(this.valueBytes, this.valueLength);
            default:
                for (Tag<?> tag : ODS.getCustomTags()) {
                    if (getDataType() != tag.getID()) continue;
                    Class<?> tagClazz = tag.getClass();
                    try {
                        return ((Tag<?>) tagClazz.getConstructor(String.class,
                                (Class<?>) ((ParameterizedType) tagClazz.getGenericInterfaces()[0])
                                        .getActualTypeArguments()[0]).newInstance(this.name, null))
                                .createFromData(this.valueBytes, this.valueLength);
                    } catch (NoSuchMethodException ex) {
                        throw new ODSException("Invalid Custom Tag Constructor.");
                    } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                        throw new ODSException("Unable to create an instance of a custom tag. IllegalAccessException, InstantiationException, " +
                                "or InvocationTargetException.");
                    }
                }
                if (!ODS.ignoreInvalidCustomTags)
                    throw new RuntimeException("Error: That data type does not exist!");
                else
                    return new InvalidTag(this.name).createFromData(this.valueBytes, this.valueLength);
        }
    }

}
