package org.mvel2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mvel2.util.ArgsRepackUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TbJson {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String stringify(Object value) {
        if (value != null) {
            try {
                return value != null ? OBJECT_MAPPER.writeValueAsString(value) : null;
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("The given Json object value: "
                        + value + " cannot be transformed to a String", e);
            }
        } else {
            return "null";
        }
    }

    public static Object parse(String value, ExecutionContext ctx) throws IOException {
        if (value != null) {
            JsonNode node = toJsonNode(value);
            if (node.isObject()) {
                return ArgsRepackUtil.repack(ctx, convertValue(node, Map.class));
            } else if (node.isArray()) {
                return ArgsRepackUtil.repack(ctx, convertValue(node, List.class));
            } else if (node.isDouble()) {
                return node.doubleValue();
            } else if (node.isLong()) {
                return node.longValue();
            } else if (node.isInt()) {
                return node.intValue();
            } else if (node.isBoolean()) {
                return node.booleanValue();
            } else if (node.isTextual()) {
                return node.asText();
            } else if (node.isBinary()) {
                return node.binaryValue();
            } else if (node.isNull()) {
                return null;
            } else {
                return node.asText();
            }
        } else {
            return null;
        }
    }

    private static JsonNode toJsonNode(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(value);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        try {
            return fromValue != null ? OBJECT_MAPPER.convertValue(fromValue, toValueType) : null;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("The given object value: "
                    + fromValue + " cannot be converted to " + toValueType, e);
        }
    }

}
