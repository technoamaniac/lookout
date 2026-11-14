package dev.aman.lookout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class ConfigTest {
    @Test
    public void blankUrlMeansNotConfigured() {
        assertTrue(Config.parse("  ", "").webhookUrl().isEmpty());
        assertTrue(Config.parse(null, null).webhookUrl().isEmpty());
    }

    @Test
    public void acceptsAndTrimsAnHttpsUrl() {
        assertEquals("https://example.com/h",
                Config.parse("  https://example.com/h ", "").webhookUrlText());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsCleartextHttp() {
        Config.parse("http://example.com/h", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMalformedUrl() {
        Config.parse("not a url", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUrlWithIllegalCharacters() {
        Config.parse("https://example.com/my hook", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUrlWithoutHost() {
        Config.parse("https:///path", "");
    }

    @Test
    public void parsesIgnoredPackagesTrimmedAndDeduplicated() {
        Config config = Config.parse("", " a.b , c.d,, a.b ");
        assertEquals(List.of("a.b", "c.d"), config.ignoredPackages());
        assertEquals("a.b, c.d", config.ignoredPackagesText());
    }
}
