package dev.aman.lookout.config;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Validated, immutable user configuration. */
public final class Config {
    /** What the URL field starts with; on its own it means no URL yet. */
    public static final String URL_PREFIX = "https://";

    private final URL webhookUrl;
    private final List<String> ignoredPackages;
    private final boolean sendDeviceName;

    private Config(URL webhookUrl, List<String> ignoredPackages, boolean sendDeviceName) {
        this.webhookUrl = webhookUrl;
        this.ignoredPackages = Collections.unmodifiableList(ignoredPackages);
        this.sendDeviceName = sendDeviceName;
    }

    /**
     * @param url blank or just {@link #URL_PREFIX} means "not configured"; otherwise it must be an https URL
     * @param ignoredCsv comma-separated packages to never report
     * @param sendDeviceName whether events carry the TV's name, which can be personal
     * @throws IllegalArgumentException if the URL is malformed or not https
     */
    public static Config parse(String url, String ignoredCsv, boolean sendDeviceName) {
        return new Config(parseUrl(url), parsePackages(ignoredCsv), sendDeviceName);
    }

    public Optional<URL> webhookUrl() {
        return Optional.ofNullable(webhookUrl);
    }

    public List<String> ignoredPackages() {
        return ignoredPackages;
    }

    public boolean sendDeviceName() {
        return sendDeviceName;
    }

    public String webhookUrlText() {
        return webhookUrl == null ? "" : webhookUrl.toString();
    }

    public String ignoredPackagesText() {
        return String.join(", ", ignoredPackages);
    }

    // Compares the URL as text: URL#equals resolves host names over the network
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Config)) return false;
        Config that = (Config) other;
        return webhookUrlText().equals(that.webhookUrlText()) && ignoredPackages.equals(that.ignoredPackages)
                && sendDeviceName == that.sendDeviceName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(webhookUrlText(), ignoredPackages, sendDeviceName);
    }

    private static URL parseUrl(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty() || text.equalsIgnoreCase(URL_PREFIX)) return null;
        URL url;
        try {
            // Parse as a URI first: it rejects characters such as spaces that URL lets through
            url = new URI(text).toURL();
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Not a valid URL", e);
        }
        if (!"https".equals(url.getProtocol())) {
            throw new IllegalArgumentException("URL must start with https://");
        }
        if (url.getHost().isEmpty()) {
            throw new IllegalArgumentException("URL has no host");
        }
        return url;
    }

    private static List<String> parsePackages(String csv) {
        Set<String> packages = new LinkedHashSet<>();
        if (csv != null) {
            for (String part : csv.split(",")) {
                String pkg = part.trim();
                if (!pkg.isEmpty()) packages.add(pkg);
            }
        }
        return new ArrayList<>(packages);
    }
}
