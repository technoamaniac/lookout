package dev.aman.lookout.delivery;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import dev.aman.lookout.event.EventCategory;
import dev.aman.lookout.event.TvState;
import dev.aman.lookout.foreground.AppEvent;
import dev.aman.lookout.heartbeat.HeartbeatEvent;
import dev.aman.lookout.playback.MediaDetails;
import dev.aman.lookout.playback.PlayState;
import dev.aman.lookout.playback.PlaybackEvent;
import dev.aman.lookout.playback.PlaybackSnapshot;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class StoreFilesTest {
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private final Message app = Message.withoutState(new AppEvent("com.example.a", "Main"), null, 1_000);
    // Quotes and a newline in the title, and the nested TV state, must survive the round trip
    private final PlaybackEvent playing = new PlaybackEvent(new PlaybackSnapshot("com.example.p", PlayState.PLAYING,
            MediaDetails.builder().title("Say \"hi\"\nnow").build(), PlaybackSnapshot.NO_POSITION));
    private final Message playback = Message.of(playing, playing.applyTo(TvState.UNKNOWN), "Den", 2_000);

    private File file(String name) {
        return new File(folder.getRoot(), name);
    }

    private static void assertSameMessage(Message expected, Message actual) {
        assertEquals(expected.id(), actual.id());
        assertEquals(expected.summary(), actual.summary());
        assertEquals(expected.occurredAtMillis(), actual.occurredAtMillis());
        assertEquals(expected.toJson(), actual.toJson());
    }

    @Test
    public void aFailedSaveKeepsThePreviousDeliveryLog() {
        DeliveryLogFile store = new DeliveryLogFile(file("deliveries.json"));
        store.save(List.of(new DeliveryRecord(app, DeliveryResult.DELIVERED, 8_000)));
        // A folder that cannot be written to makes the next save fail before it touches the file
        assertTrue(folder.getRoot().setWritable(false));
        try {
            store.save(List.of());
        } finally {
            folder.getRoot().setWritable(true);
        }

        assertEquals(1, store.load().size());
    }

    @Test
    public void savingLeavesNoTemporaryFilesBehind() {
        DeliveryLogFile store = new DeliveryLogFile(file("deliveries.json"));
        store.save(List.of(new DeliveryRecord(app, DeliveryResult.DELIVERED, 8_000)));
        store.save(List.of());
        assertArrayEquals(new String[] {"deliveries.json"}, folder.getRoot().list());
    }

    @Test
    public void deliveryLogRoundTrips() {
        DeliveryLogFile store = new DeliveryLogFile(file("deliveries.json"));
        store.save(List.of(new DeliveryRecord(playback, DeliveryResult.TEMPORARY_FAILURE, 9_000),
                new DeliveryRecord(app, DeliveryResult.DELIVERED, 8_000)));

        List<DeliveryRecord> loaded = new DeliveryLogFile(file("deliveries.json")).load();

        assertEquals(2, loaded.size());
        assertSameMessage(playback, loaded.get(0).message());
        assertEquals(DeliveryResult.TEMPORARY_FAILURE, loaded.get(0).result());
        assertEquals(9_000, loaded.get(0).attemptedAtMillis());
        assertEquals(DeliveryResult.DELIVERED, loaded.get(1).result());
    }

    @Test
    public void aSavedHeartbeatKeepsItsKind() {
        DeliveryLogFile store = new DeliveryLogFile(file("deliveries.json"));
        Message beat = Message.withoutState(new HeartbeatEvent(60), null, 3_000);
        store.save(List.of(new DeliveryRecord(beat, DeliveryResult.DELIVERED, 3_000)));
        assertEquals(EventCategory.HEARTBEAT, new DeliveryLogFile(file("deliveries.json")).load().get(0).message().category());
    }

    // Written by 0.4.0-rc.2 and earlier, before messages had a category
    @Test
    public void aSavedMessageWithoutAKindIsAChange() throws IOException {
        Files.write(file("deliveries.json").toPath(), ("{\"version\":1,\"records\":[{\"message\":{\"id\":\"x\","
                + "\"key\":\"app\",\"summary\":\"a\",\"occurred_at\":1,\"fields\":[[\"event\",\"\\\"app_changed\\\"\"]]},"
                + "\"result\":\"DELIVERED\",\"attempted_at\":2}]}").getBytes(StandardCharsets.UTF_8));
        assertEquals(EventCategory.CHANGE, new DeliveryLogFile(file("deliveries.json")).load().get(0).message().category());
    }

    @Test
    public void aDamagedDeliveryLogStartsEmpty() throws IOException {
        Files.write(file("deliveries.json").toPath(),
                "{\"version\":1,\"records\":[{\"result\":\"LOST\"}]}".getBytes(StandardCharsets.UTF_8));
        assertTrue(new DeliveryLogFile(file("deliveries.json")).load().isEmpty());
    }

    @Test
    public void loadingClearsTemporaryFilesACrashLeftBehind() throws IOException {
        Files.write(file("deliveries.json123.tmp").toPath(), "{".getBytes(StandardCharsets.UTF_8));
        Files.write(file("other.json456.tmp").toPath(), "{".getBytes(StandardCharsets.UTF_8));

        new DeliveryLogFile(file("deliveries.json")).load();

        assertArrayEquals(new String[] {"other.json456.tmp"}, folder.getRoot().list());
    }
}
