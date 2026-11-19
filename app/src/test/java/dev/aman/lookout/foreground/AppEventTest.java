package dev.aman.lookout.foreground;

import static org.junit.Assert.assertEquals;

import dev.aman.lookout.event.TvState;

import org.junit.Test;

public class AppEventTest {
    @Test
    public void sendsOnlyTheEventWhenThereIsNoActivity() {
        assertEquals("{\"event\":\"app_changed\"}",
                new AppEvent("com.example.tv").fields().compact());
    }

    @Test
    public void includesTheActivityWhenKnown() {
        assertEquals("{\"event\":\"app_changed\",\"activity\":\"com.example.tv.Main\"}",
                new AppEvent("com.example.tv", "com.example.tv.Main").fields().compact());
    }

    @Test
    public void leavesOutABlankActivity() {
        assertEquals("{\"event\":\"app_changed\"}",
                new AppEvent("com.example.tv", " ").fields().compact());
    }

    @Test
    public void becomesTheAppInTheTvsState() {
        TvState state = new AppEvent("com.example.a").applyTo(TvState.UNKNOWN);
        assertEquals("{\"app\":\"com.example.b\",\"playback\":[]}",
                new AppEvent("com.example.b", "com.example.b.Main").applyTo(state).fields().compact());
    }

    @Test
    public void escapesQuotesBackslashesAndControlChars() {
        assertEquals("{\"event\":\"app_changed\",\"activity\":\"a\\\"b\\\\c\\u000ad\"}",
                new AppEvent("com.example.tv", "a\"b\\c\nd").fields().compact());
    }

    @Test
    public void prettyJsonHasTheSameFieldsIndentedForReading() {
        assertEquals("{\n  \"event\": \"app_changed\",\n  \"activity\": \"com.example.tv.Main\"\n}",
                new AppEvent("com.example.tv", "com.example.tv.Main").fields().pretty());
    }
}
