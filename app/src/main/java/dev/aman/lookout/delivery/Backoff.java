package dev.aman.lookout.delivery;

import java.util.function.DoubleSupplier;

/**
 * How long to wait before retrying after a run of failures. The last step repeats for ever: a current event is never
 * given up on. Each wait is spread by up to a fifth either way, so several TVs behind one webhook do not retry in step.
 */
public final class Backoff {
    private static final double SPREAD = 0.2;

    private final long[] stepsMillis;
    private final DoubleSupplier random;

    /**
     * @param stepsMillis the wait after the first, second, … failure; the last one repeats
     * @param random      uniform in [0, 1); injectable so tests get exact waits
     */
    public Backoff(long[] stepsMillis, DoubleSupplier random) {
        if (stepsMillis.length == 0) throw new IllegalArgumentException("Needs at least one step");
        this.stepsMillis = stepsMillis.clone();
        this.random = random;
    }

    /** @param failures how many attempts in a row have failed, at least one */
    public long delayAfter(int failures) {
        long step = stepsMillis[Math.min(Math.max(failures, 1), stepsMillis.length) - 1];
        return Math.round(step * (1 - SPREAD + 2 * SPREAD * random.getAsDouble()));
    }
}
