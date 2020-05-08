package me.ryandw11.ods.tests;

import me.ryandw11.ods.serializer.Serializable;
import me.ryandw11.ods.tags.IntTag;
import me.ryandw11.ods.tags.ObjectTag;
import me.ryandw11.ods.tags.StringTag;

public class Owner {
    @Serializable
    private String firstName;
    @Serializable
    private String lastName;
    @Serializable
    private int age;

    public Owner init(String firstName, String lastName, int age){
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        return this;
    }

    public String getFirstName(){
        return firstName;
    }

    public String getLastName(){
        return lastName;
    }

    public int getAge(){
        return age;
    }

    public ObjectTag serialize(){
        ObjectTag ot = new ObjectTag("Owner");
        ot.addTag(new StringTag("firstName", firstName));
        ot.addTag(new StringTag("lastName", lastName));
        ot.addTag(new IntTag("age", age));
        return ot;
    }
}
