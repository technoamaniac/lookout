package dev.aman.lookout.update;

import java.io.IOException;
import java.util.Optional;

/** Where the notes a release was published with can be read. */
public interface ReleaseNotesSource {
    /** @return the notes as HTML to render, or empty if the release has none or has left the source's list */
    Optional<String> notesFor(Release release) throws IOException;
}
