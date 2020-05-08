package me.ryandw11.ods.tests.json;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.ryandw11.ods.tests.Car;
import me.ryandw11.ods.tests.Owner;
import me.ryandw11.ods.tests.TestConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class JsonTests {
    public static void testJson() throws IOException {
        List<Car> cars = new ArrayList<>();
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

            cars.add(c);
        }
        long time = System.currentTimeMillis();

        Gson gson = new Gson();
        String json = gson.toJson(cars);
        File f = new File("test.json");
        if(!f.exists()) f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(json.getBytes());
        fos.close();
        System.out.println("GSON Save in " + (System.currentTimeMillis() - time) + "ms");

        time = System.currentTimeMillis();
        FileInputStream fis = new FileInputStream(f);
        String data = new String(fis.readAllBytes());
        fis.close();
        Type listType = new TypeToken<List<Car>>(){}.getType();
        List<Car> retrievedCars = gson.fromJson(data, listType);

        retrievedCars.get(300).getOwner();

        System.out.println("GSON Load in " + (System.currentTimeMillis() - time) + "ms");

    }
}
