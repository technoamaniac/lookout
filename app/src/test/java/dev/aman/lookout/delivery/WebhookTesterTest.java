package dev.aman.lookout.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import dev.aman.lookout.foreground.AppEvent;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class WebhookTesterTest {
    private final DeliveryLog log = new DeliveryLog(10);
    private final AppEvent event = new AppEvent("com.example.test", 5);

    @Test
    public void sendsToTheGivenUrlAndReturnsTheResult() throws Exception {
        List<URL> urls = new ArrayList<>();
        WebhookTester tester = new WebhookTester(url -> {
            urls.add(url);
            return e -> DeliveryResult.PERMANENT_FAILURE;
        }, log, () -> 0);
        URL url = new URL("https://example.com/h");

        assertEquals(DeliveryResult.PERMANENT_FAILURE, tester.test(url, event));
        assertEquals(List.of(url), urls);
    }

    @Test
    public void recordsTheAttemptForTheDeliveriesTab() throws Exception {
        WebhookTester tester = new WebhookTester(url -> e -> DeliveryResult.DELIVERED, log, () -> 7_000);

        tester.test(new URL("https://example.com/h"), event);

        DeliveryRecord record = log.recent().get(0);
        assertEquals(event.summary(), record.message().summary());
        assertTrue(record.message().toJson().contains("\"id\":"));
        assertEquals(DeliveryResult.DELIVERED, record.result());
        assertEquals(7_000, record.attemptedAtMillis());
    }
}
