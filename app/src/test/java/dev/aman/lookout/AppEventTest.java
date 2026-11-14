package dev.aman.lookout;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AppEventTest {
    @Test
    public void serialisesPackageAndTimestamp() {
        assertEquals("{\"package\":\"com.example.tv\",\"timestamp\":42}",
                new AppEvent("com.example.tv", 42).toJson());
    }

    @Test
    public void escapesQuotesBackslashesAndControlChars() {
        assertEquals("{\"package\":\"a\\\"b\\\\c\\u000ad\",\"timestamp\":1}",
                new AppEvent("a\"b\\c\nd", 1).toJson());
    }
}
