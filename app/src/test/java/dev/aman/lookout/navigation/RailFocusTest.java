package dev.aman.lookout.navigation;

import static dev.aman.lookout.navigation.RailFocus.Area.OTHER;
import static dev.aman.lookout.navigation.RailFocus.Area.PAGE;
import static dev.aman.lookout.navigation.RailFocus.Area.RAIL;
import static dev.aman.lookout.navigation.RailFocus.Key.LEFT;
import static dev.aman.lookout.navigation.RailFocus.Key.RIGHT;
import static dev.aman.lookout.navigation.RailFocus.Move.DEFAULT;
import static dev.aman.lookout.navigation.RailFocus.Move.SELECTED_TAB;
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

    // Rows and buttons: Android moves them, and a text field is the exception because it would move its cursor first
    @Test
    public void upAndDownLeaveARowToAndroidWhileItStaysOnThePage() {
        assertEquals(RailFocus.Vertical.LEAVE_TO_ANDROID, RailFocus.onVertical(false, PAGE));
    }

    @Test
    public void upAndDownFocusWhatAndroidFoundFromATextField() {
        assertEquals(RailFocus.Vertical.FOCUS_FOUND, RailFocus.onVertical(true, PAGE));
    }

    // Android can prefer a side menu item to the field beside it, so a text field must not follow it there
    @Test
    public void upAndDownSearchThePageWhenAndroidWouldLeaveIt() {
        for (boolean textField : new boolean[] {false, true}) {
            assertEquals(RailFocus.Vertical.SEARCH_PAGE, RailFocus.onVertical(textField, RAIL));
            assertEquals(RailFocus.Vertical.SEARCH_PAGE, RailFocus.onVertical(textField, OTHER));
        }
    }
}
