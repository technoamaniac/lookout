package dev.aman.lookout.delivery;

import dev.aman.lookout.event.JsonFields;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/** Saves and restores a {@link Message} as JSON, for the files that keep deliveries across restarts. */
final class MessageJson {
    private MessageJson() {}

    // Fields are kept as [name, encoded value] pairs: a JSON object would lose their order, and the values are
    // already encoded
    static JSONObject toJson(Message message) throws JSONException {
        JSONArray fields = new JSONArray();
        for (Map.Entry<String, String> field : message.fields().encoded().entrySet()) {
            fields.put(new JSONArray().put(field.getKey()).put(field.getValue()));
        }
        return new JSONObject()
                .put("id", message.id())
                .put("key", message.supersessionKey())
                .put("summary", message.summary())
                .put("occurred_at", message.occurredAtMillis())
                .put("fields", fields);
    }

    static Message fromJson(JSONObject json) throws JSONException {
        JsonFields fields = new JsonFields();
        JSONArray pairs = json.getJSONArray("fields");
        for (int i = 0; i < pairs.length(); i++) {
            JSONArray pair = pairs.getJSONArray(i);
            fields.encoded(pair.getString(0), pair.getString(1));
        }
        return new Message(json.getString("id"), json.getString("key"), json.getString("summary"), fields,
                json.getLong("occurred_at"));
    }
}
