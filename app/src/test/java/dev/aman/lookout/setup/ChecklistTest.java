package dev.aman.lookout.setup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class ChecklistTest {
    private static Checklist with(Permission... granted) {
        Set<Permission> set = EnumSet.noneOf(Permission.class);
        set.addAll(Arrays.asList(granted));
        return Checklist.read(set::contains);
    }

    @Test
    public void listsEveryPermissionInOrder() {
        List<Checklist.Row> rows = with().rows();

        assertEquals(Permission.ACCESSIBILITY, rows.get(0).permission());
        assertEquals(Permission.MEDIA_ACCESS, rows.get(1).permission());
        assertEquals(Permission.INSTALL_UPDATES, rows.get(2).permission());
    }

    @Test
    public void marksWhichAreGranted() {
        List<Checklist.Row> rows = with(Permission.ACCESSIBILITY, Permission.INSTALL_UPDATES).rows();

        assertTrue(rows.get(0).granted());
        assertFalse(rows.get(1).granted());
        assertTrue(rows.get(2).granted());
    }

    @Test
    public void putsFocusOnTheFirstPermissionStillMissing() {
        assertEquals(Permission.MEDIA_ACCESS, with(Permission.ACCESSIBILITY).firstMissing().get());
        assertEquals(Permission.ACCESSIBILITY, with().firstMissing().get());
    }

    @Test
    public void hasNothingMissingWhenAllAreGranted() {
        assertFalse(with(Permission.values()).firstMissing().isPresent());
    }

    @Test
    public void needsOnlyAccessibility() {
        assertTrue(Permission.ACCESSIBILITY.isRequired());
        assertFalse(Permission.MEDIA_ACCESS.isRequired());
        assertFalse(Permission.INSTALL_UPDATES.isRequired());
    }
}
