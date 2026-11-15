package dev.aman.lookout.delivery;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * A small JSON object kept in a file, replaced whole on each write: a crash or power cut mid-write leaves the previous
 * version rather than half of the new one. Each file records its format version, and one of another version reads as
 * missing, so a format change starts afresh instead of misreading old data.
 */
final class JsonFile {
    private static final String VERSION = "version";
    private static final String TEMPORARY_SUFFIX = ".tmp";

    private final File file;
    private final int version;

    JsonFile(File file, int version) {
        this.file = file;
        this.version = version;
    }

    /**
     * The saved object, or empty when there is none, it cannot be read, or it has another version. Read once at start,
     * before any write, so it also clears temporary files a crash left behind.
     */
    Optional<JSONObject> read() {
        deleteLeftoverTemporaryFiles();
        if (!file.exists()) return Optional.empty();
        try {
            JSONObject json = new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            return json.optInt(VERSION) == version ? Optional.of(json) : Optional.empty();
        } catch (IOException | JSONException e) {
            return Optional.empty();
        }
    }

    /**
     * Stamps {@code json} with the version and saves it. The text goes to a temporary file of its own, is flushed to
     * disk, then renamed over the file, so two writers can never mix their text.
     */
    void write(JSONObject json) throws IOException, JSONException {
        byte[] text = json.put(VERSION, version).toString().getBytes(StandardCharsets.UTF_8);
        File temporary = File.createTempFile(file.getName(), TEMPORARY_SUFFIX, file.getParentFile());
        try {
            try (FileOutputStream out = new FileOutputStream(temporary)) {
                out.write(text);
                out.getFD().sync();
            }
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            // Already gone after a successful move; otherwise it must not pile up
            temporary.delete();
        }
    }

    private void deleteLeftoverTemporaryFiles() {
        File[] leftovers = file.getParentFile().listFiles((dir, name) ->
                name.startsWith(file.getName()) && name.endsWith(TEMPORARY_SUFFIX));
        if (leftovers == null) return;
        for (File leftover : leftovers) leftover.delete();
    }
}
