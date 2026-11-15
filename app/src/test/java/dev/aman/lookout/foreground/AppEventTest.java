package dev.aman.lookout.foreground;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AppEventTest {
    @Test
    public void serialisesEventPackageAndTimestamp() {
        assertEquals("{\"event\":\"app_changed\",\"package\":\"com.example.tv\",\"timestamp\":42}",
                new AppEvent("com.example.tv", 42).fields().compact());
    }

    @Test
    public void includesActivityAndDeviceWhenKnown() {
        assertEquals("{\"event\":\"app_changed\",\"package\":\"com.example.tv\","
                        + "\"activity\":\"com.example.tv.Main\",\"device\":\"Den\",\"timestamp\":42}",
                new AppEvent("com.example.tv", 42).withActivity("com.example.tv.Main").withDevice("Den").fields().compact());
    }

    @Test
    public void leavesOutBlankActivityAndDevice() {
        assertEquals("{\"event\":\"app_changed\",\"package\":\"com.example.tv\",\"timestamp\":42}",
                new AppEvent("com.example.tv", 42).withActivity(" ").withDevice("").fields().compact());
    }

    @Test
    public void escapesQuotesBackslashesAndControlChars() {
        assertEquals("{\"event\":\"app_changed\",\"package\":\"a\\\"b\\\\c\\u000ad\",\"timestamp\":1}",
                new AppEvent("a\"b\\c\nd", 1).fields().compact());
    }

    @Test
    public void prettyJsonHasTheSameFieldsIndentedForReading() {
        assertEquals("{\n  \"event\": \"app_changed\",\n  \"package\": \"com.example.tv\","
                        + "\n  \"activity\": \"com.example.tv.Main\",\n  \"timestamp\": 42\n}",
                new AppEvent("com.example.tv", 42).withActivity("com.example.tv.Main").fields().pretty());
    }
}
