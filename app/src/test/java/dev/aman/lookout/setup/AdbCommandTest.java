package dev.aman.lookout.setup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AdbCommandTest {
    private final AdbCommand commands = new AdbCommand("dev.aman.lookout", "dev.aman.lookout/dev.aman.lookout.playback.MediaAccessService");

    @Test
    public void allowsMediaAccessThroughTheNotificationListener() {
        assertEquals("adb shell cmd notification allow_listener dev.aman.lookout/dev.aman.lookout.playback.MediaAccessService",
                commands.forPermission(Permission.MEDIA_ACCESS));
    }

    @Test
    public void allowsInstallUpdatesThroughAppOps() {
        assertEquals("adb shell appops set dev.aman.lookout REQUEST_INSTALL_PACKAGES allow",
                commands.forPermission(Permission.INSTALL_UPDATES));
    }

    @Test
    public void turnsAccessibilityOnWithTheInstallScriptSoOtherServicesStay() {
        assertTrue(commands.forPermission(Permission.ACCESSIBILITY).startsWith("./scripts/install.sh"));
    }
}
