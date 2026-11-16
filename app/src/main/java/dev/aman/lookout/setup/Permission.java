package dev.aman.lookout.setup;

/** A permission Lookout asks the user to grant in the TV's system settings. */
public enum Permission {
    /** Lets Lookout see which app is in front; without it nothing is reported. */
    ACCESSIBILITY(true),
    /** Lets Lookout read the active media sessions, for playback reports. */
    MEDIA_ACCESS(false),
    /** Lets Lookout install its own updates. */
    INSTALL_UPDATES(false);

    private final boolean required;

    Permission(boolean required) {
        this.required = required;
    }

    /** Whether Lookout does nothing at all without it, as opposed to losing one feature. */
    public boolean isRequired() {
        return required;
    }
}
