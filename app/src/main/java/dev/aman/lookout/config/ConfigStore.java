package dev.aman.lookout.config;



/** Persistence for {@link Config}. */
public interface ConfigStore {
    Config load();

    void save(Config config);
}
