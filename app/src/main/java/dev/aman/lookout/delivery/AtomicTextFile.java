package dev.aman.lookout.delivery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * A small text file replaced whole on each write, so a crash or power cut mid-write leaves the previous version
 * rather than half of the new one: the text goes to a temporary file, is flushed to disk, then renamed over it.
 */
final class AtomicTextFile {
    private final File file;
    private final File temporary;

    AtomicTextFile(File file) {
        this.file = file;
        this.temporary = new File(file.getPath() + ".tmp");
    }

    /** The file's text, or empty when it does not exist yet. */
    Optional<String> read() throws IOException {
        if (!file.exists()) return Optional.empty();
        return Optional.of(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
    }

    void write(String text) throws IOException {
        try (FileOutputStream out = new FileOutputStream(temporary)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        }
        Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
    }
}
