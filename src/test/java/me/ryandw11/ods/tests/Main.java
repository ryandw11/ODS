package me.ryandw11.ods.tests;

import me.ryandw11.ods.*;
import me.ryandw11.ods.tags.*;
import me.ryandw11.ods.tests.json.JsonTests;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        ObjectDataStructure ods = new ObjectDataStructure(new File("test.ods"));

        long time = System.currentTimeMillis();

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

        Random rand = new Random();
        Car c = new Car();
        String type = TestConstants.carTypes.get(rand.nextInt(TestConstants.carTypes.size()));
        int gas = rand.nextInt(200);
        List<String> cords = Arrays.asList(rand.nextInt(500) + "", rand.nextInt(500) + "", rand.nextInt(500) + "");
        Owner own = new Owner();
        String firstName = TestConstants.names.get(rand.nextInt(TestConstants.names.size()));
        String lastName = TestConstants.names.get(rand.nextInt(TestConstants.names.size()));
        int age = rand.nextInt(100);
        own.init(firstName, lastName, age);
        c.init(type, gas, cords, own);

        tags.add(ODS.serialize("SerCar", c));

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

        Car unserCar = ODS.deserialize(ods.get("SerCar"), Car.class);

        System.out.println(unserCar.getType());
        System.out.println(unserCar.getOwner().getFirstName());
        System.out.println(System.currentTimeMillis() - time + "ms");
    }

    public static void testODS(){
        List<ObjectTag> cars = new ArrayList<>();
        Random rand = new Random();
        for(int i = 0; i < 50000; i++){
            Car c = new Car();
            String type = TestConstants.carTypes.get(rand.nextInt(TestConstants.carTypes.size()));
            int gas = rand.nextInt(200);
            List<String> cords = Arrays.asList(rand.nextInt(500) + "", rand.nextInt(500) + "", rand.nextInt(500) + "");
            Owner own = new Owner();
            String firstName = TestConstants.names.get(rand.nextInt(TestConstants.names.size()));
            String lastName = TestConstants.names.get(rand.nextInt(TestConstants.names.size()));
            int age = rand.nextInt(100);
            own.init(firstName, lastName, age);
            c.init(type, gas, cords, own);

            cars.add(c.serialize(i));
        }
        long time = System.currentTimeMillis();
        ObjectDataStructure ods = new ObjectDataStructure(new File("test.ods"), true);
        ods.save(cars);
        System.out.println("Saved ODS in " + (System.currentTimeMillis() - time) + "ms");
        time = System.currentTimeMillis();
        ObjectTag ot = ods.getObject("car300.Owner");
        System.out.println("Loaded ODS in " + (System.currentTimeMillis() - time));
    }
}
