package dev.aman.lookout.delivery;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BackoffTest {
    private final long[] steps = {1_000, 10_000, 60_000};

    @Test
    public void followsTheStepsThenRepeatsTheLast() {
        Backoff backoff = new Backoff(steps, () -> 0.5);
        assertEquals(1_000, backoff.delayAfter(1));
        assertEquals(10_000, backoff.delayAfter(2));
        assertEquals(60_000, backoff.delayAfter(3));
        assertEquals(60_000, backoff.delayAfter(50));
    }

    @Test
    public void spreadsEachWaitByUpToAFifth() {
        assertEquals(8_000, new Backoff(steps, () -> 0.0).delayAfter(2));
        assertEquals(12_000, new Backoff(steps, () -> 1.0).delayAfter(2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void needsAStep() {
        new Backoff(new long[0], () -> 0.5);
    }
}
