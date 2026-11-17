package dev.aman.lookout.update;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tidies the HTML GitHub renders for a release's notes so a TV can show it: the formatting stays, and what a remote
 * cannot use (links, the diff link GitHub appends) goes. Android's own HTML renderer does the drawing.
 */
public final class ReleaseNotes {
    private static final Pattern ENTITY = Pattern.compile("&(#x[0-9a-fA-F]+|#\\d+|[a-z]+);");
    // The link to the full diff that GitHub appends to every release: a bare version range means nothing on a TV
    private static final Pattern DIFF_LINK = Pattern.compile("<p><strong>Full Changelog</strong>.*?</p>", Pattern.DOTALL);
    // Nothing on a TV can follow a link, so only its text is kept
    private static final Pattern LINK = Pattern.compile("<a\\b[^>]*>(.*?)</a>", Pattern.DOTALL);
    private static final Pattern CODE_START = Pattern.compile("<code\\b[^>]*>");
    private static final Pattern ANY_TAG = Pattern.compile("<[^>]*>");

    private ReleaseNotes() {
    }

    /**
     * @param feedContent the {@code content} of a release's entry in the releases feed, which is escaped HTML
     * @return the notes as HTML, or an empty string when there is no text in them
     */
    static String fromFeedContent(String feedContent) {
        String html = decode(feedContent);
        html = DIFF_LINK.matcher(html).replaceAll("");
        html = LINK.matcher(html).replaceAll("$1");
        // Android's renderer draws tt in a monospace face and has no rule for code
        html = CODE_START.matcher(html).replaceAll("<tt>").replace("</code>", "</tt>");
        return ANY_TAG.matcher(html).replaceAll("").trim().isEmpty() ? "" : html.trim();
    }

    // One pass, so "&amp;lt;" becomes "&lt;" and not "<"
    private static String decode(String text) {
        Matcher entity = ENTITY.matcher(text);
        StringBuffer out = new StringBuffer(); // Matcher takes a StringBuilder only from API 34
        while (entity.find()) {
            entity.appendReplacement(out, Matcher.quoteReplacement(valueOf(entity.group(1), entity.group())));
        }
        entity.appendTail(out);
        return out.toString();
    }

    private static String valueOf(String name, String unknown) {
        switch (name) {
            case "lt": return "<";
            case "gt": return ">";
            case "amp": return "&";
            case "quot": return "\"";
            case "apos": return "'";
            default:
                if (name.charAt(0) != '#') return unknown;
                try {
                    boolean hex = name.charAt(1) == 'x';
                    return new String(Character.toChars(Integer.parseInt(name.substring(hex ? 2 : 1), hex ? 16 : 10)));
                } catch (IllegalArgumentException e) {
                    return unknown; // Too large to be a character
                }
        }
    }
}
