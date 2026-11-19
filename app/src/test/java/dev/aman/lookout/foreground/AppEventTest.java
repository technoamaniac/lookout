package dev.aman.lookout.foreground;

import static org.junit.Assert.assertEquals;

import dev.aman.lookout.event.TvState;

import org.junit.Test;

public class AppEventTest {
    // The app is in the TV's state, so the event repeats nothing of it
    @Test
    public void sendsOnlyTheEventName() {
        assertEquals("{\"event\":\"app_changed\"}", new AppEvent("com.example.tv").fields().compact());
    }

    @Test
    public void becomesTheAppInTheTvsState() {
        TvState state = new AppEvent("com.example.a").applyTo(TvState.UNKNOWN);
        assertEquals("{\"app\":\"com.example.b\",\"playback\":[]}",
                new AppEvent("com.example.b").applyTo(state).fields().compact());
    }

    @Test
    public void escapesQuotesBackslashesAndControlChars() {
        assertEquals("{\"app\":\"a\\\"b\\\\c\\u000ad\",\"playback\":[]}",
                new AppEvent("a\"b\\c\nd").applyTo(TvState.UNKNOWN).fields().compact());
    }

    @Test
    public void prettyJsonHasTheSameFieldsIndentedForReading() {
        assertEquals("{\n  \"event\": \"app_changed\"\n}", new AppEvent("com.example.tv").fields().pretty());
    }
}
