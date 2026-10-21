package pl.joinlogger.plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class JoinLoggerCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = ChatColor.GRAY + "[" + ChatColor.AQUA + "JoinLogger" + ChatColor.GRAY + "] ";
    private static final String NO_PERMISSION = PREFIX + ChatColor.RED + "Brak uprawnien!";
    private static final String USAGE = PREFIX + ChatColor.YELLOW + "Uzyj: /joinlogger <on|off|status|reload>";

    private final JoinLoggerPlugin plugin;

    JoinLoggerCommand(JoinLoggerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("joinlogger.admin")) {
            sender.sendMessage(NO_PERMISSION);
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(USAGE);
            return true;
        }

        StateService state = plugin.getStateService();
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "on":
                if (state.isWhitelistCheckEnabled()) {
                    sender.sendMessage(PREFIX + ChatColor.YELLOW + "Sprawdzanie whitelisty jest juz wlaczone.");
                    return true;
                }
                state.setWhitelistCheckEnabled(true);
                sender.sendMessage(PREFIX + ChatColor.GREEN + "Sprawdzanie whitelisty wlaczone. (zapamietane po restarcie)");
                return true;
            case "off":
                if (!state.isWhitelistCheckEnabled()) {
                    sender.sendMessage(PREFIX + ChatColor.YELLOW + "Sprawdzanie whitelisty jest juz wylaczone.");
                    return true;
                }
                state.setWhitelistCheckEnabled(false);
                sender.sendMessage(PREFIX + ChatColor.RED + "Sprawdzanie whitelisty wylaczone. (zapamietane po restarcie)");
                return true;
            case "status":
                boolean enabled = state.isWhitelistCheckEnabled();
                sender.sendMessage(PREFIX + "Sprawdzanie whitelisty: "
                        + (enabled ? ChatColor.GREEN + "WLACZONE" : ChatColor.RED + "WYLACZONE"));
                return true;
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(PREFIX + ChatColor.GREEN + "Config przeladowany.");
                return true;
            default:
                sender.sendMessage(USAGE);
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return java.util.Arrays.asList("on", "off", "status", "reload");
        }
        return Collections.emptyList();
    }
}
