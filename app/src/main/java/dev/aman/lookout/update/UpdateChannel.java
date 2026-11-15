package dev.aman.lookout.update;

/** Which kind of release the user wants the updater to offer. */
public interface UpdateChannel {
    /** True when release candidates are offered too; stable releases only otherwise. */
    boolean includesPreReleases();

    void setIncludesPreReleases(boolean include);
}
