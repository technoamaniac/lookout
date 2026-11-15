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

    private final AtomicTextFile file;

    public OutboxFile(File file) {
        this.file = new AtomicTextFile(file);
    }

    // An unreadable or unknown file starts an empty outbox rather than stopping delivery; the next save replaces it
    @Override
    public Outbox.Snapshot load() {
        try {
            Optional<String> text = file.read();
            if (!text.isPresent()) return Outbox.Snapshot.EMPTY;
            JSONObject json = new JSONObject(text.get());
            if (json.optInt("version") != VERSION) return Outbox.Snapshot.EMPTY;
            List<Outbox.Item> items = new ArrayList<>();
            JSONArray saved = json.getJSONArray("items");
            for (int i = 0; i < saved.length(); i++) {
                JSONObject item = saved.getJSONObject(i);
                items.add(new Outbox.Item(MessageJson.fromJson(item.getJSONObject("message")),
                        item.getBoolean("settled")));
            }
            return new Outbox.Snapshot(json.has("webhook") ? json.getString("webhook") : null, items);
        } catch (IOException | JSONException e) {
            return Outbox.Snapshot.EMPTY;
        }
    }

    @Override
    public void save(Outbox.Snapshot snapshot) {
        try {
            JSONArray items = new JSONArray();
            for (Outbox.Item item : snapshot.items()) {
                items.put(new JSONObject()
                        .put("message", MessageJson.toJson(item.message()))
                        .put("settled", item.settled()));
            }
            JSONObject json = new JSONObject().put("version", VERSION).put("items", items);
            if (snapshot.webhook() != null) json.put("webhook", snapshot.webhook());
            file.write(json.toString());
        } catch (IOException | JSONException e) {
            // Delivery carries on from memory; the previous file stays whole
        }
    }
}
