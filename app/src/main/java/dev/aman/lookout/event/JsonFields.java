package dev.aman.lookout.event;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds a JSON object, keeping fields in the order they were added. Nested objects and arrays are copied in. */
public final class JsonFields {
    // Each value is an already-encoded scalar (String), a nested object (JsonFields) or an array of them (Array)
    private final Map<String, Object> fields = new LinkedHashMap<>();

    public JsonFields string(String name, String value) {
        fields.put(name, quote(value));
        return this;
    }

    /** Leaves the field out when the value is null or blank, rather than sending null. */
    public JsonFields optionalString(String name, String value) {
        return value == null || value.trim().isEmpty() ? this : string(name, value);
    }

    public JsonFields number(String name, long value) {
        fields.put(name, Long.toString(value));
        return this;
    }

    public JsonFields bool(String name, boolean value) {
        fields.put(name, Boolean.toString(value));
        return this;
    }

    /** Adds a nested object. */
    public JsonFields object(String name, JsonFields object) {
        fields.put(name, object.copy());
        return this;
    }

    /** Adds an array of objects. */
    public JsonFields objects(String name, List<JsonFields> objects) {
        fields.put(name, new Array(objects));
        return this;
    }

    public JsonFields copy() {
        JsonFields copy = new JsonFields();
        copy.fields.putAll(fields);
        return copy;
    }

    public String compact() {
        return write(new StringBuilder(), false, 0).toString();
    }

    /** The same JSON with one value per line, nested objects and arrays indented. */
    public String pretty() {
        return write(new StringBuilder(), true, 0).toString();
    }

    private StringBuilder write(StringBuilder out, boolean pretty, int depth) {
        if (fields.isEmpty()) return out.append("{}");
        out.append('{');
        String separator = "";
        for (Map.Entry<String, Object> field : fields.entrySet()) {
            out.append(separator);
            separator = ",";
            newline(out, pretty, depth + 1);
            out.append(quote(field.getKey())).append(pretty ? ": " : ":");
            writeValue(out, field.getValue(), pretty, depth + 1);
        }
        newline(out, pretty, depth);
        return out.append('}');
    }

    private static void writeValue(StringBuilder out, Object value, boolean pretty, int depth) {
        if (value instanceof JsonFields) {
            ((JsonFields) value).write(out, pretty, depth);
        } else if (value instanceof Array) {
            ((Array) value).write(out, pretty, depth);
        } else {
            out.append((String) value);
        }
    }

    private static void newline(StringBuilder out, boolean pretty, int depth) {
        if (!pretty) return;
        out.append('\n');
        for (int i = 0; i < depth; i++) out.append("  ");
    }

    private static String quote(String s) {
        StringBuilder out = new StringBuilder(s.length() + 2).append('"');
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c < 0x20) {
                out.append(String.format("\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.append('"').toString();
    }

    /** An array of objects, copied so later changes to the originals do not reach it. */
    private static final class Array {
        private final List<JsonFields> items = new ArrayList<>();

        Array(List<JsonFields> objects) {
            for (JsonFields object : objects) items.add(object.copy());
        }

        void write(StringBuilder out, boolean pretty, int depth) {
            if (items.isEmpty()) {
                out.append("[]");
                return;
            }
            out.append('[');
            String separator = "";
            for (JsonFields item : items) {
                out.append(separator);
                separator = ",";
                newline(out, pretty, depth + 1);
                item.write(out, pretty, depth + 1);
            }
            newline(out, pretty, depth);
            out.append(']');
        }
    }
}
