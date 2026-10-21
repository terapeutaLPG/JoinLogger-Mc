package pl.joinlogger.plugin;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public final class StateService {

    private static final String STATE_KEY = "whitelist-check-enabled";

    private final JoinLoggerPlugin plugin;
    private final File stateFile;
    private boolean whitelistCheckEnabled;

    StateService(JoinLoggerPlugin plugin) {
        this.plugin = plugin;
        this.stateFile = new File(plugin.getDataFolder(), "state.yml");
        load();
    }

    private void load() {
        if (!stateFile.exists()) {
            // pierwszy start - stan domyslny brany z config.yml
            this.whitelistCheckEnabled = plugin.getConfig().getBoolean("enable-auto-ban", true);
            save();
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(stateFile);
        this.whitelistCheckEnabled = yaml.getBoolean(STATE_KEY, true);
    }

    void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set(STATE_KEY, whitelistCheckEnabled);
        try {
            yaml.save(stateFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie udalo sie zapisac stanu pluginu!", e);
        }
    }

    public boolean isWhitelistCheckEnabled() {
        return whitelistCheckEnabled;
    }

    public void setWhitelistCheckEnabled(boolean enabled) {
        this.whitelistCheckEnabled = enabled;
        save();
    }
}
