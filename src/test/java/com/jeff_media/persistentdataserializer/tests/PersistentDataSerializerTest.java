package com.jeff_media.persistentdataserializer.tests;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.jeff_media.persistentdataserializer.PersistentDataSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class PersistentDataSerializerTest {

    private ServerMock serverMock;
    private Player player;
    private PersistentDataContainer pdc, pdc1, pdc2, pdc3;

    private NamespacedKey key(String key) {
        return NamespacedKey.fromString("test:" + key);
    }

    @BeforeEach
    public void setup() {
        serverMock = MockBukkit.mock();
        player = serverMock.addPlayer();

        pdc = player.getPersistentDataContainer().getAdapterContext().newPersistentDataContainer();
        pdc1 = pdc.getAdapterContext().newPersistentDataContainer();
        pdc2 = pdc.getAdapterContext().newPersistentDataContainer();
        pdc3 = pdc.getAdapterContext().newPersistentDataContainer();
        pdc.set(key("name"), DT.STRING, "mfnalex");
        pdc.set(key("int_array"), DT.INTEGER_ARRAY, new int[] {1,2,3,4,5});
        pdc.set(key("long_array"), DT.LONG_ARRAY, new long[] {100000,200000,300000,400000,500000});
        pdc1.set(key("age"), DT.INTEGER, 28);
        pdc2.set(key("height"), DT.DOUBLE, 1.85);
        pdc2.set(key("heightx2"), DT.DOUBLE, 1.85*2);
        pdc3.set(key("weight"), DT.FLOAT, 60.5f);
        pdc.set(key("pdc1"), DT.TAG_CONTAINER, pdc1);
        pdc.set(key("pdc23"), DT.TAG_CONTAINER_ARRAY,
                new PersistentDataContainer[] {pdc2, pdc3});
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testMap() {
        List<Map<?,?>> serialized = PersistentDataSerializer.toMap(pdc);
        PersistentDataContainer deserialized = PersistentDataSerializer.fromMap(pdc.getAdapterContext(), serialized);


        PersistentDataContainer[] d_pdc23 = deserialized.get(key("pdc23"),
                DT.TAG_CONTAINER_ARRAY);

        Assertions.assertNotNull(d_pdc23);

        PersistentDataContainer d_pdc3 = d_pdc23[1];

        Assertions.assertEquals((Float) 60.5f, d_pdc3.get(key("weight"),
            DT.FLOAT));

        Assertions.assertArrayEquals(new long[] {100000,200000,300000,400000,500000}, deserialized.get(key("long_array"), DT.LONG_ARRAY));
    }

    @Test
    public void testYaml() throws InvalidConfigurationException {
        List<Map<?,?>> serialized = PersistentDataSerializer.toMap(pdc);
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("pdc", serialized);
        String serializedYaml = yaml.saveToString();
        //System.out.println(serializedYaml);
        YamlConfiguration deserializedYaml = new YamlConfiguration();
        deserializedYaml.loadFromString(serializedYaml);
        List<Map<?,?>> deserialized = deserializedYaml.getMapList("pdc");
        PersistentDataContainer pdc = PersistentDataSerializer.fromMap(this.pdc.getAdapterContext(), deserialized);
        List<Map<?,?>> serialized2 = PersistentDataSerializer.toMap(pdc);
        YamlConfiguration yaml2 = new YamlConfiguration();
        yaml2.set("pdc", serialized2);
        String serializedYaml2 = yaml2.saveToString();
        Assertions.assertEquals(serializedYaml, serializedYaml2);
    }

    @Test
    public void testJson() {
        String serialized = PersistentDataSerializer.toJson(pdc);
        //System.out.println(serialized);
        PersistentDataContainer deserialized = PersistentDataSerializer.fromJson(pdc.getAdapterContext(), serialized);
        String serialized2 = PersistentDataSerializer.toJson(deserialized);
        //System.out.println(serialized2);
        Assertions.assertEquals(serialized, serialized2);
    }

    private interface DT extends PersistentDataType { }
}
