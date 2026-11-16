package dev.aman.lookout.foreground;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class PackageFilterTest {
    private static final List<String> DEFAULTS = List.of("com.android.systemui", "com.example.keyboard");

    @Test
    public void ignoresWhatTheUserIgnoresAndTheDefaults() {
        PackageFilter filter = PackageFilter.of(List.of("com.example.launcher"), DEFAULTS, Collections.emptyList());

        assertFalse(filter.accepts("com.example.launcher"));
        assertFalse(filter.accepts("com.android.systemui"));
        assertFalse(filter.accepts("com.example.keyboard"));
        assertTrue(filter.accepts("com.example.player"));
    }

    @Test
    public void reportsADefaultTheUserClearedAndNothingElse() {
        PackageFilter filter = PackageFilter.of(Collections.emptyList(), DEFAULTS, List.of("com.example.keyboard"));

        assertTrue(filter.accepts("com.example.keyboard"));
        assertFalse(filter.accepts("com.android.systemui"));
    }

    @Test
    public void stillIgnoresADefaultTheUserAlsoIgnoredByName() {
        PackageFilter filter = PackageFilter.of(List.of("com.example.keyboard"), DEFAULTS,
                List.of("com.example.keyboard"));

        assertFalse(filter.accepts("com.example.keyboard"));
    }
}
