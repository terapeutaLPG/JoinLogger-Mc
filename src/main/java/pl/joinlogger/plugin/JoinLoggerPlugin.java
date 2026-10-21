package pl.joinlogger.plugin;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class JoinLoggerPlugin extends JavaPlugin {

    private LogService logService;
    private StateService stateService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.stateService = new StateService(this);
        this.logService = new LogService(this);

        getServer().getPluginManager().registerEvents(new JoinListener(this), this);

        JoinLoggerCommand command = new JoinLoggerCommand(this);
        this.getCommand("joinlogger").setExecutor(command);
        this.getCommand("joinlogger").setTabCompleter(command);

        getLogger().info("JoinLogger wlaczony. Sprawdzanie whitelisty: "
                + (stateService.isWhitelistCheckEnabled() ? "WLACZONE" : "WYLACZONE"));
    }

    @Override
    public void onDisable() {
        if (stateService != null) {
            stateService.save();
        }
        if (logService != null) {
            logService.close();
        }
        getLogger().info("JoinLogger wylaczony.");
    }

    public LogService getLogService() {
        return logService;
    }

    public StateService getStateService() {
        return stateService;
    }

    File getLogFile() {
        return new File(getDataFolder(), "join-attempts.log");
    }
}
