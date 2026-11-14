package dev.aman.lookout;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class RetryingDispatcherTest {
    private static final AppEvent EVENT = new AppEvent("com.example.a", 1);

    private final List<Long> sleeps = new ArrayList<>();
    private int attempts;

    private RetryingDispatcher dispatcher(EventSink sink, boolean relevant) {
        return new RetryingDispatcher(sink, Runnable::run, new long[] {0, 100, 200},
                sleeps::add, e -> relevant);
    }

    @Test
    public void stopsAfterFirstSuccess() {
        dispatcher(e -> { attempts++; return true; }, true).dispatch(EVENT);
        assertEquals(1, attempts);
        assertEquals(0, sleeps.size());
    }

    @Test
    public void retriesWithBackoffUntilSuccess() {
        dispatcher(e -> ++attempts == 3, true).dispatch(EVENT);
        assertEquals(3, attempts);
        assertEquals(List.of(100L, 200L), sleeps);
    }

    @Test
    public void givesUpAfterTheLastBackoffStep() {
        dispatcher(e -> { attempts++; return false; }, true).dispatch(EVENT);
        assertEquals(3, attempts);
    }

    @Test
    public void dropsEventsThatHaveBeenSuperseded() {
        dispatcher(e -> { attempts++; return true; }, false).dispatch(EVENT);
        assertEquals(0, attempts);
    }
}
