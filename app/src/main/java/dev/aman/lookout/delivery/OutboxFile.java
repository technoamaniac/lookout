package dev.aman.lookout.delivery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Keeps the outbox in a JSON file in the app's private storage. */
public final class OutboxFile implements OutboxStore {
    private static final int VERSION = 1;

    private final JsonFile file;

    public OutboxFile(File file) {
        this.file = new JsonFile(file, VERSION);
    }

    // An unreadable file starts an empty outbox rather than stopping delivery; the next save replaces it
    @Override
    public Outbox.Snapshot load() {
        Optional<JSONObject> json = file.read();
        if (!json.isPresent()) return Outbox.Snapshot.EMPTY;
        try {
            return decode(json.get());
        } catch (JSONException e) {
            return Outbox.Snapshot.EMPTY;
        }
    }

    @Override
    public void save(Outbox.Snapshot snapshot) {
        try {
            file.write(encode(snapshot));
        } catch (IOException | JSONException e) {
            // Delivery carries on from memory; the previous file stays whole
        }
    }

    private static JSONObject encode(Outbox.Snapshot snapshot) throws JSONException {
        JSONArray items = new JSONArray();
        for (Outbox.Item item : snapshot.items()) {
            items.put(new JSONObject()
                    .put("message", MessageJson.toJson(item.message()))
                    .put("settled", item.settled()));
        }
        // A null webhook leaves the key out
        return new JSONObject().put("webhook", snapshot.webhook()).put("items", items);
    }

    private static Outbox.Snapshot decode(JSONObject json) throws JSONException {
        List<Outbox.Item> items = new ArrayList<>();
        JSONArray saved = json.getJSONArray("items");
        for (int i = 0; i < saved.length(); i++) {
            JSONObject item = saved.getJSONObject(i);
            items.add(new Outbox.Item(MessageJson.fromJson(item.getJSONObject("message")),
                    item.getBoolean("settled")));
        }
        return new Outbox.Snapshot(json.has("webhook") ? json.getString("webhook") : null, items);
    }
}
