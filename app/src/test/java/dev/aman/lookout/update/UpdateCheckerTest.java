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
        return includePreReleases -> Optional.of(new Release(Version.parse(version), url, url));
    }

    @Test
    public void offersANewerRelease() throws IOException {
        Optional<Release> update = new UpdateChecker(publishing("1.1.0")).newerThan(Version.parse("1.0.0"), false);
        assertEquals("1.1.0", update.orElseThrow().version().toString());
    }

    @Test
    public void staysQuietWhenAlreadyCurrent() throws IOException {
        assertTrue(new UpdateChecker(publishing("1.0.0")).newerThan(Version.parse("1.0.0"), false).isEmpty());
    }

    @Test
    public void neverOffersADowngrade() throws IOException {
        assertTrue(new UpdateChecker(publishing("1.0.0")).newerThan(Version.parse("1.2.0"), false).isEmpty());
    }

    @Test
    public void treatsADevBuildAsOlderThanAnyRelease() throws IOException {
        assertTrue(new UpdateChecker(publishing("0.0.1")).newerThan(Version.parse("0.0.0-dev"), false).isPresent());
    }

    @Test
    public void offersAPreReleaseNewerThanTheInstalledStableRelease() throws IOException {
        assertEquals("0.3.0-rc.1",
                new UpdateChecker(publishing("0.3.0-rc.1")).newerThan(Version.parse("0.2.0"), true).orElseThrow().version().toString());
    }

    @Test
    public void offersTheNextCandidateAndTheStableReleaseThatFollowsIt() throws IOException {
        assertTrue(new UpdateChecker(publishing("0.3.0-rc.2")).newerThan(Version.parse("0.3.0-rc.1"), true).isPresent());
        assertTrue(new UpdateChecker(publishing("0.3.0")).newerThan(Version.parse("0.3.0-rc.2"), true).isPresent());
    }

    @Test
    public void doesNotOfferACandidateOlderThanTheInstalledBuild() throws IOException {
        assertTrue(new UpdateChecker(publishing("0.3.0-rc.1")).newerThan(Version.parse("0.3.0"), true).isEmpty());
    }

    @Test
    public void passesTheChoiceToTheSource() throws IOException {
        URL url = new URL("https://example.com/lookout.apk");
        boolean[] asked = new boolean[1];
        ReleaseSource source = includePreReleases -> {
            asked[0] = includePreReleases;
            return Optional.of(new Release(Version.parse("9.0.0"), url, url));
        };
        new UpdateChecker(source).newerThan(Version.parse("1.0.0"), true);
        assertTrue(asked[0]);
    }

    @Test
    public void staysQuietWhenNothingIsPublished() throws IOException {
        assertTrue(new UpdateChecker(includePreReleases -> Optional.empty()).newerThan(Version.parse("1.0.0"), false).isEmpty());
    }
}
