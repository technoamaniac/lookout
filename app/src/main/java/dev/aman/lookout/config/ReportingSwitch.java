package dev.aman.lookout.config;

/** The user's on/off switch for sending reports, kept apart from the webhook form because it applies at once. */
public interface ReportingSwitch {
    boolean isReportingOn();

    void setReportingOn(boolean on);
}
