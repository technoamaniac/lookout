package dev.aman.lookout.update;

import java.net.URL;

/** A published version of the app and where to download it. */
public final class Release {
    private final Version version;
    private final URL apkUrl;
    private final URL checksumUrl;

    public Release(Version version, URL apkUrl, URL checksumUrl) {
        this.version = version;
        this.apkUrl = apkUrl;
        this.checksumUrl = checksumUrl;
    }

    public Version version() {
        return version;
    }

    public URL apkUrl() {
        return apkUrl;
    }

    /** A file holding the APK's SHA-256, in the format of `sha256sum`. */
    public URL checksumUrl() {
        return checksumUrl;
    }
}
