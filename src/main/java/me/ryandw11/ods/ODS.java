package me.ryandw11.ods;

import me.ryandw11.ods.exception.ODSException;
import me.ryandw11.ods.serializer.Serializable;
import me.ryandw11.ods.tags.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a utility class that provided wrappers for the different tags.
 * All wrappers exclude a name.
 * <p>This class also contains overall settings for ODS.</p>
 */
public class ODS {

    protected static boolean ignoreInvalidCustomTags = false;
    /**
     * This list stores all registered custom tags.
     */
    private static List<Tag<?>> customTags = new ArrayList<>();

    /**
     * Wrap an object to a tag.
     * <p>The name is not set by this method.</p>
     * <p>This method <b>cannot</b> wrap Lists, Maps. Objects are automatically serialized.</p>
     *
     * @param o The object to wrap.
     * @return The tag equivalent.
     */
    public static Tag<?> wrap(Object o) {
        return wrap("", o);
    }

    /**
     * Wrap an object to a tag.
     * <p>This method <b>cannot</b> wrap Lists, Maps. Objects are automatically serialized.</p>
     *
     * @param name The name of the tag to wrap.
     * @param o    The object to wrap.
     * @return The tag equivalent.
     */
    public static Tag<?> wrap(String name, Object o) {
        if (o instanceof Byte) {
            return new ByteTag(name, (byte) o);
        }
        if (o instanceof Character) {
            return new CharTag(name, (char) o);
        }
        if (o instanceof Double) {
            return new DoubleTag(name, (double) o);
        }
        if (o instanceof Float) {
            return new FloatTag(name, (float) o);
        }
        if (o instanceof Integer) {
            return new IntTag(name, (int) o);
        }
        if (o instanceof Long) {
            return new LongTag(name, (long) o);
        }
        if (o instanceof Short) {
            return new ShortTag(name, (short) o);
        }
        if (o instanceof String) {
            return new StringTag(name, (String) o);
        }
        if (o instanceof List) {
            return wrap(name, (List<?>) o);
        }
        return serialize(name, o);
    }

    /**
     * Unwrap a tag and turn it back into it's original value.
     * <p>This method will not work for Lists, Maps, and Objects.</p>
     *
     * @param tag The tag to unwrap.
     * @param <T> The data type of the tag.
     * @return The unwrapped tag.
     */
    public static <T> T unwrap(Tag<T> tag) {
        if (tag instanceof ObjectTag) {
            try {
                ObjectTag objTag = (ObjectTag) tag;
                String clazzName = (String) objTag.getTag("ODS_TAG").getValue();
                if (clazzName == null)
                    throw new RuntimeException("Cannot unwrap object: TagObject is not a serialized object!");
                return (T) deserialize(tag, Class.forName(clazzName));
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }
        return tag.getValue();
    }

    /**
     * Wrap a list into a list tag.
     *
     * @param name The name of the list tag.
     * @param list The list to turn into a list tag.
     *             <p>The list can only contain objects that can be converted into tags.
     *             The contents of the list are automatically wrapped.
     *             <b>Note: </b> This list cannot contain Objects, Lists, or Maps.</p>
     * @param <T>  The data type of the list.
     * @return The wrapped list.
     */
    public static <T> ListTag<?> wrap(String name, List<T> list) {
        List<Tag<?>> output = new ArrayList<>();
        for (T t : list) {
            output.add(wrap(t));
        }
        return new ListTag<>(name, output);
    }

    /**
     * Wrap all of the objects inside of the list.
     * <p>The same limitations apply as with the {@link #wrap(Object)} method.</p>
     *
     * @param list The list to wrap.
     * @param <T>  The data type of the list.
     * @return The wrapped list.
     */
    public static <T> List<Tag<?>> wrap(List<T> list) {
        List<Tag<?>> output = new ArrayList<>();
        for (T t : list) {
            output.add(wrap(t));
        }
        return output;
    }

    /**
     * Unwrap a list full of tags.
     * <p>This method is <b>not</b> for unwrapping the LinkList to a normal list.</p>
     *
     * @param tags The list of tags.
     * @param <T>  The data type of the tags.
     *             <p>The list must be a uniform data type.</p>
     * @return The unwrapped list.
     */
    public static <T> List<T> unwrapList(List<Tag<T>> tags) {
        List<T> output = new ArrayList<>();
        for (Tag<T> tag : tags) {
            output.add(unwrap(tag));
        }
        return output;
    }

    /**
     * Unwrap a ListTag into a list.
     *
     * @param list The ListTag to convert back into a list.
     *             <p>The contents of the list are automatically unwrapped.</p>
     * @param <T>  The data type of the list.
     * @return The unwrapped list.
     */
    public static <T> List<T> unwrapListTag(ListTag<? extends Tag<T>> list) {
        List<T> output = new ArrayList<>();
        for (Tag<?> tag : list.getValue()) {
            output.add(unwrap((Tag<T>) tag));
        }
        return output;
    }

    /**
     * Serialize an object.
     *
     * @param key The key
     * @param obj The object
     * @return The object tag.
     */
    public static ObjectTag serialize(String key, Object obj) {
        ObjectTag objectTag = new ObjectTag(key);
        try {
            Class<?> clazz = obj.getClass();
            objectTag.addTag(new StringTag("ODS_TAG", clazz.getName()));
            for (Field f : clazz.getDeclaredFields()) {
                if (f.getAnnotation(Serializable.class) == null) continue;
                f.setAccessible(true);
                objectTag.addTag(wrap(f.getName(), f.get(obj)));
                f.setAccessible(false);
            }
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
        if (objectTag.getValue().size() < 2)
            throw new RuntimeException("Cannot serialize object: No serializable fields detected!");
        return objectTag;
    }

    /**
     * Deserialize back into an object
     *
     * @param tag       The tag.
     * @param mainClazz The main class.
     * @param <T>       The deserialized class.
     * @return The object.
     */
    public static <T> T deserialize(Tag<?> tag, Class<T> mainClazz) {
        if (!(tag instanceof ObjectTag)) throw new RuntimeException("Cannot deserialize tag: Tag is not an ObjectTag!");
        ObjectTag objectTag = (ObjectTag) tag;
        if (!objectTag.hasTag("ODS_TAG"))
            throw new RuntimeException("Cannot deserialize tag: This tag was not serialized!");
        Object obj;
        try {
            obj = mainClazz.getConstructor().newInstance();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        for (Field f : mainClazz.getDeclaredFields()) {
            if (f.getAnnotation(Serializable.class) == null) continue;
            try {
                f.setAccessible(true);
                f.set(obj, unwrap(objectTag.getTag(f.getName())));
                f.setAccessible(false);
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }
        return (T) obj;
    }

    /**
     * Register a custom tag into the system. (This tag must respect the reserved tag ids).
     *
     * @param tag The tag class to add.
     */
    public static void registerCustomTag(Tag<?> tag) {
        if (tag.getID() > -1 && tag.getID() < 15)
            throw new ODSException("Invalid Tag ID. ID cannot be 0 - 15");
        ODS.customTags.add(tag);
    }

    /**
     * Get the list of custom tags.
     *
     * @return The list of custom tags.
     */
    public static List<Tag<?>> getCustomTags() {
        return ODS.customTags;
    }

    /**
     * Informs ods whether or not to thrown an error when it comes across an undefined custom tag.
     * <p>Only enable this when developing something like a visualizer program.</p>
     *
     * @param value Whether or not to thrown an error when it comes across an undefined custom tag.
     */
    public static void allowUndefinedTags(boolean value) {
        ODS.ignoreInvalidCustomTags = value;
    }
}
