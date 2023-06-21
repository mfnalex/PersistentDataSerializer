/*
 * Copyright (c) 2023. JEFF Media GbR / mfnalex et al.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.jeff_media.persistentdataserializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class to serialize and deserialize for {@link PersistentDataContainer}s
 */
public final class PersistentDataSerializer {

    /**
     * All native primitive {@link PersistentDataType}s declared in {@link PersistentDataType}
     */
    private static final Set<PersistentDataType<?,?>> NATIVE_PRIMITIVE_PERSISTENT_DATA_TYPES = new HashSet<>();

    /**
     * All native primitive {@link PersistentDataType}s mapped by their name
     */
    private static final Map<String, PersistentDataType<?,?>> NATIVE_PRIMITIVE_PERSISTENT_DATA_TYPES_BY_NAME = new HashMap<>();

    /**
     * All native primitive {@link PersistentDataType} mapped by their class
     */
    private static final Map<PersistentDataType<?,?>, String> NATIVE_PRIMITIVE_PERSISTENT_DATA_TYPES_BY_CLASS = new HashMap<>();

    /**
     * TypeToken for {@link List} of {@link Map}s with {@link String} keys and {@link Object} values
     */
    private static final TypeToken<List<Map<String, Object>>> LIST_MAP_TYPE_TOKEN = new TypeToken<List<Map<String, Object>>>() {
    };

    private static final Gson GSON = new GsonBuilder().create();

