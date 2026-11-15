package dev.aman.lookout.playback;

import dev.aman.lookout.event.JsonFields;

import java.util.Objects;

/**
 * What an app says about the media it plays, using the standard Android metadata only. Text is capped, and an
 * artwork address is kept only when it is a web address, since anything else means nothing to a webhook.
 */
public final class MediaDetails {
    /** Nothing known. */
    public static final MediaDetails NONE = builder().build();

    static final int MAX_TEXT = 200;
    static final int MAX_ADDRESS = 2_048;

    private final String title;
    private final String artist;
    private final String album;
    private final String albumArtist;
    private final String subtitle;
    private final String description;
    private final String genre;
    private final String artwork;
    private final long year;
    private final long durationSeconds;

    private MediaDetails(Builder b) {
        title = b.title;
        artist = b.artist;
        album = b.album;
        albumArtist = b.albumArtist;
        subtitle = b.subtitle;
        description = b.description;
        genre = b.genre;
        artwork = b.artwork;
        year = b.year;
        durationSeconds = b.durationSeconds;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Adds what is known, leaving out the rest. */
    void addTo(JsonFields fields) {
        fields.optionalString("title", title)
                .optionalString("artist", artist)
                .optionalString("album", album)
                .optionalString("album_artist", albumArtist)
                .optionalString("subtitle", subtitle)
                .optionalString("description", description)
                .optionalString("genre", genre);
        if (year > 0) fields.number("year", year);
        if (durationSeconds > 0) fields.number("duration", durationSeconds);
        fields.optionalString("artwork", artwork);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MediaDetails)) return false;
        MediaDetails other = (MediaDetails) o;
        return year == other.year && durationSeconds == other.durationSeconds
                && Objects.equals(title, other.title) && Objects.equals(artist, other.artist)
                && Objects.equals(album, other.album) && Objects.equals(albumArtist, other.albumArtist)
                && Objects.equals(subtitle, other.subtitle) && Objects.equals(description, other.description)
                && Objects.equals(genre, other.genre) && Objects.equals(artwork, other.artwork);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, artist, album, albumArtist, subtitle, description, genre, artwork, year,
                durationSeconds);
    }

    /** Every setter accepts null or blank for "not given". */
    public static final class Builder {
        private String title;
        private String artist;
        private String album;
        private String albumArtist;
        private String subtitle;
        private String description;
        private String genre;
        private String artwork;
        private long year;
        private long durationSeconds;

        private Builder() {}

        public Builder title(String value) {
            title = text(value);
            return this;
        }

        public Builder artist(String value) {
            artist = text(value);
            return this;
        }

        public Builder album(String value) {
            album = text(value);
            return this;
        }

        public Builder albumArtist(String value) {
            albumArtist = text(value);
            return this;
        }

        public Builder subtitle(String value) {
            subtitle = text(value);
            return this;
        }

        public Builder description(String value) {
            description = text(value);
            return this;
        }

        public Builder genre(String value) {
            genre = text(value);
            return this;
        }

        /** The first web address among the candidates; apps often give a local one first. */
        public Builder artwork(String... addresses) {
            artwork = null;
            for (String address : addresses) {
                // A cut address would point nowhere, so one that is too long is passed over
                if (isWebAddress(address) && address.length() <= MAX_ADDRESS) {
                    artwork = address;
                    break;
                }
            }
            return this;
        }

        private static boolean isWebAddress(String address) {
            return address != null && (address.regionMatches(true, 0, "https://", 0, 8)
                    || address.regionMatches(true, 0, "http://", 0, 7));
        }

        /** @param value the year, or 0 when not given */
        public Builder year(long value) {
            year = value;
            return this;
        }

        /** @param millis the length of the media, or 0 or less when not given */
        public Builder durationMillis(long millis) {
            durationSeconds = millis / 1_000;
            return this;
        }

        public MediaDetails build() {
            return new MediaDetails(this);
        }

        private static String text(String value) {
            return value == null || value.trim().isEmpty() ? null : cap(value, MAX_TEXT);
        }

        // Never ends on half a surrogate pair, which would not be valid text
        private static String cap(String value, int max) {
            if (value.length() <= max) return value;
            int end = Character.isHighSurrogate(value.charAt(max - 1)) ? max - 1 : max;
            return value.substring(0, end);
        }
    }
}
