package me.ryandw11.ods;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * The primary class of the ObjectDataStructure library.
 * <p>This class uses Java NIO instead of IO.</p>
 */
public class ObjectDataStructureNIO {
    private File file;
    private Compression compression;

    /**
     * The file that is to be saved to
     * @param file The file.
     */
    public ObjectDataStructureNIO(File file){
        this(file, Compression.GZIP);
    }

    /**
     * The file to be saved to
     * @param file The file.
     * @param compression What compression the file should use.
     */
    public ObjectDataStructureNIO(File file, Compression compression){
        this.file = file;
        this.compression = compression;
    }

    private OutputStream getOutputStream() throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        if(compression == Compression.GZIP)
            return new GZIPOutputStream(fos);
        if(compression == Compression.ZLIB)
            return new DeflaterOutputStream(fos);
        return fos;
    }

    private InputStream getInputStream() throws IOException {
        FileInputStream fis = new FileInputStream(file);
        if(compression == Compression.GZIP)
            return new GZIPInputStream(fis);
        if(compression == Compression.ZLIB)
            return new InflaterInputStream(fis);
        return fis;
    }

    /**
     * Grab a tag based upon the name.
     * TODO Look into ZLIB compression instead of gzip.
     * <p>This will not work with object notation see {@link #getObject(String)} for that method.</p>
     * <p>Ensure you are storing this result with the correct data type.</p>
     * @param name The name of the tag.
     * @return The tag.
     *         <p>This will return null if no tag with the name specified is found, or if the file does not exist.</p>
     */
    public <T extends Tag<?>> T get(String name){
        try{
            if(!file.exists()) return null;
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer data = ByteBuffer.allocate((int) channel.size());
            channel.read(data);
            T out = (T) getSubData(data, name);
            channel.close();
            raf.close();
            return out;
        }catch(IOException ex){
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Grab a tag based upon an object key.
     * <p>This method allows you to directly get sub-objects with little overhead.</p>
     * <code>
     *     getObject("primary.firstsub.secondsub");
     * </code>
     * @param key The key to use.
     * @return The object Tag.
     *      <p>This will return null if the requested sub-object does not exist, or if the file does not exist.</p>
     */
    public <T> T getObject(String key){
        try{
            if(!file.exists()) return null;
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer data = ByteBuffer.allocate((int) channel.size());
            channel.read(data);
            T out = (T) getSubObjectData(data, key);
            channel.close();
            raf.close();
            return out;
        }catch(IOException ex){
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Get all of the tags in the file.
     * @return All of the tags.
     * <p>This will return null if there are no tags, or if the file does not exist.</p>
     */
    public List<Tag<?>> getAll(){
        try{
            if(!file.exists()) return null;
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer data = ByteBuffer.allocate((int) channel.size());
            channel.read(data);
            List<Tag<?>> output = getListData(data);
            channel.close();
            raf.close();
            return output;
        }catch(IOException ex){
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Save tags to the file.
     * <p>This will overwrite the existing file. To append tags see {@link #append(Tag)} and {@link #appendAll(List)}</p>
     * @param tags The list of tags to save.
     */
    public void save(List<? extends Tag<?>> tags){
        try{
            OutputStream os = getOutputStream();
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));
            for(Tag<?> tag : tags){
                tag.writeData(dos);
            }
            dos.close();
            os.close();
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    /**
     * Append tags to the end of the file.
     * @param tag The tag to be appended.
     */
    public void append(Tag<?> tag){
        try{
            if(!file.exists()) file.createNewFile();
            OutputStream os = getOutputStream();
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));

            InputStream is = getInputStream();
            dos.write(is.readAllBytes());
            is.close();
            tag.writeData(dos);

            dos.close();
            os.close();
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    /**
     * Append a list of tags to the end of the file.
     * @param tags The list of tags.
     */
    public void appendAll(List<Tag<?>> tags){
        try{
            if (!file.exists()) file.createNewFile();
            OutputStream os = getOutputStream();
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));

            InputStream is = getInputStream();
            dos.write(is.readAllBytes());
            is.close();
            for(Tag<?> tag : tags){
                tag.writeData(dos);
            }
            dos.close();
            is.close();
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    /**
     * Get a tag based upon the name.
     * @param data The list of data.
     * @param name The name
     * @return The tag
     * @throws IOException This method does not handle the possible IO Exceptions.
     */
    private Tag<?> getSubData(ByteBuffer data, String name) throws IOException {
        data.flip();
        TagBuilder currentBuilder = new TagBuilder();
        while(data.hasRemaining()){
            currentBuilder.setDataType(data.get());
            currentBuilder.setDataSize(data.getInt());
            currentBuilder.setStartingIndex(data.position());
            currentBuilder.setNameSize(((Short) data.getShort()).intValue());
            // If the name size isn't the same, then don't waste time reading the name.
            if(currentBuilder.getNameSize() != name.getBytes(StandardCharsets.UTF_8).length){
                data.position(((Long)currentBuilder.getStartingIndex()).intValue() + currentBuilder.getDataSize());
                currentBuilder = new TagBuilder();
                continue;
            }
            //TODO make sure the long math does not screw up this system.
            byte[] nameBytes = new byte[currentBuilder.getNameSize()];
            data.get(nameBytes);
            String tagName = new String(nameBytes, StandardCharsets.UTF_8);
            currentBuilder.setName(tagName);
            // If the name is not correct, skip forward!
            if(!name.equals(tagName)){
                data.position(((Long)currentBuilder.getStartingIndex()).intValue() + currentBuilder.getDataSize());
                currentBuilder = new TagBuilder();
                continue;
            }

            byte[] value = new byte[((int) currentBuilder.getStartingIndex() - data.position()) + currentBuilder.getDataSize()];
            data.get(value);
            currentBuilder.setValueBytes(value);
            data.clear();
            return currentBuilder.process();
        }
        data.clear();
        return null;
    }

    /**
     * Get a tag based upon the name.
     * TODO remove byte[] from params
     * @param data The list of data.
     * @param key The key
     * @return The tag
     * @throws IOException This method does not handle the possible IO Exceptions.
     */
    private Tag<?> getSubObjectData(ByteBuffer data, String key) throws IOException {
        data.flip();
        String name = key.split("\\.")[0];
        String otherKey = getKey(key.split("\\."));

        TagBuilder currentBuilder = new TagBuilder();
        while(data.hasRemaining()){
            currentBuilder.setDataType(data.get());
            currentBuilder.setDataSize(data.getInt());
            currentBuilder.setStartingIndex(data.position());
            currentBuilder.setNameSize(((Short) data.getShort()).intValue());
            // If the name size isn't the same, then don't waste time reading the name.
            if(currentBuilder.getNameSize() != name.getBytes(StandardCharsets.UTF_8).length){
                data.position(((Long)currentBuilder.getStartingIndex()).intValue() + currentBuilder.getDataSize());
                currentBuilder = new TagBuilder();
                continue;
            }

            byte[] nameBytes = new byte[currentBuilder.getNameSize()];
            data.get(nameBytes);
            String tagName = new String(nameBytes, StandardCharsets.UTF_8);
            currentBuilder.setName(tagName);
            // If the name is not correct, skip forward!
            if(!name.equals(tagName)){
                data.position(((Long)currentBuilder.getStartingIndex()).intValue() + currentBuilder.getDataSize());
                currentBuilder = new TagBuilder();
                continue;
            }

            byte[] value = new byte[((int) currentBuilder.getStartingIndex() - data.position()) + currentBuilder.getDataSize()];
            data.get(value);
            currentBuilder.setValueBytes(value);
            data.clear();
            if(otherKey != null) {
                ByteBuffer newData = ByteBuffer.wrap(currentBuilder.getValueBytes());
                newData.position(value.length);
                return getSubObjectData(newData, otherKey);
            }
            return currentBuilder.process();
        }
        data.clear();
        return null;
    }

    private String getKey(String[] s){
        List<String> list = new ArrayList<>(Arrays.asList(s));
        list.remove(0);
        if(list.size() == 1) return list.get(0);
        if(list.size() < 1) return null;

        return String.join(".", list);
    }

    /**
     * Get the list of tags based upon bytes.
     * <p>This is meant for internal use only.</p>
     * @param data the array of bytes
     * @return The List of tags.
     * @throws IOException This method does not handle any of the possible IO Exceptions.
     */
    public static List<Tag<?>> getListData(ByteBuffer data) throws IOException {
        data.flip();
        List<Tag<?>> output = new ArrayList<>();

        TagBuilder currentBuilder = new TagBuilder();
        while(data.hasRemaining()){
            currentBuilder.setDataType(data.get());
            currentBuilder.setDataSize(data.getInt());
            currentBuilder.setStartingIndex(data.position());
            currentBuilder.setNameSize(((Short) data.getShort()).intValue());

            byte[] nameBytes = new byte[currentBuilder.getNameSize()];
            data.get(nameBytes);
            String tagName = new String(nameBytes, StandardCharsets.UTF_8);
            currentBuilder.setName(tagName);

            byte[] value = new byte[((int) currentBuilder.getStartingIndex() - data.position()) + currentBuilder.getDataSize()];
            data.get(value);
            currentBuilder.setValueBytes(value);
            output.add(currentBuilder.process());
        }
        data.clear();
        return output;
    }
}
