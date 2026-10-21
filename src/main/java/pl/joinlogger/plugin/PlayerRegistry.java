package pl.joinlogger.plugin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

public final class PlayerRegistry {

    private final JoinLoggerPlugin plugin;
    private final File file;
    private final Set<String> players = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    PlayerRegistry(JoinLoggerPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        load();
        importFromServer();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        players.addAll(yaml.getStringList("players"));
    }

    private void importFromServer() {
        // zczytujemy wszystkich graczy, ktorzy kiedykolwiek grali na serwerze
        boolean changed = false;
        for (org.bukkit.OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (op.getName() != null && players.add(op.getName())) {
                changed = true;
            }
        }
        if (changed) {
            save();
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("players", new ArrayList<>(players));
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie udalo sie zapisac bazy graczy!", e);
        }
    }

    public void record(String nick) {
        if (nick == null) {
            return;
        }
        if (players.add(nick)) {
            save();
        }
    }

    public List<String> getKnownPlayers() {
        return new ArrayList<>(players);
    }

    public boolean knows(String nick) {
        // TreeSet z CASE_INSENSITIVE_ORDER porownuje bez wielkosci liter
        return players.contains(nick);
    }
}
