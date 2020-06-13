package me.ryandw11.ods.util;

/**
 * Contains the information of tags specified in a key.
 */
public class KeyScoutChild {
    private String name;
    private int size;
    // This is set the the position of the first tag size int byte. Aka tag beginning - 1
    private int startingIndex;
    public KeyScoutChild(){
        this.size = 0;
        this.startingIndex = -1;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

    public void setSize(int size){
        this.size = size;
    }

    /**
     * The size of the tag bytes.
     * This does not include the data type tag and the 4 size bytes.
     * To get the size of the entire tag do (getSize() + 5)
     * @return The size of the tag.
     */
    public int getSize(){
        return size;
    }

    public void setStartingIndex(int startingIndex){
        this.startingIndex = startingIndex;
    }

    /**
     * The index in the file where the first size byte is.
     * To get the start of the full tag do (getStartingIndex()-1)
     * @return The starting index of the first size byte for the tag.
     */
    public int getStartingIndex(){
        return startingIndex;
    }

    protected void removeSize(int amount){
        this.size -= amount;
    }

    protected void addSize(int amount){
        this.size += amount;
    }
}
