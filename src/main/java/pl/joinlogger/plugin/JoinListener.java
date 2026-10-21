package pl.joinlogger.plugin;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public final class JoinListener implements Listener {

    private final JoinLoggerPlugin plugin;

    JoinListener(JoinLoggerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBannedLogin(PlayerLoginEvent event) {
        // gracz z banem moze sie zalogowac, jesli zostal dodany z powrotem na whiteliste
        if (event.getResult() != PlayerLoginEvent.Result.KICK_BANNED) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isWhitelisted()) {
            return;
        }
        plugin.getPlayerRegistry().record(player.getName());
        @SuppressWarnings("deprecation")
        BanList nameBans = Bukkit.getBanList(BanList.Type.NAME);
        nameBans.pardon(player.getName());
        event.setResult(PlayerLoginEvent.Result.ALLOWED);
        event.setKickMessage("");
        plugin.getLogService().logAutoUnban(player.getName(), event.getAddress().getHostAddress());
        Bukkit.getLogger().info("[JoinLogger] Auto-odbanowano " + player.getName()
                + " - gracz znajduje sie na whitelist.");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            return;
        }
        Player player = event.getPlayer();
        String nick = player.getName();
        String ip = event.getAddress().getHostAddress();
        boolean whitelisted = player.isWhitelisted();

        plugin.getPlayerRegistry().record(nick);

        if (plugin.getConfig().getBoolean("enable-join-log", true)) {
            plugin.getLogService().logJoinAttempt(nick, ip, whitelisted);
        }

        if (shouldBan(player, whitelisted)) {
            ban(player, nick, ip);
        }
    }

    private boolean shouldBan(Player player, boolean whitelisted) {
        if (whitelisted) {
            return false;
        }
        if (!plugin.getStateService().isWhitelistCheckEnabled()) {
            return false;
        }
        if (!plugin.getConfig().getBoolean("enable-auto-ban", true)) {
            return false;
        }
        // banujemy tylko gdy serwer ma wlaczona whiteliste - inaczej kazdy gracz bylby banowany
        if (!Bukkit.hasWhitelist()) {
            return false;
        }
        // nie nadpisujemy juz istniejacego bana
        return !Bukkit.getBanList(BanList.Type.NAME).isBanned(player.getName());
    }

    private void ban(Player player, String nick, String ip) {
        String reason = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("ban-reason", "&cNie jestes na whitelist tego serwera!"));

        @SuppressWarnings("deprecation")
        BanList nameBans = Bukkit.getBanList(BanList.Type.NAME);
        nameBans.addBan(nick, reason, null, "JoinLogger");

        player.kickPlayer(reason);

        plugin.getLogService().logAutoBan(nick, ip);
        Bukkit.getLogger().info("[JoinLogger] Perma-ban: " + nick + " (" + ip + ") - brak na whitelist.");
    }
}
