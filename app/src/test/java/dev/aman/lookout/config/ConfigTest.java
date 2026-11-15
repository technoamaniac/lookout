package dev.aman.lookout.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class ConfigTest {
    @Test
    public void blankUrlMeansNotConfigured() {
        assertTrue(Config.parse("  ", "", false).webhookUrl().isEmpty());
        assertTrue(Config.parse(null, null, false).webhookUrl().isEmpty());
    }

    @Test
    public void theUrlPrefixAloneMeansNotConfigured() {
        assertTrue(Config.parse(Config.URL_PREFIX, "", false).webhookUrl().isEmpty());
        assertTrue(Config.parse(" HTTPS:// ", "", false).webhookUrl().isEmpty());
    }

    @Test
    public void configsWithTheSameNormalisedValuesAreEqual() {
        assertEquals(Config.parse("https://example.com/h ", "a, b, a", true),
                Config.parse("https://example.com/h", "a,b", true));
        assertNotEquals(Config.parse("https://example.com/h", "a", true),
                Config.parse("https://example.com/h", "a", false));
    }

    @Test
    public void acceptsAndTrimsAnHttpsUrl() {
        assertEquals("https://example.com/h",
                Config.parse("  https://example.com/h ", "", false).webhookUrlText());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsCleartextHttp() {
        Config.parse("http://example.com/h", "", false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMalformedUrl() {
        Config.parse("not a url", "", false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUrlWithIllegalCharacters() {
        Config.parse("https://example.com/my hook", "", false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUrlWithoutHost() {
        Config.parse("https:///path", "", false);
    }

    @Test
    public void parsesIgnoredPackagesTrimmedAndDeduplicated() {
        Config config = Config.parse("", " a.b , c.d,, a.b ", false);
        assertEquals(List.of("a.b", "c.d"), config.ignoredPackages());
        assertEquals("a.b, c.d", config.ignoredPackagesText());
    }

    @Test
    public void keepsTheDeviceNameChoice() {
        assertTrue(Config.parse("", "", true).sendDeviceName());
        assertFalse(Config.parse("", "", false).sendDeviceName());
    }
}
