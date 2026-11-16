package dev.aman.lookout.update;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the newest release of a public GitHub repository without the API, and so without JSON
 * parsing or its anonymous rate limit. The release workflow names its assets predictably, so a
 * tag is all that is needed. Stable releases come from /releases/latest, which redirects to the
 * tag's page and never points at a pre-release. Pre-releases are only listed in the releases
 * feed, so that is read when they are wanted.
 */
public final class GitHubReleases implements ReleaseSource {
    private static final int TIMEOUT_MS = 10_000;
    // The feed lists ten entries in tens of kilobytes; a cap keeps a broken or hostile reply from filling the memory
    private static final int MAX_FEED_BYTES = 1024 * 1024;
    private static final String TAG_MARKER = "/releases/tag/";
    private static final Pattern STABLE_TAG = Pattern.compile("v(\\d+\\.\\d+\\.\\d+)");
    private static final Pattern ANY_TAG = Pattern.compile("v(\\d+\\.\\d+\\.\\d+(?:-[0-9A-Za-z.-]+)?)");
    private static final Pattern FEED_TAG = Pattern.compile(Pattern.quote(TAG_MARKER) + "([^\"<>\\s]+)\"");

    private final String baseUrl;
    private final String repo;

    /** @param baseUrl normally https://github.com; overridable for tests */
    public GitHubReleases(String baseUrl, String repo) {
        this.baseUrl = baseUrl;
        this.repo = repo;
    }

    /** Lookout's own public releases, which the release workflow publishes. */
    public static GitHubReleases ofLookout() {
        return new GitHubReleases("https://github.com", "technoamaniac/lookout");
    }

    @Override
    public Optional<Release> latest(boolean includePreReleases) throws IOException {
        if (includePreReleases) {
            Optional<Release> fromFeed = newestInFeed();
            // The feed holds only the newest entries; never end up with less than a stable-only check finds
            if (fromFeed.isPresent()) return fromFeed;
        }
        return releaseFor(latestTag(), STABLE_TAG);
    }

    // The feed is newest first, but the order of tags is not the order of versions: 1.0.0 beats 1.0.0-rc.2
    private Optional<Release> newestInFeed() throws IOException {
        Optional<Release> newest = Optional.empty();
        Matcher tags = FEED_TAG.matcher(readFeed());
        while (tags.find()) {
            Optional<Release> candidate = releaseFor(tags.group(1), ANY_TAG);
            if (candidate.isPresent()
                    && (!newest.isPresent() || candidate.get().version().compareTo(newest.get().version()) > 0)) {
                newest = candidate;
            }
        }
        return newest;
    }

    private Optional<Release> releaseFor(String tag, Pattern shape) throws IOException {
        if (tag == null) return Optional.empty();
        Matcher m = shape.matcher(tag);
        // A tag in any other shape is not one of ours; the strict match also keeps odd
        // characters out of the download URLs built below
        if (!m.matches()) return Optional.empty();
        String assets = baseUrl + "/" + repo + "/releases/download/" + tag + "/lookout-" + tag + ".apk";
        Version version;
        try {
            version = Version.parse(m.group(1));
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // A number too large to be one of our versions
        }
        return Optional.of(new Release(version, new URL(assets), new URL(assets + ".sha256")));
    }

    private String readFeed() throws IOException {
        HttpURLConnection conn = open("/releases.atom");
        try {
            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_NOT_FOUND) return "";
            if (status != HttpURLConnection.HTTP_OK) throw new IOException("Unexpected response: " + status);
            try (InputStream in = conn.getInputStream()) {
                ByteArrayOutputStream body = new ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                for (int n; (n = in.read(chunk)) != -1; ) {
                    if (body.size() + n > MAX_FEED_BYTES) throw new IOException("Release feed is too large");
                    body.write(chunk, 0, n);
                }
                return body.toString("UTF-8");
            }
        } finally {
            conn.disconnect();
        }
    }

    private String latestTag() throws IOException {
        HttpURLConnection conn = open("/releases/latest");
        try {
            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_NOT_FOUND) return null;
            String location = conn.getHeaderField("Location");
            if (status / 100 != 3 || location == null) throw new IOException("Unexpected response: " + status);
            // With no releases GitHub redirects to the releases list instead of a tag page
            int marker = location.indexOf(TAG_MARKER);
            return marker < 0 ? null : location.substring(marker + TAG_MARKER.length());
        } finally {
            conn.disconnect();
        }
    }

    // Redirects are read, not followed: the one from /releases/latest is what names the tag
    private HttpURLConnection open(String path) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "/" + repo + path).openConnection();
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setInstanceFollowRedirects(false);
        return conn;
    }
}
