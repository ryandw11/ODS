package me.ryandw11.ods.tests;

import me.ryandw11.ods.ODS;
import me.ryandw11.ods.serializer.Serializable;
import me.ryandw11.ods.tags.IntTag;
import me.ryandw11.ods.tags.ObjectTag;
import me.ryandw11.ods.tags.StringTag;

import java.util.List;

public class Car {

    @Serializable
    private String type;
    @Serializable
    private int gas;
    @Serializable
    private List<String> cords;
    @Serializable
    private Owner owner;

    public Car init(String type, int gas, List<String> cords, Owner owner){
        this.type = type;
        this.gas = gas;
        this.cords = cords;
        this.owner = owner;
        return this;
    }

    public String getType(){
        return type;
    }

    public int getGas(){
        return gas;
    }

    public List<String> getCords(){
        return cords;
    }

    public Owner getOwner(){
        return owner;
    }

    public ObjectTag serialize(int i){
        ObjectTag ot = new ObjectTag("car" + i);
        ot.addTag(new StringTag("type", type));
        ot.addTag(new IntTag("gas", gas));
        ot.addTag(ODS.wrap("cords", cords));
        ot.addTag(owner.serialize());
        return ot;
    }
}
