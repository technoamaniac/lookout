package dev.aman.lookout.update;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReleaseNotesTest {
    @Test
    public void headingsStandOnTheirOwnLinesAndItemsGetBullets() {
        String html = "<h3>Added</h3>\n<ul>\n<li>One</li>\n<li>Two</li>\n</ul>\n<h3>Fixed</h3>\n<ul>\n<li>Three</li>\n</ul>";
        assertEquals("Added\n• One\n• Two\n\nFixed\n• Three", ReleaseNotes.fromHtml(html));
    }

    @Test
    public void dropsMarkupButKeepsTheText() {
        assertEquals("• Open **Settings** and press <code>", ReleaseNotes.fromHtml(
                "<ul><li>Open <strong>**Settings**</strong> and press <code>&lt;code&gt;</code></li></ul>"));
    }

    @Test
    public void dropsTheFullChangelogLink() {
        String html = "<ul><li>One</li></ul>\n<p><strong>Full Changelog</strong>: <a href=\"x\"><tt>v1...v2</tt></a></p>";
        assertEquals("• One", ReleaseNotes.fromHtml(html));
    }

    @Test
    public void feedContentIsUnescapedOnceBeforeItIsRead() {
        assertEquals("• It's <b> & more", ReleaseNotes.fromFeedContent(
                "&lt;ul&gt;&lt;li&gt;It&amp;#39;s &amp;lt;b&amp;gt; &amp;amp; more&lt;/li&gt;&lt;/ul&gt;"));
    }

    @Test
    public void numericEntitiesBecomeCharacters() {
        assertEquals("It's → ok", ReleaseNotes.fromHtml("<p>It&#39;s &#x2192; ok</p>"));
    }

    @Test
    public void leavesAnEntityItDoesNotKnowAsItIs() {
        assertEquals("&nbsp; &#99999999999;", ReleaseNotes.fromHtml("<p>&nbsp; &#99999999999;</p>"));
    }

    @Test
    public void emptyHtmlGivesEmptyText() {
        assertEquals("", ReleaseNotes.fromHtml(""));
    }
}
