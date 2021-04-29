package me.ryandw11.ods.internal;

import me.ryandw11.ods.Tag;
import me.ryandw11.ods.compression.Compressor;

import java.io.File;
import java.util.List;

/**
 * This interface is for the different ways data can be stored / read. viz. File and Memory
 * <p>
 * To see the method explanations please view the individual implementation: {@link ODSFile}, {@link ODSMem}.
 * You can also view the main explanations at {@link me.ryandw11.ods.ObjectDataStructure}.
 */
public interface ODSInternal {
    <T extends Tag<?>> T get(String key);

    List<Tag<?>> getAll();

    void save(List<? extends Tag<?>> tags);

    void append(Tag<?> tag);

    void appendAll(List<Tag<?>> tags);

    boolean find(String key);

    boolean delete(String key);

    boolean replaceData(String key, Tag<?> replacement);

    void set(String key, Tag<?> value);

    byte[] export(Compressor compressor);

    void importFile(File file, Compressor compressor);

    void saveToFile(File file, Compressor compressor);

    void clear();
}
