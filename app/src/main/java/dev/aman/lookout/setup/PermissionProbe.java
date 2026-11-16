package dev.aman.lookout.setup;

/** Reads whether the system currently grants a permission. */
public interface PermissionProbe {
    boolean isGranted(Permission permission);
}
