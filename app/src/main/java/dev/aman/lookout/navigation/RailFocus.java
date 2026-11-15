package dev.aman.lookout.navigation;

/**
 * Where the D-pad goes between a page and the side rail. Android's own focus search picks whichever rail item is
 * nearest, which could be the Updates tab (switching the page) or the Enabled switch, and lets Up and Down
 * wander off a page into the rail. This keeps a page's own movement and routes everything that would leave it.
 */
public final class RailFocus {
    public enum Key { UP, DOWN, LEFT, RIGHT }

    public enum Area { RAIL, PAGE, OTHER }

    public enum Move {
        /** Let Android handle the key. */
        DEFAULT,
        /** Go to the rail's selected tab. */
        SELECTED_TAB,
        /** Keep the focus where it is. */
        STAY
    }

    private RailFocus() {}

    /**
     * @param from         where the focus is now
     * @param to           where Android's focus search would take it; {@link Area#OTHER} when it finds nothing
     * @param cursorCanMove whether the focused text field still has room to move its cursor in the key's direction
     */
    public static Move onKey(Key key, Area from, Area to, boolean cursorCanMove) {
        if (from != Area.PAGE || to == Area.PAGE) return Move.DEFAULT;
        switch (key) {
            case LEFT:
                return cursorCanMove ? Move.DEFAULT : Move.SELECTED_TAB;
            case UP:
            case DOWN:
                // Only a jump into the rail is stopped; with nothing to move to, the page may still scroll
                return to == Area.RAIL ? Move.STAY : Move.DEFAULT;
            default:
                return Move.DEFAULT;
        }
    }
}
