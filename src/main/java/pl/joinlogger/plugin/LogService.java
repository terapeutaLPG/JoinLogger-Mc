package pl.joinlogger.plugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;

public final class LogService implements AutoCloseable {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        // stala strefa czasowa logow - niezalezna od strefy ustawionej na serwerze
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("Europe/Warsaw"));
    }

    private final JoinLoggerPlugin plugin;
    private BufferedWriter writer;

    LogService(JoinLoggerPlugin plugin) {
        this.plugin = plugin;
        openWriter();
    }

    private synchronized void openWriter() {
        try {
            File logFile = plugin.getLogFile();
            File parent = logFile.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Nie udalo sie utworzyc folderu logow: " + parent);
                return;
            }
            this.writer = new BufferedWriter(new FileWriter(logFile, true));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie udalo sie otworzyc pliku logow!", e);
        }
    }

    public synchronized void logJoinAttempt(String nick, String ip, boolean whitelisted) {
        if (writer == null) {
            return;
        }
        try {
            writer.write(String.format("[%s] nick=%s ip=%s whitelist=%s%n",
                    DATE_FORMAT.format(new Date()), nick, ip, whitelisted));
            writer.flush();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Nie udalo sie zapisac do logu!", e);
        }
    }

    public synchronized void logAutoBan(String nick, String ip) {
        if (!plugin.getConfig().getBoolean("log-bans", true)) {
            return;
        }
        if (writer == null) {
            return;
        }
        try {
            writer.write(String.format("[%s] AUTO-BAN nick=%s ip=%s powod=brak na whitelist%n",
                    DATE_FORMAT.format(new Date()), nick, ip));
            writer.flush();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Nie udalo sie zapisac do logu!", e);
        }
    }

    public synchronized void logAutoUnban(String nick, String ip) {
        if (!plugin.getConfig().getBoolean("log-bans", true)) {
            return;
        }
        if (writer == null) {
            return;
        }
        try {
            writer.write(String.format("[%s] AUTO-UNBAN nick=%s ip=%s powod=gracz na whitelist%n",
                    DATE_FORMAT.format(new Date()), nick, ip));
            writer.flush();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Nie udalo sie zapisac do logu!", e);
        }
    }

    @Override
    public synchronized void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Blad przy zamykaniu pliku logow!", e);
            }
            writer = null;
        }
    }
}
