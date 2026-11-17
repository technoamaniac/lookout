package dev.aman.lookout.update;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Turns the HTML GitHub renders for a release's notes into plain text a TV screen can show. */
public final class ReleaseNotes {
    private static final Pattern ENTITY = Pattern.compile("&(#x[0-9a-fA-F]+|#\\d+|[a-z]+);");
    private static final Pattern HEADING_START = Pattern.compile("<h[1-6][^>]*>");
    private static final Pattern HEADING_END = Pattern.compile("</h[1-6]>");
    private static final Pattern BLOCK_END = Pattern.compile("</(?:p|ul|ol)>");
    private static final Pattern LINE_BREAK = Pattern.compile("<br\\s*/?>");
    private static final Pattern LIST_ITEM = Pattern.compile("<li[^>]*>");
    private static final Pattern ANY_TAG = Pattern.compile("<[^>]*>");
    // The link to the full diff that GitHub appends to every release: a bare version range means nothing on a TV
    private static final Pattern DIFF_LINK = Pattern.compile("(?m)^Full Changelog.*$");
    private static final Pattern SPACE_AROUND_BREAKS = Pattern.compile(" *\n *");
    private static final Pattern BLANK_RUNS = Pattern.compile("\n{3,}");

    private ReleaseNotes() {
    }

    /** @param feedContent the {@code content} of a release's entry in the releases feed, which is escaped HTML */
    static String fromFeedContent(String feedContent) {
        return fromHtml(decode(feedContent));
    }

    /** Headings become their own lines, list items get a bullet, and every other tag and the link list is dropped. */
    static String fromHtml(String html) {
        // Line breaks in the source are layout only; the tags below decide where lines break
        String text = html.replace('\n', ' ');
        text = HEADING_START.matcher(text).replaceAll("\n\n");
        text = HEADING_END.matcher(text).replaceAll("\n");
        text = LIST_ITEM.matcher(text).replaceAll("• ");
        text = text.replace("</li>", "\n");
        text = BLOCK_END.matcher(text).replaceAll("\n\n");
        text = LINE_BREAK.matcher(text).replaceAll("\n");
        text = ANY_TAG.matcher(text).replaceAll("");
        // The spaces that stood for the source's line breaks would otherwise indent every line
        text = SPACE_AROUND_BREAKS.matcher(decode(text)).replaceAll("\n");
        text = DIFF_LINK.matcher(text).replaceAll("");
        return BLANK_RUNS.matcher(text).replaceAll("\n\n").trim();
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
