package com.jeff_media.persistentdataserializer;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Utility class to serialize and deserialize for {@link PersistentDataContainer}s
 */
public final class PersistentDataSerializer {

    private static final char PSEP = '\000';

    private static final Set<PersistentDataType<?, ?>> NATIVE_PERSISTENT_DATA_TYPES = new HashSet<>();
    private static final Map<String, PersistentDataType<?, ?>> NATIVE_PERSISTENT_DATA_TYPES_BY_NAME = new HashMap<>();
    private static final Map<PersistentDataType<?, ?>, String> NATIVE_PERSISTENT_DATA_TYPE_NAMES_BY_CLASS =
            new HashMap<>();

    static {
        for (Field field : PersistentDataType.class.getFields()) {
            if (field.getType() != PersistentDataType.class) {
                continue;
            }
            try {
                PersistentDataType<?, ?> type = Objects.requireNonNull((PersistentDataType<?, ?>) field.get(null));
                String name = field.getName();

                NATIVE_PERSISTENT_DATA_TYPES.add(type);
                NATIVE_PERSISTENT_DATA_TYPES_BY_NAME.put(name, type);
                NATIVE_PERSISTENT_DATA_TYPE_NAMES_BY_CLASS.put(type, name);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Could not get native persistent data type: " + field.getType().getName(),
                        e);
            }
        }
    }

    /**
     * Private constructor to prevent instantiation
     */
    private PersistentDataSerializer() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Gets the proper {@link PersistentDataType} for the given {@link NamespacedKey}
     *
     * @param pdc PersistentDataContainer
     * @param key NamespacedKey
     * @return PersistentDataType
     * @throws IllegalArgumentException if no native PersistentDataType was found. (This should never happen.)
     */
    public static PersistentDataType<?, ?> getPersistentDataType(
            @NotNull
            PersistentDataContainer pdc,
            @NotNull
            NamespacedKey key) throws IllegalArgumentException {
        Objects.requireNonNull(pdc, "PersistentDataContainer cannot be null");
        Objects.requireNonNull(key, "NamespacedKey cannot be null");
        for (PersistentDataType<?, ?> type : NATIVE_PERSISTENT_DATA_TYPES) {
            if (pdc.has(key, type)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Could not find a native PersistentDataType for key " + key.toString() +
                " in PersistentDataContainer " + pdc.toString());
    }

    private static YamlConfiguration createYaml() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().pathSeparator(PSEP);
        return yaml;
    }

    @NotNull
    public static List<Map<String, Object>> serialize(
            @NotNull
            PersistentDataContainer pdc) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (NamespacedKey key : pdc.getKeys()) {
            Map<String, Object> map = new LinkedHashMap<>();
            PersistentDataType<?, ?> type = getPersistentDataType(pdc, key);
            Object value = pdc.get(key, type);
            if (type.equals(PersistentDataType.TAG_CONTAINER)) {
                value = serialize((PersistentDataContainer) value);
            }
            else if (type.equals(PersistentDataType.TAG_CONTAINER_ARRAY)) {
                PersistentDataContainer[] containers = (PersistentDataContainer[]) value;
                List<List<Map<String, Object>>> serializedContainers = new ArrayList<>();
                for (PersistentDataContainer container : containers) {
                    serializedContainers.add(serialize(container));
                }
                value = serializedContainers;
            }
            map.put("key", key.toString());
            map.put("type", getNativePersistentDataTypeFieldName(type));
            map.put("value", value);
            list.add(map);
        }
        return list;
    }

    @NotNull
    public static PersistentDataContainer deserialize(PersistentDataAdapterContext context,
                                                      List<Map<String, Object>> serializedPdc) {
        PersistentDataContainer pdc = context.newPersistentDataContainer();
        for (Map<String, Object> map : serializedPdc) {
            NamespacedKey key = NamespacedKey.fromString((String) map.get("key"));
            
            Objects.requireNonNull(key, "Key cannot be null");
            PersistentDataType<Object, Object> type =
                    (PersistentDataType<Object, Object>) getNativePersistentDataTypeByFieldName((String) map.get("type"));
            
            
            Object value = map.get("value");
            if(type.equals(PersistentDataType.TAG_CONTAINER)) {
                value = deserialize(context, (List<Map<String, Object>>) value);
            }
            else if(type.equals(PersistentDataType.TAG_CONTAINER_ARRAY)) {
                List<List<Map<String, Object>>> serializedContainers = (List<List<Map<String, Object>>>) value;
                PersistentDataContainer[] containers = new PersistentDataContainer[serializedContainers.size()];
                for(int i=0; i<serializedContainers.size(); i++) {
                    containers[i] = deserialize(context, serializedContainers.get(i));
                }
                value = containers;
            }
            
            pdc.set(key, type, value);
            
        }
        return pdc;
    }

    /**
     * Gets a native {@link PersistentDataType} by its field name, e.g. "STRING" or "BYTE_ARRAY" (case-sensitive)
     *
     * @param fieldName field name
     * @return native PersistentDataType
     * @throws IllegalArgumentException if no native PersistentDataType was found with the given field name
     */
    @NotNull
    public static PersistentDataType<?, ?> getNativePersistentDataTypeByFieldName(
            @NotNull
            String fieldName) {
        Objects.requireNonNull(fieldName, "Field name cannot be null");
        PersistentDataType<?, ?> type = NATIVE_PERSISTENT_DATA_TYPES_BY_NAME.get(fieldName);
        if (type == null) {
            throw new IllegalArgumentException("Could not find native PersistentDataType with field name " + fieldName);
        }
        return type;
    }

    /**
     * Gets the field name for the given native {@link PersistentDataType}
     *
     * @param type native PersistentDataType
     * @return field name
     * @throws IllegalArgumentException if the given PersistentDataType is not native and therefore does not have a field name
     */
    @NotNull
    public static String getNativePersistentDataTypeFieldName(
            @NotNull
            PersistentDataType<?, ?> type) {
        Objects.requireNonNull(type, "PersistentDataType cannot be null");
        String name = NATIVE_PERSISTENT_DATA_TYPE_NAMES_BY_CLASS.get(type);
        if (name == null) {
            throw new IllegalArgumentException(
                    "Could not find native field name for PersistentDataType with " + "primitive class " +
                            type.getPrimitiveType().getName() + " and complex class " +
                            type.getComplexType().getName());
        }
        return name;
    }

}
