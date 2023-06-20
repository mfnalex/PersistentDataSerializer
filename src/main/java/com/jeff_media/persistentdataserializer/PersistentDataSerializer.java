package com.jeff_media.persistentdataserializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.NamespacedKey;
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

    private static final Set<PersistentDataType<?, ?>> NATIVE_PERSISTENT_DATA_TYPES = new HashSet<>();
    private static final Map<String, PersistentDataType<?, ?>> NATIVE_PERSISTENT_DATA_TYPES_BY_NAME = new HashMap<>();
    private static final Map<PersistentDataType<?, ?>, String> NATIVE_PERSISTENT_DATA_TYPE_NAMES_BY_CLASS =
            new HashMap<>();
    private static final TypeToken<List<Map<String,Object>>> MAP_TYPE_TOKEN = new TypeToken<List<Map<String,Object>>>() {};
    private static final Gson gson = new GsonBuilder().create();

    static {
        for (Field field : PersistentDataType.class.getFields()) {
            if (field.getType() != PersistentDataType.class) {
                continue;
            }
            try {
                PersistentDataType<?, ?> type = Objects.requireNonNull((PersistentDataType<?, ?>) field.get(null));
                if(!(type instanceof PersistentDataType.PrimitivePersistentDataType)) {
                    continue;
                }
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
                " in PersistentDataContainer " + pdc.toString() + ". Available native datatypes are " + String.join(", ", NATIVE_PERSISTENT_DATA_TYPES_BY_NAME.keySet()));
    }

    /**
     * Serializes a {@link PersistentDataContainer} to a list of maps
     * @param pdc PersistentDataContainer
     * @return serialized PersistentDataContainer
     */
    @NotNull
    public static List<Map<?, ?>> toMapList(
            @NotNull
            PersistentDataContainer pdc) {
        List<Map<?, ?>> list = new ArrayList<>();
        for (NamespacedKey key : pdc.getKeys()) {
            Map<String, Object> map = new LinkedHashMap<>();
            PersistentDataType<?, ?> type = getPersistentDataType(pdc, key);
            Object value = pdc.get(key, type);
            if (type.equals(PersistentDataType.TAG_CONTAINER)) {
                value = toMapList((PersistentDataContainer) value);
            }
            else if (type.equals(PersistentDataType.TAG_CONTAINER_ARRAY)) {
                PersistentDataContainer[] containers = (PersistentDataContainer[]) value;
                List<List<Map<?, ?>>> serializedContainers = new ArrayList<>();
                for (PersistentDataContainer container : containers) {
                    serializedContainers.add(toMapList(container));
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

    /**
     * Deserializes a {@link PersistentDataContainer} from a list of maps
     * @param context PersistentDataAdapterContext
     * @param serializedPdc serialized PersistentDataContainer
     * @return deserialized PersistentDataContainer
     */
    @NotNull
    public static PersistentDataContainer fromMapList(PersistentDataAdapterContext context,
                                                      List<Map<?, ?>> serializedPdc) {
        PersistentDataContainer pdc = context.newPersistentDataContainer();
        for (Map<?, ?> map : serializedPdc) {
            NamespacedKey key = NamespacedKey.fromString((String) map.get("key"));

            Objects.requireNonNull(key, "Key cannot be null");
            Object value = map.get("value");

            PersistentDataType<Object, Object> type =
                    (PersistentDataType<Object, Object>) getNativePersistentDataTypeByFieldName((String) map.get("type"));

            if(type.equals(PersistentDataType.TAG_CONTAINER)) {
                value = fromMapList(context, (List<Map<?, ?>>) value);
            }
            else if(type.equals(PersistentDataType.TAG_CONTAINER_ARRAY)) {
                List<List<Map<?, ?>>> serializedContainers = (List<List<Map<?, ?>>>) value;
                PersistentDataContainer[] containers = new PersistentDataContainer[serializedContainers.size()];
                for(int i=0; i<serializedContainers.size(); i++) {
                    containers[i] = fromMapList(context, serializedContainers.get(i));
                }
                value = containers;
            }
            else {
                value = cast(value, type);
            }

            pdc.set(key, type, value);

        }
        return pdc;
    }

    private static Object cast(Object value, PersistentDataType<?,?> type) {
        Class<?> primitiveType = type.getPrimitiveType();
        if(primitiveType == Float.class) {
            return ((Number) value).floatValue();
        } else if(primitiveType == Integer.class) {
            return ((Number) value).intValue();
        } else if(primitiveType == Double.class) {
            return ((Number) value).doubleValue();
        } else if(primitiveType == Short.class) {
            return ((Number) value).shortValue();
        } else if(primitiveType == Byte.class) {
            if(type.getComplexType() == Boolean.class) {
                if(value instanceof Byte) {
                    return ((Byte) value) == 1;
                } else if(value instanceof Boolean) {
                    return (Boolean) value;
                }
            } else if(value instanceof Boolean) {
                return (byte) (((Boolean) value) ? 1 : 0);
            } else if(value instanceof Number) {
                return ((Number) value).byteValue();
            }
        } else if(value instanceof List) {
            List<?> list = (List<?>) value;
            int length = list.size();
            if(type == PersistentDataType.BYTE_ARRAY) {
                byte[] arr = new byte[length];
                for(int i=0; i<length; i++) {
                    arr[i] = ((Number) list.get(i)).byteValue();
                }
                return arr;
            } else if(type == PersistentDataType.INTEGER_ARRAY) {
                int[] arr = new int[length];
                for(int i=0; i<length; i++) {
                    arr[i] = ((Number) list.get(i)).intValue();
                }
                return arr;
            } else if(type == PersistentDataType.LONG_ARRAY) {
                long[] arr = new long[length];
                for(int i=0; i<length; i++) {
                    arr[i] = ((Number) list.get(i)).longValue();
                }
                return arr;
            } else {
                throw new IllegalArgumentException("Unknown array type " + type.getPrimitiveType().getComponentType().getName());
            }
        }
        return value;
    }

    public static String toJson(PersistentDataContainer pdc) {
        return gson.toJson(toMapList(pdc), MAP_TYPE_TOKEN.getType());
    }

    public static PersistentDataContainer fromJson(PersistentDataAdapterContext context, String json) {
        return fromMapList(context, gson.fromJson(json, MAP_TYPE_TOKEN.getType()));
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
