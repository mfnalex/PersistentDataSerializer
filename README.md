# PersistentDataSerializer
<!--- Buttons start -->
<p align="center">
  <a href="https://repo.jeff-media.com/javadoc/public/com/jeff_media/PersistentDataSerializer/1.0-SNAPSHOT">
    <img src="https://static.jeff-media.com/img/button_javadocs.png?3" alt="Javadocs">
  </a>
  <a href="https://discord.jeff-media.com/">
    <img src="https://static.jeff-media.com/img/button_discord.png?3" alt="Discord">
  </a>
  <a href="https://paypal.me/mfnalex">
    <img src="https://static.jeff-media.com/img/button_donate.png?3" alt="Donate">
  </a>
</p>
<!--- Buttons end -->

Allows to serialize and deserialize Bukkit's PersistentDataContainers to and from `List<Map<?,?>>`s and Json, without requiring NMS.

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
You must also shade the dependency into your plugin's .jar. [Click here if you don't know how to do](https://blog.jeff-media.com/common-maven-questions/#how-to-shade-dependencies) that.

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

Note: To deserialize, you need to provide a `PersistentDataAdapterContext`. You can get one from any existing PersistentDataContainer by calling [PersistentDataContainer#getAdapterContext()](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/persistence/PersistentDataContainer.html#getAdapterContext())

## Information about MockBukkit
If you're using MockBukkit, you'll notice that serializing to Json or YAML causes issues as the MockBukkit implementation of PersistentDataContainer is buggy. It will work fine on the actual server though.