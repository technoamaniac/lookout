package dev.aman.lookout;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Delivers events as a JSON POST to an HTTP(S) endpoint. */
public final class WebhookSink implements EventSink {
    private static final int TIMEOUT_MS = 5000;
    private static final int HTTP_REQUEST_TIMEOUT = 408;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private final URL url;

    public WebhookSink(URL url) {
        this.url = url;
    }

    @Override
    public DeliveryResult send(AppEvent event) {
        byte[] body = event.toJson().getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            // Following a redirect turns the POST into a body-less GET that would still look delivered
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(body.length);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(body);
            }
            return resultFor(conn.getResponseCode());
        } catch (IOException | RuntimeException e) {
            // Android's HTTP stack can throw unchecked exceptions (e.g. a bad host); on a worker
            // thread those would crash the whole process
            return DeliveryResult.TEMPORARY_FAILURE;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // Timeouts, rate limits and server errors may clear up; other refusals (a wrong path, a
    // redirect, bad auth) will not
    private static DeliveryResult resultFor(int status) {
        if (status >= 200 && status < 300) return DeliveryResult.DELIVERED;
        if (status == HTTP_REQUEST_TIMEOUT || status == HTTP_TOO_MANY_REQUESTS || status >= 500) {
            return DeliveryResult.TEMPORARY_FAILURE;
        }
        return DeliveryResult.PERMANENT_FAILURE;
    }
}
