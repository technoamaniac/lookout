package dev.aman.lookout.apps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class IgnoredChoiceTest {
    private static final InstalledApp LAUNCHER = new InstalledApp("Projectivy Launcher", "com.spocky.projengmenu", true);
    private static final InstalledApp TV_HOME = new InstalledApp("Google TV Home", "com.google.android.apps.tv.launcherx", true);
    private static final InstalledApp SMARTTUBE = new InstalledApp("SmartTube", "org.smarttube.stable", false);
    private static final InstalledApp JELLYFIN = new InstalledApp("Jellyfin", "org.jellyfin.androidtv", false);
    private static final InstalledApp LOWER = new InstalledApp("apple tv", "com.apple.atve", false);

    @Test
    public void listsHomeScreensThenAppsAlphabeticallyIgnoringCase() {
        IgnoredChoice choice = choiceOf(
                Arrays.asList(SMARTTUBE, LAUNCHER, LOWER, JELLYFIN, TV_HOME), Collections.emptyList());

        assertEquals(Arrays.asList("HOME_SCREENS", "Google TV Home", "Projectivy Launcher",
                "APPS", "apple tv", "Jellyfin", "SmartTube"), describe(choice.entries()));
    }

    @Test
    public void ticksTheIgnoredAppsOnly() {
        IgnoredChoice choice = choiceOf(Arrays.asList(SMARTTUBE, JELLYFIN), Arrays.asList(SMARTTUBE.packageName()));

        for (IgnoredChoice.Entry entry : choice.entries()) {
            if (entry.isHeader()) continue;
            assertEquals(entry.packageName().equals(SMARTTUBE.packageName()), entry.isTicked());
        }
    }

    @Test
    public void listsAnIgnoredPackageTheTvDoesNotListUnderOther() {
        IgnoredChoice choice = choiceOf(Collections.singletonList(SMARTTUBE),
                Arrays.asList("com.google.android.apps.tv.dreamx", SMARTTUBE.packageName()));

        assertEquals(Arrays.asList("APPS", "SmartTube", "OTHER", "com.google.android.apps.tv.dreamx"),
                describe(choice.entries()));
        assertTrue(choice.entries().get(3).isTicked());
    }

    @Test
    public void keepsRowsWhereTheyAreWhenTickedOrCleared() {
        IgnoredChoice choice = choiceOf(Arrays.asList(SMARTTUBE, JELLYFIN), Collections.singletonList("com.gone"));
        List<String> before = describe(choice.entries());

        choice.toggle(JELLYFIN.packageName());
        choice.toggle("com.gone");

        assertEquals(before, describe(choice.entries()));
        assertFalse(choice.entries().get(choice.entries().size() - 1).isTicked());
    }

    @Test
    public void saysWhichPackagesAreIgnoredWithEarlierChoicesFirst() {
        IgnoredChoice choice = choiceOf(Arrays.asList(SMARTTUBE, JELLYFIN, LAUNCHER),
                Arrays.asList(LAUNCHER.packageName(), "com.gone"));

        choice.toggle(SMARTTUBE.packageName());
        choice.toggle(LAUNCHER.packageName());
        choice.toggle(LAUNCHER.packageName());

        assertEquals(Arrays.asList("com.gone", SMARTTUBE.packageName(), LAUNCHER.packageName()),
                choice.ignoredPackages());
    }

    @Test
    public void showsNoHeadingForAnEmptySection() {
        IgnoredChoice choice = choiceOf(Collections.singletonList(SMARTTUBE), Collections.emptyList());

        assertEquals(Arrays.asList("APPS", "SmartTube"), describe(choice.entries()));
    }

    private static final InstalledApp SYSTEM_UI = new InstalledApp("System UI", "com.android.systemui", false);
    private static final InstalledApp KEYBOARD = new InstalledApp("Gboard", "com.google.android.inputmethod.latin", false);

    @Test
    public void listsBuiltInsLastAndTicksThem() {
        IgnoredChoice choice = new IgnoredChoice(Collections.singletonList(SMARTTUBE),
                Arrays.asList(SYSTEM_UI, KEYBOARD), Collections.emptyList(), Collections.emptyList());

        assertEquals(Arrays.asList("APPS", "SmartTube", "BUILT_IN", "Gboard", "System UI"), describe(choice.entries()));
        assertTrue(choice.entries().get(3).isTicked());
        assertTrue(choice.entries().get(4).isTicked());
        assertEquals(2, choice.tickedCount());
    }

    @Test
    public void clearingABuiltInMakesItReportedNotIgnored() {
        IgnoredChoice choice = new IgnoredChoice(Collections.emptyList(), Arrays.asList(SYSTEM_UI, KEYBOARD),
                Collections.emptyList(), Collections.emptyList());

        choice.toggle(KEYBOARD.packageName());

        assertEquals(Collections.singletonList(KEYBOARD.packageName()), choice.reportedPackages());
        assertTrue(choice.ignoredPackages().isEmpty());
        assertEquals(1, choice.tickedCount());
        assertFalse(choice.entries().get(1).isTicked());
    }

    @Test
    public void tickingAClearedBuiltInAgainStopsReportingIt() {
        IgnoredChoice choice = new IgnoredChoice(Collections.emptyList(), Collections.singletonList(KEYBOARD),
                Collections.emptyList(), Collections.singletonList(KEYBOARD.packageName()));
        assertFalse(choice.entries().get(1).isTicked());

        choice.toggle(KEYBOARD.packageName());

        assertTrue(choice.reportedPackages().isEmpty());
        assertTrue(choice.entries().get(1).isTicked());
    }

    @Test
    public void listsABuiltInOnlyOnceEvenWhenTheTvListsItAsAnApp() {
        IgnoredChoice choice = new IgnoredChoice(Arrays.asList(SMARTTUBE, KEYBOARD),
                Collections.singletonList(KEYBOARD), Collections.emptyList(), Collections.emptyList());

        assertEquals(Arrays.asList("APPS", "SmartTube", "BUILT_IN", "Gboard"), describe(choice.entries()));
    }

    @Test
    public void dropsAnExplicitEntryForABuiltInSoItHasOneSwitch() {
        IgnoredChoice choice = new IgnoredChoice(Collections.emptyList(), Collections.singletonList(KEYBOARD),
                Arrays.asList(KEYBOARD.packageName(), "com.gone"), Collections.emptyList());

        assertEquals(Collections.singletonList("com.gone"), choice.ignoredPackages());
    }

    @Test
    public void ignoresAReportedPackageThatIsNotABuiltIn() {
        IgnoredChoice choice = new IgnoredChoice(Collections.singletonList(SMARTTUBE), Collections.emptyList(),
                Collections.emptyList(), Collections.singletonList("com.example.stale"));

        assertTrue(choice.reportedPackages().isEmpty());
    }

    private static IgnoredChoice choiceOf(Collection<InstalledApp> apps, Collection<String> ignored) {
        return new IgnoredChoice(apps, Collections.emptyList(), ignored, Collections.emptyList());
    }

    private static List<String> describe(List<IgnoredChoice.Entry> entries) {
        List<String> lines = new ArrayList<>();
        for (IgnoredChoice.Entry entry : entries) {
            lines.add(entry.isHeader() ? entry.section().name() : entry.label());
        }
        return lines;
    }
}
