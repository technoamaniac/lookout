package dev.aman.lookout.event;

import java.util.LinkedHashMap;
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
