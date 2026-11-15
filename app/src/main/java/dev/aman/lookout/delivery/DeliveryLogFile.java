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

    private final JsonFile file;

    public DeliveryLogFile(File file) {
        this.file = new JsonFile(file, VERSION);
    }

    // An unreadable file, or a result this version does not know, starts an empty log; the next save replaces it
    @Override
    public List<DeliveryRecord> load() {
        Optional<JSONObject> json = file.read();
        if (!json.isPresent()) return List.of();
        try {
            return decode(json.get());
        } catch (JSONException | IllegalArgumentException e) {
            return List.of();
        }
    }

    @Override
    public void save(List<DeliveryRecord> newestFirst) {
        try {
            file.write(encode(newestFirst));
        } catch (IOException | JSONException e) {
            // The log is still shown from memory; the previous file stays whole
        }
    }

    private static JSONObject encode(List<DeliveryRecord> newestFirst) throws JSONException {
        JSONArray records = new JSONArray();
        for (DeliveryRecord record : newestFirst) {
            records.put(new JSONObject()
                    .put("message", MessageJson.toJson(record.message()))
                    .put("result", record.result().name())
                    .put("attempted_at", record.attemptedAtMillis()));
        }
        return new JSONObject().put("records", records);
    }

    // DeliveryResult.valueOf throws IllegalArgumentException for a result this version does not know
    private static List<DeliveryRecord> decode(JSONObject json) throws JSONException {
        List<DeliveryRecord> records = new ArrayList<>();
        JSONArray saved = json.getJSONArray("records");
        for (int i = 0; i < saved.length(); i++) {
            JSONObject record = saved.getJSONObject(i);
            records.add(new DeliveryRecord(MessageJson.fromJson(record.getJSONObject("message")),
                    DeliveryResult.valueOf(record.getString("result")), record.getLong("attempted_at")));
        }
        return records;
    }
}
