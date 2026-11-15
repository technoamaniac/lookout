package dev.aman.lookout.navigation;

/**
 * Where the D-pad goes between a page and the side rail. Android's own focus search picks whichever rail item is
 * nearest, which could be the Updates tab (switching the page) or the Enabled switch. This sends Left from a page
 * to the selected tab and keeps a page's own movement. Up and Down never leave a page; {@link #onVertical} decides how.
 */
public final class RailFocus {
    public enum Key { UP, DOWN, LEFT, RIGHT }

    public enum Area { RAIL, PAGE, OTHER }

    public enum Move {
        /** Let Android handle the key. */
        DEFAULT,
        /** Go to the rail's selected tab. */
        SELECTED_TAB
    }

    /** How Up or Down is carried out from a page. */
    public enum Vertical {
        /** Let Android move the focus. */
        LEAVE_TO_ANDROID,
        /** Focus what Android's search found. */
        FOCUS_FOUND,
        /** Search the page alone, and scroll it when nothing is further. */
        SEARCH_PAGE
    }

    private RailFocus() {}

    /**
     * Up and Down from a page. A text field is moved here even when Android's search finds the right view, because
     * the key would first move the cursor to the end of the text. Anything else is left to Android, except when its
     * search would not stay on the page: the nearest view can be a side menu item rather than the field beside it.
     *
     * @param fromTextField whether the focused view is a text field
     * @param found         where Android's focus search would take it; {@link Area#OTHER} when it finds nothing
     */
    public static Vertical onVertical(boolean fromTextField, Area found) {
        if (found != Area.PAGE) return Vertical.SEARCH_PAGE;
        return fromTextField ? Vertical.FOCUS_FOUND : Vertical.LEAVE_TO_ANDROID;
    }

    /**
     * @param from         where the focus is now
     * @param to           where Android's focus search would take it; {@link Area#OTHER} when it finds nothing
     * @param cursorCanMove whether the focused text field still has room to move its cursor in the key's direction
     */
    public static Move onKey(Key key, Area from, Area to, boolean cursorCanMove) {
        if (from != Area.PAGE || to == Area.PAGE) return Move.DEFAULT;
        return key == Key.LEFT && !cursorCanMove ? Move.SELECTED_TAB : Move.DEFAULT;
    }
}
