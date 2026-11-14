package dev.aman.lookout.update;

import java.io.IOException;
import java.util.Optional;

/** Where the newest published release can be looked up. */
public interface ReleaseSource {
    /** @return the newest stable release, or empty if nothing has been published */
    Optional<Release> latest() throws IOException;
}
