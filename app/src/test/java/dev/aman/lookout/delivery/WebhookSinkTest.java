package dev.aman.lookout.delivery;

import static dev.aman.lookout.delivery.DeliveryResult.DELIVERED;
import static dev.aman.lookout.delivery.DeliveryResult.PERMANENT_FAILURE;
import static dev.aman.lookout.delivery.DeliveryResult.TEMPORARY_FAILURE;
import static org.junit.Assert.assertEquals;

import dev.aman.lookout.foreground.AppEvent;
import dev.aman.lookout.support.FakeWebhookServer;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WebhookSinkTest {
    private static final AppEvent EVENT = new AppEvent("com.example.a", 1);

    private FakeWebhookServer server;
    private WebhookSink sink;

    @Before
    public void startServer() throws IOException {
        server = new FakeWebhookServer();
        sink = new WebhookSink(server.url("/hook"));
    }

    @After
    public void stopServer() throws IOException {
        server.close();
    }

    @Test
    public void postsTheEventAsJson() {
        assertEquals(DELIVERED, sink.send(EVENT));
        FakeWebhookServer.Request request = server.requests().get(0);
        assertEquals("POST", request.method);
        assertEquals("/hook", request.path);
        assertEquals("application/json", request.contentType);
        assertEquals(EVENT.toJson(), request.body);
    }

    @Test
    public void serverErrorsAreTemporary() {
        server.respondWith(503);
        assertEquals(TEMPORARY_FAILURE, sink.send(EVENT));
    }

    @Test
    public void timeoutAndRateLimitResponsesAreTemporary() {
        server.respondWith(408);
        assertEquals(TEMPORARY_FAILURE, sink.send(EVENT));
        server.respondWith(429);
        assertEquals(TEMPORARY_FAILURE, sink.send(EVENT));
    }

    @Test
    public void otherClientErrorsArePermanent() {
        server.respondWith(404);
        assertEquals(PERMANENT_FAILURE, sink.send(EVENT));
    }

    @Test
    public void redirectIsPermanentAndNotFollowed() {
        server.respondWith(302);
        assertEquals(PERMANENT_FAILURE, sink.send(EVENT));
        assertEquals(1, server.requests().size());
    }

    @Test
    public void unreachableServerIsTemporary() throws IOException {
        server.close();
        assertEquals(TEMPORARY_FAILURE, sink.send(EVENT));
    }
}
