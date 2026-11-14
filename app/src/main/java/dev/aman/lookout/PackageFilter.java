package dev.aman.lookout;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Decides which packages count as a user-facing foreground app. */
public final class PackageFilter {
    private final Set<String> ignored;

    public PackageFilter(Collection<String> ignored) {
        this.ignored = Collections.unmodifiableSet(new HashSet<>(ignored));
    }

    public boolean accepts(String packageName) {
        return !ignored.contains(packageName);
    }
}
