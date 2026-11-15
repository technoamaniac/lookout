package dev.aman.lookout.delivery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Keeps the delivery log in a JSON file in the app's private storage. */
public final class DeliveryLogFile implements DeliveryLogStore {
    private static final int VERSION = 1;

    private final AtomicTextFile file;

    public DeliveryLogFile(File file) {
        this.file = new AtomicTextFile(file);
    }

    // An unreadable file, or a result this version does not know, starts an empty log; the next save replaces it
    @Override
    public List<DeliveryRecord> load() {
        try {
            Optional<String> text = file.read();
            if (!text.isPresent()) return List.of();
            JSONObject json = new JSONObject(text.get());
            if (json.optInt("version") != VERSION) return List.of();
            List<DeliveryRecord> records = new ArrayList<>();
            JSONArray saved = json.getJSONArray("records");
            for (int i = 0; i < saved.length(); i++) {
                JSONObject record = saved.getJSONObject(i);
                records.add(new DeliveryRecord(MessageJson.fromJson(record.getJSONObject("message")),
                        DeliveryResult.valueOf(record.getString("result")), record.getLong("attempted_at")));
            }
            return records;
        } catch (IOException | JSONException | IllegalArgumentException e) {
            return List.of();
        }
    }

    @Override
    public void save(List<DeliveryRecord> newestFirst) {
        try {
            JSONArray records = new JSONArray();
            for (DeliveryRecord record : newestFirst) {
                records.put(new JSONObject()
                        .put("message", MessageJson.toJson(record.message()))
                        .put("result", record.result().name())
                        .put("attempted_at", record.attemptedAtMillis()));
            }
            file.write(new JSONObject().put("version", VERSION).put("records", records).toString());
        } catch (IOException | JSONException e) {
            // The log is still shown from memory; the previous file stays whole
        }
    }
}
