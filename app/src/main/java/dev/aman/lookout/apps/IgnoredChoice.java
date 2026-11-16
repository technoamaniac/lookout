package dev.aman.lookout.apps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The state of the ignored-apps list while it is open: which apps are ticked, laid out in sections. Rows never
 * move when ticked or cleared, so a press does not shift the list under the D-pad.
 */
public final class IgnoredChoice {
    public enum Section { HOME_SCREENS, APPS, OTHER, BUILT_IN }

    private static final Comparator<InstalledApp> BY_LABEL = Comparator
            .comparing((InstalledApp app) -> app.label().toLowerCase(Locale.ROOT))
            .thenComparing(InstalledApp::packageName);

    private final Set<String> ignored;
    private final Set<String> reported;
    private final Set<String> builtInPackages = new LinkedHashSet<>();
    private final Map<String, String> labels = new HashMap<>();
    private final List<Entry> layout = new ArrayList<>();

    /**
     * @param apps     what the TV has
     * @param builtIns packages ignored by default, which are ticked until the user clears them; kept out of the
     *                 other sections
     * @param ignored  the packages already ignored, in order; any not in {@code apps} are listed under
     *                 {@link Section#OTHER}, so a package sent from a computer, or one that has no launcher entry,
     *                 can still be seen and cleared
     * @param reported the built-ins the user already cleared
     */
    public IgnoredChoice(Collection<InstalledApp> apps, Collection<InstalledApp> builtIns,
                         Collection<String> ignored, Collection<String> reported) {
        for (InstalledApp builtIn : builtIns) builtInPackages.add(builtIn.packageName());
        // A built-in is decided by its own tick, so an explicit entry for one would only be a second, hidden switch
        this.ignored = new LinkedHashSet<>(ignored);
        this.ignored.removeAll(builtInPackages);
        this.reported = new LinkedHashSet<>(reported);
        this.reported.retainAll(builtInPackages);
        List<InstalledApp> homes = new ArrayList<>();
        List<InstalledApp> others = new ArrayList<>();
        Set<String> installed = new LinkedHashSet<>(builtInPackages);
        for (InstalledApp app : apps) {
            if (builtInPackages.contains(app.packageName())) continue;
            installed.add(app.packageName());
            labels.put(app.packageName(), app.label());
            (app.isHomeScreen() ? homes : others).add(app);
        }
        Collections.sort(homes, BY_LABEL);
        Collections.sort(others, BY_LABEL);
        addSection(Section.HOME_SCREENS, homes);
        addSection(Section.APPS, others);
        List<InstalledApp> unlisted = new ArrayList<>();
        for (String pkg : this.ignored) {
            if (!installed.contains(pkg)) unlisted.add(new InstalledApp(pkg, pkg, false));
        }
        addSection(Section.OTHER, unlisted);
        List<InstalledApp> defaults = new ArrayList<>(builtIns);
        Collections.sort(defaults, BY_LABEL);
        for (InstalledApp builtIn : defaults) labels.put(builtIn.packageName(), builtIn.label());
        addSection(Section.BUILT_IN, defaults);
    }

    private void addSection(Section section, List<InstalledApp> apps) {
        if (apps.isEmpty()) return;
        layout.add(Entry.header(section));
        for (InstalledApp app : apps) layout.add(Entry.app(section, app.label(), app.packageName()));
    }

    /** The rows top to bottom, each app with whether it is ticked right now. */
    public List<Entry> entries() {
        List<Entry> entries = new ArrayList<>();
        for (Entry entry : layout) {
            entries.add(entry.isHeader() ? entry : entry.withTicked(isTicked(entry.packageName())));
        }
        return entries;
    }

    private boolean isTicked(String packageName) {
        return builtInPackages.contains(packageName) ? !reported.contains(packageName) : ignored.contains(packageName);
    }

    /** Ticks the package if it is clear, clears it if it is ticked. */
    public void toggle(String packageName) {
        // A built-in is ticked while it is absent from the reported set, so the two sets work in opposite senses
        Set<String> chosen = builtInPackages.contains(packageName) ? reported : ignored;
        if (!chosen.remove(packageName)) chosen.add(packageName);
    }

    /** How many rows are ticked now, built-ins included. */
    public int tickedCount() {
        int count = ignored.size();
        for (String pkg : builtInPackages) {
            if (!reported.contains(pkg)) count++;
        }
        return count;
    }

    /** The built-ins the user cleared, to be reported although they are ignored by default. */
    public List<String> reportedPackages() {
        return new ArrayList<>(reported);
    }

    /** What to save as ignored, built-ins left out: earlier choices first, then new ones in the order they were ticked. */
    public List<String> ignoredPackages() {
        return new ArrayList<>(ignored);
    }

    /** The ignored apps' names for a summary, falling back to the package for one the TV does not list. */
    public List<String> ignoredLabels() {
        List<String> names = new ArrayList<>();
        for (String pkg : ignored) names.add(labels.getOrDefault(pkg, pkg));
        return names;
    }

    /** One row: a section heading, or an app. */
    public static final class Entry {
        private final Section section;
        private final String label;
        private final String packageName;
        private final boolean ticked;

        private Entry(Section section, String label, String packageName, boolean ticked) {
            this.section = section;
            this.label = label;
            this.packageName = packageName;
            this.ticked = ticked;
        }

        static Entry header(Section section) {
            return new Entry(section, null, null, false);
        }

        static Entry app(Section section, String label, String packageName) {
            return new Entry(section, label, packageName, false);
        }

        Entry withTicked(boolean ticked) {
            return new Entry(section, label, packageName, ticked);
        }

        public boolean isHeader() {
            return packageName == null;
        }

        public Section section() {
            return section;
        }

        public String label() {
            return label;
        }

        public String packageName() {
            return packageName;
        }

        public boolean isTicked() {
            return ticked;
        }
    }
}
