package dev.aman.lookout.update;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A semantic version (major.minor.patch with an optional pre-release), ordered per semver. */
public final class Version implements Comparable<Version> {
    private static final Pattern SEMVER = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z.-]+))?");

    private final String text;
    private final long[] core;
    private final List<String> preRelease;

    private Version(String text, long[] core, List<String> preRelease) {
        this.text = text;
        this.core = core;
        this.preRelease = preRelease;
    }

    /** @throws IllegalArgumentException if the text is not a semantic version */
    public static Version parse(String text) {
        Matcher m = SEMVER.matcher(text);
        if (!m.matches()) throw new IllegalArgumentException("Not a semantic version: " + text);
        long[] core = {Long.parseLong(m.group(1)), Long.parseLong(m.group(2)), Long.parseLong(m.group(3))};
        List<String> pre = m.group(4) == null ? List.of() : Arrays.asList(m.group(4).split("\\."));
        return new Version(text, core, pre);
    }

    @Override
    public int compareTo(Version other) {
        for (int i = 0; i < core.length; i++) {
            int byPart = Long.compare(core[i], other.core[i]);
            if (byPart != 0) return byPart;
        }
        // 1.0.0-rc.1 comes before 1.0.0
        if (preRelease.isEmpty() || other.preRelease.isEmpty()) {
            return Boolean.compare(preRelease.isEmpty(), other.preRelease.isEmpty());
        }
        for (int i = 0; i < Math.min(preRelease.size(), other.preRelease.size()); i++) {
            int byId = compareIdentifiers(preRelease.get(i), other.preRelease.get(i));
            if (byId != 0) return byId;
        }
        return Integer.compare(preRelease.size(), other.preRelease.size());
    }

    // Numeric identifiers rank below alphanumeric ones and compare by value
    private static int compareIdentifiers(String a, String b) {
        boolean aNumeric = a.chars().allMatch(Character::isDigit);
        boolean bNumeric = b.chars().allMatch(Character::isDigit);
        if (aNumeric && bNumeric) {
            return a.length() != b.length() ? Integer.compare(a.length(), b.length()) : a.compareTo(b);
        }
        if (aNumeric != bNumeric) return aNumeric ? -1 : 1;
        return a.compareTo(b);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Version && compareTo((Version) o) == 0;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(core) * 31 + preRelease.hashCode();
    }

    @Override
    public String toString() {
        return text;
    }
}
