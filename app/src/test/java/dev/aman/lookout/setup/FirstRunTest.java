package dev.aman.lookout.setup;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FirstRunTest {
    @Test
    public void isDueForANewInstall() {
        assertTrue(FirstRun.isDue(false, false));
    }

    @Test
    public void isNotDueOnceSeen() {
        assertFalse(FirstRun.isDue(true, false));
    }

    @Test
    public void isNotDueForAnInstallThatAlreadyHasAWebhook() {
        assertFalse(FirstRun.isDue(false, true));
    }
}
