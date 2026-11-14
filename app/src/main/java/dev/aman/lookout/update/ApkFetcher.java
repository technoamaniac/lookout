package dev.aman.lookout.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Downloads a release APK and checks it against its published SHA-256. That catches truncated
 * or corrupted downloads; Android's own signature check on install is what rejects an APK
 * that was not built by us.
 */
public final class ApkFetcher {
    private static final int TIMEOUT_MS = 15_000;
    private static final long MAX_APK_BYTES = 100L * 1024 * 1024;
    private static final int MAX_CHECKSUM_BYTES = 1024;
    private static final Pattern CHECKSUM_LINE = Pattern.compile("^([0-9a-fA-F]{64})(\\s.*)?$", Pattern.DOTALL);

    /** @return the verified APK inside {@code dir} */
    public File fetch(Release release, File dir) throws IOException {
        String expected = readChecksum(release.checksumUrl());
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create " + dir);
        File apk = new File(dir, "update.apk");
        MessageDigest digest = sha256();
        HttpURLConnection conn = open(release.apkUrl());
        try (InputStream in = conn.getInputStream(); OutputStream out = new FileOutputStream(apk)) {
            copy(in, out, digest);
        } catch (IOException | RuntimeException e) {
            apk.delete();
            throw e;
        } finally {
            conn.disconnect();
        }
        if (!hex(digest.digest()).equals(expected.toLowerCase(Locale.ROOT))) {
            apk.delete();
            throw new IOException("Checksum mismatch");
        }
        return apk;
    }

    private static String readChecksum(URL url) throws IOException {
        HttpURLConnection conn = open(url);
        try (InputStream in = conn.getInputStream()) {
            byte[] buffer = new byte[MAX_CHECKSUM_BYTES + 1];
            int length = 0;
            for (int n; length < buffer.length && (n = in.read(buffer, length, buffer.length - length)) > 0; ) {
                length += n;
            }
            if (length > MAX_CHECKSUM_BYTES) throw new IOException("Checksum file is too large");
            Matcher m = CHECKSUM_LINE.matcher(new String(buffer, 0, length, StandardCharsets.UTF_8).trim());
            if (!m.matches()) throw new IOException("Checksum file is malformed");
            return m.group(1);
        } finally {
            conn.disconnect();
        }
    }

    private static HttpURLConnection open(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        int status = conn.getResponseCode(); // Redirects are followed: assets live on a CDN host
        if (status != HttpURLConnection.HTTP_OK) {
            conn.disconnect();
            throw new IOException("Download failed with status " + status);
        }
        return conn;
    }

    private static void copy(InputStream in, OutputStream out, MessageDigest digest) throws IOException {
        byte[] buffer = new byte[16 * 1024];
        long total = 0;
        for (int n; (n = in.read(buffer)) > 0; ) {
            total += n;
            if (total > MAX_APK_BYTES) throw new IOException("Download is too large");
            digest.update(buffer, 0, n);
            out.write(buffer, 0, n);
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) out.append(String.format("%02x", b));
        return out.toString();
    }
}
