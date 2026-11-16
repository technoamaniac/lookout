package dev.aman.lookout.foreground;

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

    /**
     * @param ignored   packages the user ignores
     * @param byDefault packages ignored unless the user says otherwise: keyboards and system chrome raise window
     *                  events over the real foreground app
     * @param reported  packages from {@code byDefault} that the user chose to report anyway
     */
    public static PackageFilter of(Collection<String> ignored, Collection<String> byDefault,
                                   Collection<String> reported) {
        Set<String> all = new HashSet<>(ignored);
        for (String pkg : byDefault) {
            if (!reported.contains(pkg)) all.add(pkg);
        }
        return new PackageFilter(all);
    }

    public boolean accepts(String packageName) {
        return !ignored.contains(packageName);
    }
}
