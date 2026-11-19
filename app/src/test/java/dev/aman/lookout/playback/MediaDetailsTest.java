package dev.aman.lookout.playback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import dev.aman.lookout.event.JsonFields;

import org.junit.Test;

public class MediaDetailsTest {
    private static String json(MediaDetails details) {
        JsonFields fields = new JsonFields();
        details.addTo(fields);
        return fields.compact();
    }

    @Test
    public void nothingKnownAddsNothing() {
        assertEquals("{}", json(MediaDetails.NONE));
    }

    @Test
    public void blankTextYearAndDurationAreLeftOut() {
        assertEquals("{}", json(MediaDetails.builder().title(" ").artist("").album(null).year(0).durationMillis(-1)
                .build()));
    }

    @Test
    public void aDurationUnderASecondIsLeftOut() {
        assertEquals("{}", json(MediaDetails.builder().durationMillis(999).build()));
    }

    @Test
    public void longTextIsCut() {
        String cut = json(MediaDetails.builder().title("x".repeat(MediaDetails.MAX_TEXT + 50)).build());
        assertEquals("{\"title\":\"" + "x".repeat(MediaDetails.MAX_TEXT) + "\"}", cut);
    }

    @Test
    public void textIsNeverCutInsideACharacterMadeOfTwoUnits() {
        String emoji = new String(Character.toChars(0x1F3B5));
        String text = "x".repeat(MediaDetails.MAX_TEXT - 1) + emoji;
        assertEquals("{\"title\":\"" + "x".repeat(MediaDetails.MAX_TEXT - 1) + "\"}",
                json(MediaDetails.builder().title(text).build()));
    }

    @Test
    public void artworkIsTheFirstWebAddressAndNeverALocalOne() {
        assertEquals("{\"artwork\":\"http://example.com/b.png\"}",
                json(MediaDetails.builder().artwork(null, "content://x/1", "http://example.com/b.png", "https://c")
                        .build()));
        assertEquals("{}", json(MediaDetails.builder().artwork("content://x/1", "file:///sdcard/a.png").build()));
    }

    @Test
    public void aTooLongArtworkAddressIsPassedOverNotCut() {
        String address = "https://e.com/" + "a".repeat(MediaDetails.MAX_ADDRESS);
        assertEquals("{}", json(MediaDetails.builder().artwork(address).build()));
        assertEquals("{\"artwork\":\"HTTPS://ok\"}",
                json(MediaDetails.builder().artwork(address, "HTTPS://ok").build()));
    }

    @Test
    public void detailsAreEqualOnlyWhenEveryFieldIs() {
        assertEquals(MediaDetails.builder().title("a").year(1).build(),
                MediaDetails.builder().title("a").year(1).build());
        assertNotEquals(MediaDetails.builder().title("a").build(),
                MediaDetails.builder().title("a").genre("g").build());
        assertNotEquals(MediaDetails.builder().durationMillis(1_000).build(), MediaDetails.NONE);
    }
}