    // Cache the native primitive PersistentDataTypes
    static {

        for (final Field field : PersistentDataType.class.getFields()) {

            // Ignore non-PersistentDataType fields
            if (field.getType() != PersistentDataType.class) {
                continue;
            }

            try {
                final PersistentDataType<?, ?> type = Objects.requireNonNull((PersistentDataType<?, ?>) field.get(null));

                // Ignore non-primitive PersistentDataTypes, such as PersistentDataType.BOOLEAN
                if (!(type instanceof PersistentDataType.PrimitivePersistentDataType)) {
                    continue;
                }

                final String name = field.getName();

                NATIVE_PRIMITIVE_PERSISTENT_DATA_TYPES.add(type);
                NATIVE_PRIMITIVE_PERSISTENT_DATA_TYPES_BY_NAME.put(name, type);
                NATIVE_PRIMITIVE_PERSISTENT_DATA_TYPES_BY_CLASS.put(type, name);
            } catch (final IllegalAccessException exception) {
                throw new RuntimeException("Could not access native persistent data type field: " + field.getType().getName(), exception);
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
     * Gets the proper {@link org.bukkit.persistence.PersistentDataType.PrimitivePersistentDataType} for the given {@link NamespacedKey}
     *
     * @param pdc PersistentDataContainer
     * @param key NamespacedKey
     * @return PrimitivePersistentDataType
     * @throws IllegalArgumentException if no native PrimitivePersistentDataType was found. (This should never happen.)
     */
    public static PersistentDataType<?,?> getPrimitivePersistentDataType(
            @NotNull final PersistentDataContainer pdc,
            @NotNull final NamespacedKey key
    ) throws IllegalArgumentException {

        Objects.requireNonNull(pdc, "pdc cannot be null");
        Objects.requireNonNull(key, "key cannot be null");

        for (final PersistentDataType<?,?> type : NATIVE_PRIMITIVE_PERSISTENT_DATA_TYPES) {
            if (pdc.has(key, type)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Could not find a native PrimitivePersistentDataType for key " + key.toString() +
                " in PersistentDataContainer " + pdc.toString() + ". Available native datatypes are " + String.join(", ", NATIVE_PRIMITIVE_PERSISTENT_DATA_TYPES_BY_NAME.keySet()));
    }

    /**
     * Serializes a {@link PersistentDataContainer} to a list of maps
     *
     * @param pdc PersistentDataContainer
     * @return serialized PersistentDataContainer
     */
    @NotNull
    public static List<Map<?, ?>> toMapList(
            @NotNull final PersistentDataContainer pdc
    ) {

        Objects.requireNonNull(pdc, "pdc cannot be null");
        final List<Map<?, ?>> list = new ArrayList<>();

        for (final NamespacedKey key : pdc.getKeys()) {

            final Map<String, Object> map = new LinkedHashMap<>();
            final PersistentDataType<?, ?> type = getPrimitivePersistentDataType(pdc, key);
            Object value = pdc.get(key, type);
            Objects.requireNonNull(value, "value cannot be null");

            if (type.equals(PersistentDataType.TAG_CONTAINER)) {
                value = toMapList((PersistentDataContainer) value);
            } else if (type.equals(PersistentDataType.TAG_CONTAINER_ARRAY)) {
                final PersistentDataContainer[] containers = (PersistentDataContainer[]) value;
                Objects.requireNonNull(containers, "containers cannot be null");
                final List<List<Map<?, ?>>> serializedContainers = new ArrayList<>();
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
     *
     * @param context       PersistentDataAdapterContext
     * @param serializedPdc serialized PersistentDataContainer
     * @return deserialized PersistentDataContainer
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static PersistentDataContainer fromMapList(
            @NotNull final PersistentDataAdapterContext context,
            @NotNull final List<Map<?, ?>> serializedPdc
    ) {

        Objects.requireNonNull(context, "context cannot be null");
        Objects.requireNonNull(serializedPdc, "serializedPdc cannot be null");

        final PersistentDataContainer pdc = context.newPersistentDataContainer();
        for (final Map<?, ?> map : serializedPdc) {
            final NamespacedKey key = NamespacedKey.fromString((String) map.get("key"));

            Objects.requireNonNull(key, "key cannot be null");
            Object value = map.get("value");

            final PersistentDataType<Object, Object> type =
                    (PersistentDataType<Object, Object>) getNativePersistentDataTypeByFieldName((String) map.get("type"));

            if (type.equals(PersistentDataType.TAG_CONTAINER)) {
                value = fromMapList(context, (List<Map<?, ?>>) value);
            } else if (type.equals(PersistentDataType.TAG_CONTAINER_ARRAY)) {
                List<List<Map<?, ?>>> serializedContainers = (List<List<Map<?, ?>>>) value;
                final PersistentDataContainer[] containers = new PersistentDataContainer[serializedContainers.size()];
                for (int i = 0; i < serializedContainers.size(); i++) {
                    containers[i] = fromMapList(context, serializedContainers.get(i));
                }
                value = containers;
            } else {
                value = cast(value, type);
            }

            pdc.set(key, type, value);
        }
        
        return pdc;
    }

    private static Object cast(
            @Nullable final Object value,
            @NotNull final PersistentDataType<?, ?> type
    ) {
        
        if (value == null) {
            return null;
        }
        
        Objects.requireNonNull(type, "type cannot be null");
        final Class<?> primitiveType = type.getPrimitiveType();
        
        if (primitiveType == Float.class) {
            return ((Number) value).floatValue();
        } else if (primitiveType == Integer.class) {
            return ((Number) value).intValue();
        } else if (primitiveType == Double.class) {
            return ((Number) value).doubleValue();
        } else if (primitiveType == Short.class) {
            return ((Number) value).shortValue();
        } else if (primitiveType == Byte.class) {
            if (type.getComplexType() == Boolean.class) {
                if (value instanceof Byte) {
                    return ((Byte) value) == 1;
                } else if (value instanceof Boolean) {
                    return (Boolean) value;
                }
            } else if (value instanceof Boolean) {
                return (byte) (((Boolean) value) ? 1 : 0);
            } else if (value instanceof Number) {
                return ((Number) value).byteValue();
            }
        } else if (value instanceof List) {
            final List<?> list = (List<?>) value;
            int length = list.size();
            if (type == PersistentDataType.BYTE_ARRAY) {
                byte[] arr = new byte[length];
                for (int i = 0; i < length; i++) {
                    arr[i] = ((Number) list.get(i)).byteValue();
                }
                return arr;
            } else if (type == PersistentDataType.INTEGER_ARRAY) {
                int[] arr = new int[length];
                for (int i = 0; i < length; i++) {
                    arr[i] = ((Number) list.get(i)).intValue();
                }
                return arr;
            } else if (type == PersistentDataType.LONG_ARRAY) {
                long[] arr = new long[length];
                for (int i = 0; i < length; i++) {
                    arr[i] = ((Number) list.get(i)).longValue();
                }
                return arr;
            } else {
                throw new IllegalArgumentException("Unknown array type: " + type.getPrimitiveType().getComponentType().getName());
            }
        }
        return value;
    }

    /**
     * Serializes a {@link PersistentDataContainer} to JSON
     *
     * @param pdc PersistentDataContainer
     * @return JSON string
     */
    public static String toJson(
            @NotNull final PersistentDataContainer pdc
    ) {
        Objects.requireNonNull(pdc, "pdc cannot be null");
        return GSON.toJson(toMapList(pdc), LIST_MAP_TYPE_TOKEN.getType());
    }

    /**
     * Deserializes a {@link PersistentDataContainer} from JSON
     * @param context PersistentDataAdapterContext
     * @param json JSON string
     * @return deserialized PersistentDataContainer
     * @throws JsonSyntaxException if the JSON is malformed
     */
    public static PersistentDataContainer fromJson(
            @NotNull final PersistentDataAdapterContext context,
            @NotNull final String json
    ) throws JsonSyntaxException {
        Objects.requireNonNull(context, "context cannot be null");
        Objects.requireNonNull(json, "json cannot be null");
        return fromMapList(context, GSON.fromJson(json, LIST_MAP_TYPE_TOKEN.getType()));
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
            @NotNull final String fieldName
    ) throws IllegalArgumentException {
        Objects.requireNonNull(fieldName, "fieldName cannot be null");
        final PersistentDataType<?, ?> type = NATIVE_PRIMITIVE_PERSISTENT_DATA_TYPES_BY_NAME.get(fieldName);
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
            @NotNull final PersistentDataType<?, ?> type
    ) throws IllegalArgumentException {
        Objects.requireNonNull(type, "type cannot be null");
        final String name = NATIVE_PRIMITIVE_PERSISTENT_DATA_TYPES_BY_CLASS.get(type);
        if (name == null) {
            throw new IllegalArgumentException(
                    "Could not find native field name for PersistentDataType with " + "primitive class " +
                            type.getPrimitiveType().getName() + " and complex class " +
                            type.getComplexType().getName());
        }
        return name;
    }

}
