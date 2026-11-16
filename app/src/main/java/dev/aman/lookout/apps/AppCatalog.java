package dev.aman.lookout.apps;

import java.util.List;

/** The apps on the TV that a user can choose to ignore. */
public interface AppCatalog {
    List<InstalledApp> apps();
}
