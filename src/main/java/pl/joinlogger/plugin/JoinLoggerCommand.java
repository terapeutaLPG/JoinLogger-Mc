package pl.joinlogger.plugin;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class JoinLoggerCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = ChatColor.GRAY + "[" + ChatColor.AQUA + "JoinLogger" + ChatColor.GRAY + "] ";
    private static final String NO_PERMISSION = PREFIX + ChatColor.RED + "Brak uprawnien!";
    private static final String USAGE = PREFIX + ChatColor.YELLOW + "Uzyj: /joinlogger <on|off|status|whitelist|banned|reload>";

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
        if (args.length == 0) {
            sender.sendMessage(USAGE);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "on":
            case "off":
                toggleCheck(sender, args[0].equalsIgnoreCase("on"));
                return true;
            case "status":
                sendStatus(sender);
                return true;
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(PREFIX + ChatColor.GREEN + "Config przeladowany.");
                return true;
            case "whitelist":
                handleWhitelist(sender, args);
                return true;
            case "banned":
                sendBannedList(sender);
                return true;
            default:
                sender.sendMessage(USAGE);
                return true;
        }
    }

    private void toggleCheck(CommandSender sender, boolean enable) {
        StateService state = plugin.getStateService();
        if (state.isWhitelistCheckEnabled() == enable) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + "Sprawdzanie whitelisty jest juz "
                    + (enable ? "wlaczone." : "wylaczone."));
            return;
        }
        state.setWhitelistCheckEnabled(enable);
        sender.sendMessage(PREFIX + (enable ? ChatColor.GREEN : ChatColor.RED)
                + "Sprawdzanie whitelisty " + (enable ? "wlaczone." : "wylaczone.")
                + ChatColor.GRAY + " (zapamietane po restarcie)");
    }

    private void sendStatus(CommandSender sender) {
        boolean enabled = plugin.getStateService().isWhitelistCheckEnabled();
        sender.sendMessage(PREFIX + "Sprawdzanie whitelisty: "
                + (enabled ? ChatColor.GREEN + "WLACZONE" : ChatColor.RED + "WYLACZONE"));
    }

    private void handleWhitelist(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + "Uzyj: /joinlogger whitelist <list|add|remove> [nick]");
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "list":
                sendWhitelistList(sender);
                return;
            case "add":
                if (args.length < 3) {
                    sender.sendMessage(PREFIX + ChatColor.YELLOW + "Uzyj: /joinlogger whitelist add <nick>");
                    return;
                }
                addToWhitelist(sender, args[2]);
                return;
            case "remove":
                if (args.length < 3) {
                    sender.sendMessage(PREFIX + ChatColor.YELLOW + "Uzyj: /joinlogger whitelist remove <nick>");
                    return;
                }
                removeFromWhitelist(sender, args[2]);
                return;
            default:
                sender.sendMessage(PREFIX + ChatColor.YELLOW + "Uzyj: /joinlogger whitelist <list|add|remove> [nick]");
        }
    }

    @SuppressWarnings("deprecation")
    private void addToWhitelist(CommandSender sender, String nick) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(nick);
        if (target.isWhitelisted()) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + nick + " jest juz na whitelist.");
            return;
        }
        target.setWhitelisted(true);
        plugin.getPlayerRegistry().record(nick);

        String extra = "";
        BanList nameBans = Bukkit.getBanList(BanList.Type.NAME);
        if (nameBans.isBanned(nick)) {
            nameBans.pardon(nick);
            plugin.getLogService().logAutoUnban(nick, "-");
            extra = ChatColor.GREEN + " (ban zdjety automatycznie)";
        }
        sender.sendMessage(PREFIX + ChatColor.GREEN + nick + " dodany na whitelist." + extra);
    }

    @SuppressWarnings("deprecation")
    private void removeFromWhitelist(CommandSender sender, String nick) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(nick);
        if (!target.isWhitelisted()) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + nick + " nie jest na whitelist.");
            return;
        }
        target.setWhitelisted(false);
        sender.sendMessage(PREFIX + ChatColor.RED + nick + " usuniety z whitelist.");
    }

    // ---------- interaktywne listy ----------

    private void sendWhitelistList(CommandSender sender) {
        List<String> known = plugin.getPlayerRegistry().getKnownPlayers();
        List<String> onWhitelist = new ArrayList<>();
        List<String> notOnWhitelist = new ArrayList<>();
        for (String nick : known) {
            if (Bukkit.getOfflinePlayer(nick).isWhitelisted()) {
                onWhitelist.add(nick);
            } else {
                notOnWhitelist.add(nick);
            }
        }

        sender.sendMessage("");
        sender.sendMessage(PREFIX + ChatColor.WHITE + "Lista graczy " + ChatColor.GRAY
                + "(na whitelist: " + ChatColor.GREEN + onWhitelist.size() + ChatColor.GRAY
                + ", poza: " + ChatColor.RED + notOnWhitelist.size() + ChatColor.GRAY + ")");
        sender.spigot().sendMessage(sectionHeader(ChatColor.GREEN, "NA WHITELIST:"));
        if (onWhitelist.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "  (brak)");
        }
        for (String nick : onWhitelist) {
            sender.spigot().sendMessage(playerLine(nick, true));
        }
        sender.spigot().sendMessage(sectionHeader(ChatColor.RED, "POZA WHITELIST:"));
        if (notOnWhitelist.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "  (brak)");
        }
        for (String nick : notOnWhitelist) {
            sender.spigot().sendMessage(playerLine(nick, false));
        }
        sender.sendMessage("");
    }

    @SuppressWarnings("deprecation")
    private void sendBannedList(CommandSender sender) {
        BanList nameBans = Bukkit.getBanList(BanList.Type.NAME);
        List<String> banned = new ArrayList<>();
        for (BanEntry entry : (Set<BanEntry>) nameBans.getBanEntries()) {
            banned.add(entry.getTarget());
        }

        sender.sendMessage("");
        sender.sendMessage(PREFIX + ChatColor.WHITE + "Zbanowani gracze " + ChatColor.GRAY
                + "(" + banned.size() + ")");
        if (banned.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "  (brak)");
            sender.sendMessage("");
            return;
        }
        for (String nick : banned) {
            sender.spigot().sendMessage(playerLine(nick, Bukkit.getOfflinePlayer(nick).isWhitelisted()));
        }
        sender.sendMessage(ChatColor.GRAY + "  Kliknij [+], aby dodac gracza na whitelist (ban zostanie zdjety).");
        sender.sendMessage("");
    }

    private TextComponent sectionHeader(ChatColor color, String text) {
        TextComponent header = new TextComponent("  " + text);
        header.setColor(toBungee(color));
        header.setBold(true);
        return header;
    }

    private TextComponent playerLine(String nick, boolean whitelisted) {
        TextComponent line = new TextComponent("  " + nick + " ");
        line.setColor(toBungee(whitelisted ? ChatColor.GREEN : ChatColor.RED));

        TextComponent plus = new TextComponent("[+]");
        plus.setColor(toBungee(ChatColor.DARK_GREEN));
        plus.setBold(true);
        plus.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/joinlogger whitelist add " + nick));
        plus.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(ChatColor.GREEN + "Kliknij, aby dodac " + nick + " na whitelist").create()));

        TextComponent minus = new TextComponent(" [-]");
        minus.setColor(toBungee(ChatColor.DARK_RED));
        minus.setBold(true);
        minus.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/joinlogger whitelist remove " + nick));
        minus.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(ChatColor.RED + "Kliknij, aby usunac " + nick + " z whitelist").create()));

        line.addExtra(plus);
        line.addExtra(minus);
        return line;
    }

    private static net.md_5.bungee.api.ChatColor toBungee(ChatColor color) {
        return net.md_5.bungee.api.ChatColor.valueOf(color.name());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(Arrays.asList("on", "off", "status", "whitelist", "banned", "reload"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("whitelist")) {
            return filterPrefix(Arrays.asList("list", "add", "remove"), args[1]);
        }
        return new ArrayList<>();
    }

    private List<String> filterPrefix(List<String> options, String prefix) {
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(prefix.toLowerCase(Locale.ROOT))) {
                result.add(option);
            }
        }
        return result;
    }
}
