package com.jeff_media.persistentdataserializer.tests;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.jeff_media.persistentdataserializer.PersistentDataSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class PersistentDataSerializerTest {

    private ServerMock serverMock;
    private Player player;
    @BeforeEach
    public void setup() {
        serverMock = MockBukkit.mock();
        player = serverMock.addPlayer();
    }

    @Test
    public void testPdc() {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        PersistentDataContainer pdc1 = pdc.getAdapterContext().newPersistentDataContainer();
        PersistentDataContainer pdc2 = pdc.getAdapterContext().newPersistentDataContainer();
        PersistentDataContainer pdc3 = pdc.getAdapterContext().newPersistentDataContainer();
        pdc.set(NamespacedKey.fromString("test:name"), PersistentDataType.STRING, "mfnalex");
        pdc1.set(NamespacedKey.fromString("test:age"), PersistentDataType.INTEGER, 28);
        pdc2.set(NamespacedKey.fromString("test:height"), PersistentDataType.DOUBLE, 1.85);
        pdc3.set(NamespacedKey.fromString("test:weight"), PersistentDataType.FLOAT, 60.5f);
        pdc.set(NamespacedKey.fromString("test:pdc1"), PersistentDataType.TAG_CONTAINER, pdc1);
        pdc.set(NamespacedKey.fromString("test:pdc23"), PersistentDataType.TAG_CONTAINER_ARRAY,
                new PersistentDataContainer[] {pdc2, pdc3});
        pdc.getKeys().forEach(key -> {
            
        });
        
        List<Map<String,Object>> serialized = PersistentDataSerializer.serialize(pdc);
        PersistentDataContainer deserialized = PersistentDataSerializer.deserialize(pdc.getAdapterContext(), serialized);
        
//        deserialized.getKeys().forEach(key -> {
//            
//        });
        
        PersistentDataContainer[] d_pdc23 = deserialized.get(NamespacedKey.fromString("test:pdc23"),
                PersistentDataType.TAG_CONTAINER_ARRAY);
        
        Assertions.assertNotNull(d_pdc23);
        
        PersistentDataContainer d_pdc3 = d_pdc23[1];
        
        Assertions.assertEquals( new Float(60.5f), d_pdc3.get(NamespacedKey.fromString("test:weight"),
            PersistentDataType.FLOAT));
    }
}
