package dev.aman.lookout.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.junit.Test;

public class UpdateCheckerTest {
    private static ReleaseSource publishing(String version) throws IOException {
        URL url = new URL("https://example.com/lookout.apk");
        return () -> Optional.of(new Release(Version.parse(version), url, url));
    }

    @Test
    public void offersANewerRelease() throws IOException {
        Optional<Release> update = new UpdateChecker(publishing("1.1.0")).newerThan(Version.parse("1.0.0"));
        assertEquals("1.1.0", update.orElseThrow().version().toString());
    }

    @Test
    public void staysQuietWhenAlreadyCurrent() throws IOException {
        assertTrue(new UpdateChecker(publishing("1.0.0")).newerThan(Version.parse("1.0.0")).isEmpty());
    }

    @Test
    public void neverOffersADowngrade() throws IOException {
        assertTrue(new UpdateChecker(publishing("1.0.0")).newerThan(Version.parse("1.2.0")).isEmpty());
    }

    @Test
    public void treatsADevBuildAsOlderThanAnyRelease() throws IOException {
        assertTrue(new UpdateChecker(publishing("0.0.1")).newerThan(Version.parse("0.0.0-dev")).isPresent());
    }

    @Test
    public void staysQuietWhenNothingIsPublished() throws IOException {
        assertTrue(new UpdateChecker(Optional::empty).newerThan(Version.parse("1.0.0")).isEmpty());
    }
}
