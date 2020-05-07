# Object Data Structure (ODS)
Object Data Structure is a file format inspired by NBT. Everything in this file format is made of tags.  
ODS is not human readable, data is stored in bytes.

## Specification
Each tag has a header consisting of 7 bytes.
<html>
<table>
<tr>
<th>
1 Byte
</th>
<th>
4 Bytes
</th>
<th> 2 Bytes </th>
</tr>
<tr>
<td>The Data Type</td>
<td>The size of the tag (in bytes) starting from the next byte.</td>
<td>The number of bytes the key has.</td>
</tr>
</table>
</html>
Each tag has a key. There can only be one tag with a ceratin key.

## Data Types
<html>
<table>
<tr><th>Byte</th><th>Type</th></tr>
<tr><td>0</th><td>Reserved</td></tr>
<tr><td>1</td><td>String</td></tr>
<tr><td>2</td><td>int</td></tr>
<tr><td>3</td><td>Float</td></tr>
<tr><td>4</td><td>Double</td></tr>
<tr><td>5</td><td>Short</td></tr>
<tr><td>6</td><td>Long</td></tr>
<tr><td>7</td><td>Char</td></tr>
<tr><td>8</td><td>Byte</td></tr>
<tr><td>9</td><td>List</td></tr>
<tr><td>10</td><td>Map</td></tr>
<tr><td>11</td><td>Object</td></tr>
</table>
</html>

# Example for saving
```java
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
```

# Example for loading
```java
        StringTag tag = ods.get("ExampleKey");
        System.out.println("The value of ExampleKey is: " + tag.getValue());

        ObjectTag myCar = ods.get("Car");
        System.out.println("The car is a " + myCar.getTag("type").getValue());

        StringTag ownerFirstName = ods.getObject("Car.Owner.firstName");
        StringTag ownerLastName = ods.getObject("Car.Owner.lastName");
        System.out.println("The owner of the car is " + ODS.unwrap(ownerFirstName) + " " + ODS.unwrap(ownerLastName));
```
