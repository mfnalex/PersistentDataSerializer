Allows to serialize and deserialize Bukkit's PersistentDataContainers.

## Maven
#### Repository
```xml
<repository>
  <id>jeff-media-public</id>
  <url>https://repo.jeff-media.com/public</url>
</repository>
```
#### Dependency
```xml
<dependency>
  <groupId>com.jeff_media</groupId>
  <artifactId>PersistentDataSerializer</artifactId>
  <version>1.0-SNAPSHOT</version>
  <scope>compile</scope>
</dependency>
```
#### Shading
You must also shade the dependency into your plugin's .jar. (https://blog.jeff-media.com/common-maven-questions/#how-to-shade-dependencies)[Click here if you don't know how to do] that.

## Usage
```java
// Serialize to Map
List<Map<?,?>> map = PersistentDataSerializer.toMap(persistentDataContainer);

// Serialize to Json
String json = PersistentDataSerializer.toJson(persistentDataContainer);

// Deserialize from Map
PersistentDataContainer pdc = PersistentDataSerializer.fromMap(pdcContext, map);

// Deserialize from Json
PersistentDataContainer pdc = PersistentDataSerializer.fromJson(pdcContext, json);
```

Note: To deserialize, you need to provide a PersistentDataAdapterContext. You can get one from any existing PersistentDataContainer by calling (https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/persistence/PersistentDataContainer.html#getAdapterContext())[PersistentDataContainer#getAdapterContext()]
