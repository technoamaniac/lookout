package dev.aman.lookout.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VersionTest {
    private static void assertOrdered(String lower, String higher) {
        assertTrue(lower + " < " + higher, Version.parse(lower).compareTo(Version.parse(higher)) < 0);
        assertTrue(higher + " > " + lower, Version.parse(higher).compareTo(Version.parse(lower)) > 0);
    }

    @Test
    public void ordersByNumberNotByText() {
        assertOrdered("1.9.0", "1.10.0");
        assertOrdered("1.2.9", "1.2.10");
        assertOrdered("1.99.99", "2.0.0");
    }

    @Test
    public void aPreReleaseComesBeforeItsRelease() {
        assertOrdered("1.0.0-rc.1", "1.0.0");
        assertOrdered("0.0.0-dev", "0.0.0");
    }

    @Test
    public void ordersPreReleaseIdentifiersPerSemver() {
        assertOrdered("1.0.0-alpha", "1.0.0-alpha.1");
        assertOrdered("1.0.0-alpha.2", "1.0.0-alpha.10");
        assertOrdered("1.0.0-1", "1.0.0-alpha");
        assertOrdered("1.0.0-alpha", "1.0.0-beta");
    }

    @Test
    public void equalVersionsAreEqual() {
        assertEquals(Version.parse("1.2.3"), Version.parse("1.2.3"));
        assertEquals(Version.parse("1.2.3").hashCode(), Version.parse("1.2.3").hashCode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsTextThatIsNotSemver() {
        Version.parse("1.2");
    }

    @Test
    public void printsAsParsed() {
        assertEquals("1.2.3-rc.1", Version.parse("1.2.3-rc.1").toString());
    }
}
