package dev.aman.lookout.update;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import dev.aman.lookout.support.FakeWebhookServer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ApkFetcherTest {
    private static final byte[] APK = "not really an apk".getBytes(StandardCharsets.UTF_8);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private FakeWebhookServer server;
    private Release release;

    @Before
    public void startServer() throws IOException {
        server = new FakeWebhookServer();
        release = new Release(Version.parse("1.0.0"), server.url("/lookout.apk"), server.url("/lookout.apk.sha256"));
    }

    @After
    public void stopServer() throws IOException {
        server.close();
    }

    private static String sha256Of(byte[] data) throws Exception {
        StringBuilder hex = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(data)) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    private void publish(byte[] apk, String checksumFile) {
        server.route("/lookout.apk", 200, null, apk);
        server.route("/lookout.apk.sha256", 200, null, checksumFile.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void savesAnApkThatMatchesItsChecksum() throws Exception {
        publish(APK, sha256Of(APK) + "  lookout-v1.0.0.apk\n");

        File apk = new ApkFetcher().fetch(release, folder.getRoot());

        assertArrayEquals(APK, Files.readAllBytes(apk.toPath()));
    }

    @Test
    public void acceptsAnUppercaseChecksum() throws Exception {
        publish(APK, sha256Of(APK).toUpperCase() + "\n");
        assertTrue(new ApkFetcher().fetch(release, folder.getRoot()).isFile());
    }

    @Test
    public void rejectsAndDeletesAnApkThatDoesNotMatch() throws Exception {
        publish("tampered".getBytes(StandardCharsets.UTF_8), sha256Of(APK) + "  lookout.apk\n");

        try {
            new ApkFetcher().fetch(release, folder.getRoot());
            throw new AssertionError("Expected a checksum failure");
        } catch (IOException expected) {
            assertFalse(new File(folder.getRoot(), "update.apk").exists());
        }
    }

    @Test(expected = IOException.class)
    public void rejectsAMalformedChecksumFile() throws Exception {
        publish(APK, "<html>not a checksum</html>");
        new ApkFetcher().fetch(release, folder.getRoot());
    }

    @Test(expected = IOException.class)
    public void failsWhenTheApkIsMissing() throws Exception {
        server.route("/lookout.apk.sha256", 200, null, (sha256Of(APK) + "\n").getBytes(StandardCharsets.UTF_8));
        server.route("/lookout.apk", 404, null, new byte[0]);
        new ApkFetcher().fetch(release, folder.getRoot());
    }
}
