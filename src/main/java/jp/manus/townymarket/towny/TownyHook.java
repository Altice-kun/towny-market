package jp.manus.townymarket.towny;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.Method;

public final class TownyHook {
    private final JavaPlugin plugin;
    public TownyHook(JavaPlugin plugin) { this.plugin = plugin; }
    public boolean available() { return Bukkit.getPluginManager().getPlugin("Towny") != null; }
    public String nationOf(Player player) {
        try {
            Class<?> resident = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
            Object api = resident.getMethod("getInstance").invoke(null);
            Object r = api.getClass().getMethod("getResident", Player.class).invoke(api, player);
            if (r == null) return null;
            Object town = r.getClass().getMethod("getTownOrNull").invoke(r);
            if (town == null) return null;
            Object nation = town.getClass().getMethod("getNationOrNull").invoke(town);
            return nation == null ? null : (String) nation.getClass().getMethod("getName").invoke(nation);
        } catch (Exception ignored) { return null; }
    }
    public boolean isKing(Player player, String nation) {
        try {
            Class<?> apiClass = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Object n = apiClass.getMethod("getNation", String.class).invoke(api, nation);
            if (n == null) return false;
            Object king = n.getClass().getMethod("getKing").invoke(n);
            return king != null && player.getUniqueId().equals(king.getClass().getMethod("getUUID").invoke(king));
        } catch (Exception ignored) { return false; }
    }
}
