package me.ryandw11.ods;

import java.io.DataOutputStream;
import java.io.IOException;

public interface Tag<T> {
    void setValue(T t);

    T getValue();

    void setName(String name);
    String getName();

    void writeData(DataOutputStream dos) throws IOException;
    Tag<T> createFromData(byte[] value);

    byte getID();
}
