package dev.aman.lookout.update;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReleaseNotesTest {
    private static String escaped(String html) {
        return html.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    @Test
    public void keepsTheFormattingForTheRendererToDraw() {
        String html = "<h3>Added</h3>\n<ul>\n<li>Press <strong>Save</strong></li>\n</ul>";
        assertEquals(html, ReleaseNotes.fromFeedContent(escaped(html)));
    }

    @Test
    public void dropsTheFullChangelogLink() {
        String html = "<ul><li>One</li></ul>\n<p><strong>Full Changelog</strong>: <a href=\"x\"><tt>v1...v2</tt></a></p>";
        assertEquals("<ul><li>One</li></ul>", ReleaseNotes.fromFeedContent(escaped(html)));
    }

    @Test
    public void keepsTheTextOfALinkButNotTheLink() {
        assertEquals("<p>See the guide.</p>",
                ReleaseNotes.fromFeedContent(escaped("<p>See <a class=\"x\" href=\"https://e.com\">the guide</a>.</p>")));
    }

    @Test
    public void drawsCodeInMonospace() {
        assertEquals("<p>Run <tt>adb</tt></p>", ReleaseNotes.fromFeedContent(escaped("<p>Run <code>adb</code></p>")));
    }

    @Test
    public void unescapesTheFeedOnlyOnce() {
        // The feed escapes the HTML, whose own text is escaped again: the renderer decodes the second layer
        assertEquals("<p>a &lt;b&gt; &amp; c</p>", ReleaseNotes.fromFeedContent("&lt;p&gt;a &amp;lt;b&amp;gt; &amp;amp; c&lt;/p&gt;"));
    }

    @Test
    public void notesWithNoTextGiveNothing() {
        assertEquals("", ReleaseNotes.fromFeedContent(""));
        assertEquals("", ReleaseNotes.fromFeedContent("&lt;ul&gt;&lt;/ul&gt; "));
    }

    @Test
    public void drawsCodeWithAttributesInMonospace() {
        assertEquals("<pre><tt>adb</tt></pre>",
                ReleaseNotes.fromFeedContent(escaped("<pre><code class=\"sh\">adb</code></pre>")));
    }

    @Test
    public void numericEntitiesBecomeCharacters() {
        assertEquals("<p>It's \u2192 ok</p>", ReleaseNotes.fromFeedContent("&lt;p&gt;It&#39;s &#x2192; ok&lt;/p&gt;"));
    }

    @Test
    public void leavesAnEntityItDoesNotKnowAsItIs() {
        assertEquals("<p>&nbsp; &#99999999999;</p>",
                ReleaseNotes.fromFeedContent("&lt;p&gt;&nbsp; &#99999999999;&lt;/p&gt;"));
    }
}
