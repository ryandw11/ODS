package me.ryandw11.ods.util;

import java.util.ArrayList;
import java.util.List;

/**
 * An internal class that is used to contain information about a key.
 * <p>For each object in a key a KeyScoutChild is stored. The Tag of the key is stored in the end field.</p>
 */
public class KeyScout {

    private List<KeyScoutChild> children;
    private KeyScoutChild end;

    public KeyScout(){
        this.children = new ArrayList<>();
    }

    /**
     * Add a child to the key scout.
     * @param child The child.
     */
    public void addChild(KeyScoutChild child){
        this.children.add(child);
    }

    /**
     * Get the children (parent objects of the desired end tag).
     * @return The child objects.
     */
    public List<KeyScoutChild> getChildren(){
        return children;
    }

    /**
     * Get the tag parent by name.
     * @param name The name
     * @return The key scout child.
     */
    public KeyScoutChild getChildByName(String name){
        for(KeyScoutChild child : children){
            if(child.getName().equals(name))
                return child;
        }
        return null;
    }

    /**
     * Get the information for the desired tag.
     * @return The information.
     */
    public KeyScoutChild getEnd(){
        return end;
    }

    /**
     * Set the information for the ending tag.
     * @param end The end.
     */
    public void setEnd(KeyScoutChild end){
        this.end = end;
    }

    /**
     * Remove x size from the parent object headers.
     * @param size The size to remove.
     */
    public void removeAmount(int size){
        for(KeyScoutChild child : children){
            child.removeSize(size);
        }
    }

    /**
     * Add x size to the parent object headers.
     * @param size The size to add.
     */
    public void addAmount(int size){
        for(KeyScoutChild child : children){
            child.addSize(size);
        }
    }
}
