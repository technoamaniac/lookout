package dev.aman.lookout.update;

import java.io.IOException;
import java.util.Optional;

/** Where the newest published release can be looked up. */
public interface ReleaseSource {
    /**
     * @param includePreReleases whether release candidates count; when false only stable releases do
     * @return the newest matching release, or empty if nothing has been published
     */
    Optional<Release> latest(boolean includePreReleases) throws IOException;
}
