package dev.aman.lookout.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SaveGateTest {
    private static final Config SAVED = Config.parse("https://example.com/h", "a", false);

    @Test
    public void aBlankUrlCanBeNeitherTestedNorSaved() {
        SaveGate gate = new SaveGate(SAVED);
        assertEquals(SaveGate.State.NO_URL, gate.stateOf(Config.parse("", "a", false)));
        assertEquals(SaveGate.State.NO_URL, new SaveGate(Config.parse("", "", false))
                .stateOf(Config.parse(Config.URL_PREFIX, "", false)));
    }

    @Test
    public void theSavedValuesAreUnchanged() {
        assertEquals(SaveGate.State.UNCHANGED,
                new SaveGate(SAVED).stateOf(Config.parse(" https://example.com/h ", "a, a", false)));
    }

    @Test
    public void otherSettingsCanBeSavedWithoutATest() {
        assertEquals(SaveGate.State.SAVEABLE,
                new SaveGate(SAVED).stateOf(Config.parse("https://example.com/h", "a, b", true)));
    }

    @Test
    public void anInvalidUrlNeedsATest() {
        assertEquals(SaveGate.State.NEEDS_TEST, new SaveGate(SAVED).stateOf(null));
    }

    @Test
    public void aNewUrlCanBeSavedOnlyAfterATestReachesIt() {
        SaveGate gate = new SaveGate(SAVED);
        Config fresh = Config.parse("https://example.com/new", "a", false);
        assertEquals(SaveGate.State.NEEDS_TEST, gate.stateOf(fresh));

        gate.onTested(fresh, true);
        assertEquals(SaveGate.State.SAVEABLE, gate.stateOf(fresh));
        assertEquals(SaveGate.State.NEEDS_TEST, gate.stateOf(Config.parse("https://example.com/other", "a", false)));
    }

    @Test
    public void onlyATestedNewUrlIsReadyToSave() {
        SaveGate gate = new SaveGate(SAVED);
        Config fresh = Config.parse("https://example.com/new", "a", false);
        Config otherSettings = Config.parse("https://example.com/h", "b", false);
        assertFalse(gate.isNewUrlReadyToSave(fresh));
        assertFalse(gate.isNewUrlReadyToSave(otherSettings));

        gate.onTested(fresh, true);
        assertTrue(gate.isNewUrlReadyToSave(fresh));
        gate.onTested(otherSettings, true);
        assertFalse(gate.isNewUrlReadyToSave(otherSettings));
    }

    @Test
    public void aFailedTestWithdrawsAnEarlierPass() {
        SaveGate gate = new SaveGate(SAVED);
        Config fresh = Config.parse("https://example.com/new", "a", false);
        gate.onTested(fresh, true);
        gate.onTested(Config.parse("https://example.com/other", "a", false), false);
        assertEquals(SaveGate.State.NEEDS_TEST, gate.stateOf(fresh));
    }

    @Test
    public void savingMakesTheNewValuesTheBaseline() {
        SaveGate gate = new SaveGate(SAVED);
        Config fresh = Config.parse("https://example.com/new", "a", false);
        gate.onTested(fresh, true);
        gate.onSaved(fresh);
        assertEquals(SaveGate.State.UNCHANGED, gate.stateOf(fresh));
        assertEquals(SaveGate.State.NEEDS_TEST, gate.stateOf(SAVED));
    }
}
