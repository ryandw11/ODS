# Object Data Structure (ODS)
Object Data Structure is a file format inspired by NBT. Everything in this file format is made of tags.  
ODS is not human readable, data is stored in bytes.  
  
## [JavaDocs](https://ryandw11.github.io/ODS/)  
  
Access release builds of Object Data Structure by using the following methods:  
Maven: 
```xml
<repositories>
    <repository>
        <id>ObjectDataStructure</id>
        <url>https://repo.ryandw11.com/repository/maven-releases/</url>
    </repository>
</repositories>

<dependency>
    <groupId>me.ryandw11</groupId>
    <artifactId>ods</artifactId>
    <version>1.0.0</version>
</dependency>
```
Gradle:  
```gradle
repositories {
    maven { url 'https://repo.ryandw11.com/repository/maven-releases/' }
}
    
dependencies {
    implementation 'me.ryandw11:ods:1.0.0'
}
```
Fat Jar:  
[Download the latest fat jar here](https://github.com/ryandw11/ODS/releases)
# Usage
As stated above ODS uses tags. There are many primative tags: StringTag, IntTag, ShortTag, LongTag, ByteTag, DoubleTag, FloatTag.  
There are also the ListTag and MapTag. They both store primative tags in a list and map format respectivly.  
Finally there are ObjectTags. ObjectTags store other tags. For more information about the possibilites be sure to check out the wiki and javadoc!
## ODS Utility Class
The ODS class is full of useful methods that allow the easy serialization of primative objects.  
The ODS class also allows the serialization of custom objects.
## Example for saving
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

## Example for loading
```java
        System.out.println("The value of ExampleKey is: " + ods.<StringTag>get("ExampleKey").getValue());

        ObjectTag myCar = ods.get("Car");
        System.out.println("The car is a " + myCar.getTag("type").getValue());

        StringTag ownerFirstName = ods.get("Car.Owner.firstName");
        StringTag ownerLastName = ods.get("Car.Owner.lastName");
        System.out.println("The owner of the car is " + ODS.unwrap(ownerFirstName) + " " + ODS.unwrap(ownerLastName));
```
# ODS Visualizer
This tool allows you inspect ods files. 
![Picture Of the Visualizer](https://i.imgur.com/ukROPZy.png)  
[Click here to go to the visualizer repository.](https://github.com/ryandw11/ODS_Visualizer)
# Offical Language Ports
 - C++ (Coming Soon)
 - [ODSSharp (C#)](https://github.com/ryandw11/ODSSharp)
 
# Porting to another language.
To port this to another language you just need to follow the file format specifications. The specifications for the ODS file type can be found below. Test ODS files can be found below. The Visualizer tool might be helpful.
## Existing Ports
 - There are none! To add your port to this list open an issue or create a PR!

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

## Compression Types
 - None
 - GZIP
 - ZLIB
