package dev.aman.lookout.setup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** A snapshot of every permission and whether it is granted, in the order the setup screen lists them. */
public final class Checklist {
    private final List<Row> rows;

    private Checklist(List<Row> rows) {
        this.rows = Collections.unmodifiableList(rows);
    }

    public static Checklist read(PermissionProbe probe) {
        List<Row> rows = new ArrayList<>();
        for (Permission permission : Permission.values()) {
            rows.add(new Row(permission, probe.isGranted(permission)));
        }
        return new Checklist(rows);
    }

    public List<Row> rows() {
        return rows;
    }

    /** The first permission still to allow, which is where focus goes; empty when all are granted. */
    public Optional<Permission> firstMissing() {
        for (Row row : rows) {
            if (!row.granted()) return Optional.of(row.permission());
        }
        return Optional.empty();
    }

    /** One permission and whether it is granted. */
    public static final class Row {
        private final Permission permission;
        private final boolean granted;

        Row(Permission permission, boolean granted) {
            this.permission = permission;
            this.granted = granted;
        }

        public Permission permission() {
            return permission;
        }

        public boolean granted() {
            return granted;
        }
    }
}
