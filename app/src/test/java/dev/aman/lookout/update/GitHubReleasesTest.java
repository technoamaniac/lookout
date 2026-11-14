package dev.aman.lookout.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import dev.aman.lookout.support.FakeWebhookServer;

import java.io.IOException;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GitHubReleasesTest {
    private static final String LATEST = "/owner/repo/releases/latest";

    private FakeWebhookServer server;
    private GitHubReleases releases;

    @Before
    public void startServer() throws IOException {
        server = new FakeWebhookServer();
        releases = new GitHubReleases(server.baseUrl(), "owner/repo");
    }

    @After
    public void stopServer() throws IOException {
        server.close();
    }

    @Test
    public void readsTheVersionFromTheRedirectAndBuildsAssetUrls() throws IOException {
        server.route(LATEST, 302, server.baseUrl() + "/owner/repo/releases/tag/v1.4.2", new byte[0]);

        Release release = releases.latest().orElseThrow();

        assertEquals("1.4.2", release.version().toString());
        assertEquals(server.baseUrl() + "/owner/repo/releases/download/v1.4.2/lookout-v1.4.2.apk",
                release.apkUrl().toString());
        assertEquals(release.apkUrl() + ".sha256", release.checksumUrl().toString());
    }

    @Test
    public void reportsNothingWhenTheRepoHasNoReleases() throws IOException {
        server.route(LATEST, 302, "/owner/repo/releases", new byte[0]);
        assertTrue(releases.latest().isEmpty());
    }

    @Test
    public void reportsNothingOnNotFound() throws IOException {
        server.route(LATEST, 404, null, new byte[0]);
        assertEquals(Optional.empty(), releases.latest());
    }

    @Test
    public void ignoresTagsThatAreNotPlainVersions() throws IOException {
        server.route(LATEST, 302, "/owner/repo/releases/tag/nightly%20build", new byte[0]);
        assertTrue(releases.latest().isEmpty());
    }

    @Test(expected = IOException.class)
    public void failsOnAServerError() throws IOException {
        server.route(LATEST, 503, null, new byte[0]);
        releases.latest();
    }
}
