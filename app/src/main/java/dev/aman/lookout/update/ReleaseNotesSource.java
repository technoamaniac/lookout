package dev.aman.lookout.update;

import java.io.IOException;
import java.util.Optional;

/** Where the notes a version was published with can be read. */
public interface ReleaseNotesSource {
    /** @return the notes as HTML to render, or empty if the version has none or has left the source's list */
    Optional<String> notesFor(Version version) throws IOException;
}
