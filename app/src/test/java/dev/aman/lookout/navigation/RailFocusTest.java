package dev.aman.lookout.navigation;

import static dev.aman.lookout.navigation.RailFocus.Area.OTHER;
import static dev.aman.lookout.navigation.RailFocus.Area.PAGE;
import static dev.aman.lookout.navigation.RailFocus.Area.RAIL;
import static dev.aman.lookout.navigation.RailFocus.Key.DOWN;
import static dev.aman.lookout.navigation.RailFocus.Key.LEFT;
import static dev.aman.lookout.navigation.RailFocus.Key.RIGHT;
import static dev.aman.lookout.navigation.RailFocus.Key.UP;
import static dev.aman.lookout.navigation.RailFocus.Move.DEFAULT;
import static dev.aman.lookout.navigation.RailFocus.Move.SELECTED_TAB;
import static dev.aman.lookout.navigation.RailFocus.Move.STAY;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RailFocusTest {
    @Test
    public void leftFromAPageGoesToTheSelectedTabWhateverAndroidWouldPick() {
        assertEquals(SELECTED_TAB, RailFocus.onKey(LEFT, PAGE, RAIL, false));
        assertEquals(SELECTED_TAB, RailFocus.onKey(LEFT, PAGE, OTHER, false));
    }

    @Test
    public void leftStaysInsideAPageWhenThereIsSomethingToTheLeft() {
        assertEquals(DEFAULT, RailFocus.onKey(LEFT, PAGE, PAGE, false));
    }

    // Left in a text field moves the cursor until it reaches the start
    @Test
    public void leftMovesATextCursorBeforeLeavingThePage() {
        assertEquals(DEFAULT, RailFocus.onKey(LEFT, PAGE, RAIL, true));
        assertEquals(SELECTED_TAB, RailFocus.onKey(LEFT, PAGE, RAIL, false));
    }

    @Test
    public void upAndDownNeverLeaveAPageForTheRail() {
        assertEquals(STAY, RailFocus.onKey(UP, PAGE, RAIL, false));
        assertEquals(STAY, RailFocus.onKey(DOWN, PAGE, RAIL, false));
        assertEquals(DEFAULT, RailFocus.onKey(UP, PAGE, PAGE, false));
        assertEquals(DEFAULT, RailFocus.onKey(DOWN, PAGE, PAGE, false));
    }

    @Test
    public void upAndDownWithNothingToMoveToLeaveScrollingToTheirPage() {
        assertEquals(DEFAULT, RailFocus.onKey(UP, PAGE, OTHER, false));
        assertEquals(DEFAULT, RailFocus.onKey(DOWN, PAGE, OTHER, false));
    }

    @Test
    public void rightIsLeftToAndroid() {
        assertEquals(DEFAULT, RailFocus.onKey(RIGHT, PAGE, OTHER, false));
    }

    @Test
    public void theRailAndAnySheetKeepTheirOwnMovement() {
        for (RailFocus.Key key : RailFocus.Key.values()) {
            assertEquals(DEFAULT, RailFocus.onKey(key, RAIL, PAGE, false));
            assertEquals(DEFAULT, RailFocus.onKey(key, OTHER, RAIL, false));
        }
    }
}
