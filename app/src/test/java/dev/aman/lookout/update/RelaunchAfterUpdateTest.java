package dev.aman.lookout.update;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RelaunchAfterUpdateTest {
    private static final class MemoryStore implements RelaunchAfterUpdate.Store {
        long requestedAt;

        @Override
        public long relaunchRequestedAtMillis() {
            return requestedAt;
        }

        @Override
        public void setRelaunchRequestedAtMillis(long millis) {
            requestedAt = millis;
        }
    }

    private final RelaunchAfterUpdate relaunch = new RelaunchAfterUpdate(new MemoryStore());

    @Test
    public void reopensOnceAfterARequest() {
        relaunch.request(1_000);
        assertTrue(relaunch.consume(31_000));
        assertFalse(relaunch.consume(31_000));
    }

    @Test
    public void doesNothingWithoutARequest() {
        assertFalse(relaunch.consume(1_000));
    }

    @Test
    public void aCancelledRequestIsForgotten() {
        relaunch.request(1_000);
        relaunch.cancel();
        assertFalse(relaunch.consume(2_000));
    }

    @Test
    public void anOldRequestExpiresAndIsCleared() {
        relaunch.request(1_000);
        assertFalse(relaunch.consume(1_000 + RelaunchAfterUpdate.EXPIRES_AFTER_MILLIS + 1));
        assertFalse(relaunch.consume(2_000));
    }

    @Test
    public void aClockSetBackCountsAsExpired() {
        relaunch.request(10_000);
        assertFalse(relaunch.consume(5_000));
    }
}
