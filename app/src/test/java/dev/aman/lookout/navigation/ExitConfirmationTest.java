package dev.aman.lookout.navigation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ExitConfirmationTest {
    private final ExitConfirmation exit = new ExitConfirmation(2_000);

    @Test
    public void theFirstPressOnlyAsks() {
        assertFalse(exit.confirmed(10_000));
    }

    @Test
    public void aSecondPressWithinTheWindowConfirms() {
        exit.confirmed(10_000);
        assertTrue(exit.confirmed(12_000));
    }

    @Test
    public void aSecondPressAfterTheWindowAsksAgain() {
        exit.confirmed(10_000);
        assertFalse(exit.confirmed(12_001));
        assertTrue(exit.confirmed(13_000));
    }

    @Test
    public void confirmingResetsSoTheNextPressAsksAgain() {
        exit.confirmed(10_000);
        exit.confirmed(10_500);
        assertFalse(exit.confirmed(11_000));
    }
}
