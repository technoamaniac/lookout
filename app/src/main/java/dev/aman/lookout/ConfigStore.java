package dev.aman.lookout;

/** Persistence for {@link Config}. */
public interface ConfigStore {
    Config load();

    void save(Config config);
}
