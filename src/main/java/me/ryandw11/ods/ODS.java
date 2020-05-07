package me.ryandw11.ods;

import me.ryandw11.ods.tags.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This is a utility class that provided wrappers for the different tags.
 *  All wrappers exclude a name.
 */
public class ODS {
    /**
     * Wrap an object to a tag.
     * <p>The name is not set by this method.</p>
     * <p>This method <b>cannot</b> wrap Lists, Maps, or Objects.</p>
     * @param o The object to wrap.
     * @return The tag equivalent.
     */
    public static Tag<?> wrap(Object o){
        if(o instanceof Byte){
            return new ByteTag("", (byte) o);
        }
        if(o instanceof Character){
            return new CharTag("", (char) o);
        }
        if(o instanceof Double){
            return new DoubleTag("", (double) o);
        }
        if(o instanceof Float){
            return new FloatTag("", (float) o);
        }
        if(o instanceof Integer){
            return new IntTag("", (int) o);
        }
        if(o instanceof Long){
            return new LongTag("", (long) o);
        }
        if(o instanceof Short){
            return new ShortTag("", (short) o);
        }
        if(o instanceof String){
            return new StringTag("", (String) o);
        }
        throw new RuntimeException("Cannot wrap object: Invalid object type.");
    }

    /**
     * Unwrap a tag and turn it back into it's original value.
     * <p>This method will not work for Lists, Maps, and Objects.</p>
     * @param tag The tag to unwrap.
     * @param <T> The data type of the tag.
     * @return The unwrapped tag.
     */
    public static <T> T unwrap(Tag<T> tag){
        return tag.getValue();
    }

    /**
     * Wrap a list into a list tag.
     * @param name The name of the list tag.
     * @param list The list to turn into a list tag.
     *             <p>The list can only contain objects that can be converted into tags.
     *             The contents of the list are automatically wrapped.
     *             <b>Note: </b> This list cannot contain Objects, Lists, or Maps.</p>
     * @param <T> The data type of the list.
     * @return The wrapped list.
     */
    public static <T> ListTag<?> wrap(String name, List<T> list){
        List<Tag<?>> output = new ArrayList<>();
        for(T t : list){
            output.add(wrap(t));
        }
        return new ListTag<>(name, output);
    }

    /**
     * Wrap all of the objects inside of the list.
     * <p>The same limitations apply as with the {@link #wrap(Object)} method.</p>
     * @param list The list to wrap.
     * @param <T> The data type of the list.
     * @return The wrapped list.
     */
    public static <T> List<Tag<?>> wrap(List<T> list){
        List<Tag<?>> output = new ArrayList<>();
        for(T t : list){
            output.add(wrap(t));
        }
        return output;
    }

    /**
     * Unwrap a list full of tags.
     * <p>This method is <b>not</b> for unwrapping the LinkList to a normal list.</p>
     * @param tags The list of tags.
     * @param <T> The data type of the tags.
     *           <p>The list must be a uniform data type.</p>
     * @return The unwrapped list.
     */
    public static <T> List<T> unwrap(List<Tag<T>> tags){
        List<T> output = new ArrayList<>();
        for(Tag<T> tag : tags){
            output.add(unwrap(tag));
        }
        return output;
    }

    /**
     * Unwrap a ListTag into a list.
     * @param list The ListTag to convert back into a list.
     *             <p>The contents of the list are automatically unwrapped.</p>
     * @param <T> The data type of the list.
     * @return The unwrapped list.
     */
    public static <T> List<T> unwrapList(ListTag<? extends Tag<T>> list){
        List<T> output = new ArrayList<>();
        for(Tag<?> tag : list.getValue()){
            output.add(unwrap((Tag<T>) tag));
        }
        return output;
    }
}
