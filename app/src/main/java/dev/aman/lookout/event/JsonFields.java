package dev.aman.lookout.event;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/** Builds a flat JSON object, keeping fields in the order they were added. */
public final class JsonFields {
    // Values are already JSON-encoded
    private final Map<String, String> fields = new LinkedHashMap<>();

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

    /** Adds a nested object; it stays on one line in {@link #pretty()}. */
    public JsonFields object(String name, JsonFields object) {
        fields.put(name, object.compact());
        return this;
    }

    /** Adds an array of objects; nested objects stay on one line in {@link #pretty()}. */
    public JsonFields objects(String name, List<JsonFields> objects) {
        StringJoiner array = new StringJoiner(",", "[", "]");
        for (JsonFields object : objects) array.add(object.compact());
        fields.put(name, array.toString());
        return this;
    }

    /**
     * Adds a value that is already JSON-encoded, as returned by {@link #encoded()}; for restoring saved fields, so
     * nothing has to be decoded and re-encoded.
     */
    public JsonFields encoded(String name, String json) {
        fields.put(name, json);
        return this;
    }

    /** Each field's name and JSON-encoded value, in order. */
    public Map<String, String> encoded() {
        return Collections.unmodifiableMap(fields);
    }

    public JsonFields copy() {
        JsonFields copy = new JsonFields();
        copy.fields.putAll(fields);
        return copy;
    }

    public String compact() {
        return render("", "", ":");
    }

    public String pretty() {
        return render("\n", "  ", ": ");
    }

    private String render(String newline, String indent, String colon) {
        StringJoiner json = new StringJoiner("," + newline, "{" + newline, newline + "}");
        for (Map.Entry<String, String> field : fields.entrySet()) {
            json.add(indent + quote(field.getKey()) + colon + field.getValue());
        }
        return json.toString();
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
}
