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
    private static final String FEED = "/owner/repo/releases.atom";

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

        Release release = releases.latest(false).orElseThrow();

        assertEquals("1.4.2", release.version().toString());
        assertEquals(server.baseUrl() + "/owner/repo/releases/download/v1.4.2/lookout-v1.4.2.apk",
                release.apkUrl().toString());
        assertEquals(release.apkUrl() + ".sha256", release.checksumUrl().toString());
    }

    @Test
    public void reportsNothingWhenTheRepoHasNoReleases() throws IOException {
        server.route(LATEST, 302, "/owner/repo/releases", new byte[0]);
        assertTrue(releases.latest(false).isEmpty());
    }

    @Test
    public void reportsNothingOnNotFound() throws IOException {
        server.route(LATEST, 404, null, new byte[0]);
        assertEquals(Optional.empty(), releases.latest(false));
    }

    @Test
    public void ignoresTagsThatAreNotPlainVersions() throws IOException {
        server.route(LATEST, 302, "/owner/repo/releases/tag/nightly%20build", new byte[0]);
        assertTrue(releases.latest(false).isEmpty());
    }

    @Test(expected = IOException.class)
    public void failsOnAServerError() throws IOException {
        server.route(LATEST, 503, null, new byte[0]);
        releases.latest(false);
    }

    private static byte[] feedOf(String... tags) {
        StringBuilder feed = new StringBuilder("<feed>");
        for (String tag : tags) {
            feed.append("<entry><link rel=\"alternate\" type=\"text/html\" href=\"https://github.com/owner/repo/releases/tag/")
                    .append(tag).append("\"/><title>").append(tag).append("</title></entry>");
        }
        return feed.append("</feed>").toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    public void findsAPreReleaseInTheFeedWhenAsked() throws IOException {
        server.route(FEED, 200, null, feedOf("v0.3.0-rc.1", "v0.2.0"));

        Release release = releases.latest(true).orElseThrow();

        assertEquals("0.3.0-rc.1", release.version().toString());
        assertEquals(server.baseUrl() + "/owner/repo/releases/download/v0.3.0-rc.1/lookout-v0.3.0-rc.1.apk",
                release.apkUrl().toString());
    }

    @Test
    public void aStableReleaseBeatsItsOwnCandidatesWhateverTheFeedOrder() throws IOException {
        server.route(FEED, 200, null, feedOf("v0.3.0-rc.2", "v0.3.0", "v0.3.0-rc.1"));
        assertEquals("0.3.0", releases.latest(true).orElseThrow().version().toString());
    }

    @Test
    public void skipsFeedTagsThatAreNotVersions() throws IOException {
        server.route(FEED, 200, null, feedOf("nightly", "v1.0.0-rc.1"));
        assertEquals("1.0.0-rc.1", releases.latest(true).orElseThrow().version().toString());
    }

    @Test
    public void fallsBackToTheLatestStableReleaseWhenTheFeedYieldsNothing() throws IOException {
        server.route(LATEST, 302, server.baseUrl() + "/owner/repo/releases/tag/v0.2.0", new byte[0]);
        server.route(FEED, 200, null, feedOf("nightly"));
        assertEquals("0.2.0", releases.latest(true).orElseThrow().version().toString());
        server.route(FEED, 404, null, new byte[0]);
        assertEquals("0.2.0", releases.latest(true).orElseThrow().version().toString());
    }

    @Test
    public void reportsNothingWhenThereAreNoReleasesAtAll() throws IOException {
        server.route(LATEST, 302, "/owner/repo/releases", new byte[0]);
        server.route(FEED, 200, null, feedOf());
        assertTrue(releases.latest(true).isEmpty());
    }

    @Test(expected = IOException.class)
    public void failsWhenTheFeedCannotBeRead() throws IOException {
        server.route(FEED, 503, null, new byte[0]);
        releases.latest(true);
    }

    @Test
    public void neverReturnsAPreReleaseWithoutBeingAsked() throws IOException {
        server.route(LATEST, 302, server.baseUrl() + "/owner/repo/releases/tag/v0.2.0", new byte[0]);
        server.route(FEED, 200, null, feedOf("v0.3.0-rc.1", "v0.2.0"));
        assertEquals("0.2.0", releases.latest(false).orElseThrow().version().toString());
    }
}
