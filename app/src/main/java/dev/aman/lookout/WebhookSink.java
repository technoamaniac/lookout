package dev.aman.lookout;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Delivers events as a JSON POST to an HTTP(S) endpoint. */
public final class WebhookSink implements EventSink {
    private static final int TIMEOUT_MS = 5000;

    private final URL url;

    public WebhookSink(URL url) {
        this.url = url;
    }

    @Override
    public boolean send(AppEvent event) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(event.toJson().getBytes(StandardCharsets.UTF_8));
            }
            return conn.getResponseCode() < 300;
        } catch (IOException e) {
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
