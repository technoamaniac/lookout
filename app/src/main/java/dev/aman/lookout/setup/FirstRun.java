package dev.aman.lookout.setup;

/** Decides when the setup screens open by themselves. */
public final class FirstRun {
    private FirstRun() {}

    /**
     * Only for someone who has neither seen setup nor given Lookout a webhook yet, so an install that already works
     * is not interrupted when it updates to the first version that has setup.
     */
    public static boolean isDue(boolean setupSeen, boolean webhookConfigured) {
        return !setupSeen && !webhookConfigured;
    }
}
