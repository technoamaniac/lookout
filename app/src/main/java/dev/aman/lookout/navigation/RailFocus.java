package dev.aman.lookout.navigation;

/**
 * Where the D-pad goes between a page and the side rail. Android's own focus search picks whichever rail item is
 * nearest, which could be the Updates tab (switching the page) or the Enabled switch. This sends Left from a page
 * to the selected tab and keeps a page's own movement. Up and Down never leave a page; the navigator keeps them there.
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

    private RailFocus() {}

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
