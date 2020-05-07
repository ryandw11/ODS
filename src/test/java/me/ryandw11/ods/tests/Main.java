package me.ryandw11.ods.tests;

import me.ryandw11.ods.*;
import me.ryandw11.ods.tags.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        ObjectDataStructure ods = new ObjectDataStructure(new File("test.ods"));

        List<Tag<?>> tags = new ArrayList<>();
        tags.add(new StringTag("ExampleKey", "This is an example string!"));
        tags.add(new IntTag("ExampleInt", 754));

        ObjectTag car = new ObjectTag("Car");
        car.addTag(new StringTag("type", "Jeep"));
        car.addTag(new IntTag("gas", 30));
        car.addTag(new ListTag<>("coords", ODS.wrap(Arrays.asList(10, 5, 10))));

        ObjectTag owner = new ObjectTag("Owner");
        owner.addTag(new StringTag("firstName", "Jeff"));
        owner.addTag(new StringTag("lastName", "Bob"));
        owner.addTag(new IntTag("Age", 30));
        car.addTag(owner);

        tags.add(car);

        ods.save(tags);

        // ===================================
        // Loading Objects
        // ===================================
        StringTag tag = ods.get("ExampleKey");
        System.out.println("The value of ExampleKey is: " + tag.getValue());

        ObjectTag myCar = ods.get("Car");
        System.out.println("The car is a " + myCar.getTag("type").getValue());

        StringTag ownerFirstName = ods.getObject("Car.Owner.firstName");
        StringTag ownerLastName = ods.getObject("Car.Owner.lastName");
        System.out.println("The owner of the car is " + ODS.unwrap(ownerFirstName) + " " + ODS.unwrap(ownerLastName));

    }
}
