package dev.aman.lookout.update;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the newest release of a public GitHub repository without the API: github.com redirects
 * /releases/latest to the tag's page, and the release workflow names its assets predictably.
 * This skips JSON parsing and the API's anonymous rate limit. GitHub's "latest" never points
 * at a pre-release.
 */
public final class GitHubReleases implements ReleaseSource {
    private static final int TIMEOUT_MS = 10_000;
    private static final String TAG_MARKER = "/releases/tag/";
    private static final Pattern STABLE_TAG = Pattern.compile("v(\\d+\\.\\d+\\.\\d+)");

    private final String baseUrl;
    private final String repo;

    /** @param baseUrl normally https://github.com; overridable for tests */
    public GitHubReleases(String baseUrl, String repo) {
        this.baseUrl = baseUrl;
        this.repo = repo;
    }

    @Override
    public Optional<Release> latest() throws IOException {
        String tag = latestTag();
        if (tag == null) return Optional.empty();
        Matcher m = STABLE_TAG.matcher(tag);
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

    private String latestTag() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "/" + repo + "/releases/latest").openConnection();
        try {
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setInstanceFollowRedirects(false);
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
}
