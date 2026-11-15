package dev.aman.lookout.config;

/**
 * Decides what the settings form allows: a new webhook URL can be saved only once a test event has reached it,
 * since the saved URL already passed and the other settings cannot stop delivery.
 */
public final class SaveGate {
    /** What the form allows for one state of its fields. */
    public enum State {
        /** No URL: Lookout has nothing to do without a webhook, so it can be neither tested nor saved. */
        NO_URL,
        /** Nothing differs from the saved settings. */
        UNCHANGED,
        /** A new or invalid URL that must be tested first; a test is also what reports what is wrong with it. */
        NEEDS_TEST,
        /** Changed settings that may be saved. */
        SAVEABLE
    }

    private Config saved;
    // The URL the last test event reached, which may be saved; a later failed test withdraws it
    private String testedUrl;

    public SaveGate(Config saved) {
        this.saved = saved;
    }

    public void onSaved(Config config) {
        saved = config;
    }

    public void onTested(Config config, boolean delivered) {
        testedUrl = delivered ? config.webhookUrlText() : null;
    }

    /** @param current the form's fields, or null when they do not parse */
    public State stateOf(Config current) {
        if (current == null) return State.NEEDS_TEST;
        // Not Optional#isEmpty: that needs API 33 and minSdk is 28
        if (!current.webhookUrl().isPresent()) return State.NO_URL;
        if (current.equals(saved)) return State.UNCHANGED;
        boolean tested = !changesUrl(current) || current.webhookUrlText().equals(testedUrl);
        return tested ? State.SAVEABLE : State.NEEDS_TEST;
    }

    /** True once a test has made a new URL saveable, when the form should point the user to Save. */
    public boolean isNewUrlReadyToSave(Config current) {
        return changesUrl(current) && stateOf(current) == State.SAVEABLE;
    }

    private boolean changesUrl(Config current) {
        return !current.webhookUrlText().equals(saved.webhookUrlText());
    }
}
