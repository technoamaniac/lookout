package dev.aman.lookout.update;

import java.io.IOException;
import java.util.Optional;

/** Decides whether a published release is newer than the installed version. */
public final class UpdateChecker {
    private final ReleaseSource source;

    public UpdateChecker(ReleaseSource source) {
        this.source = source;
    }

    public Optional<Release> newerThan(Version installed, boolean includePreReleases) throws IOException {
        return source.latest(includePreReleases).filter(release -> release.version().compareTo(installed) > 0);
    }
}
