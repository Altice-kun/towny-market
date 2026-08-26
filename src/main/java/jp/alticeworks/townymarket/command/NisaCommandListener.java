package jp.alticeworks.townymarket.command;

import jp.alticeworks.townymarket.service.MarketService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class NisaCommandListener implements Listener {
    private final MarketService market;
    public NisaCommandListener(MarketService market){this.market=market;}
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event){
        String[] a=event.getMessage().trim().split("\\s+");
        if(a.length!=4||!a[0].equalsIgnoreCase("/n")||!a[1].equalsIgnoreCase("set")||!a[2].equalsIgnoreCase("nisa"))return;
        event.setCancelled(true);
        try{double limit=Double.parseDouble(a[3]);event.getPlayer().sendMessage(market.setNisa(event.getPlayer(),limit));}
        catch(NumberFormatException e){event.getPlayer().sendMessage("NISA限度額は0以上の数値で指定してください。");}
    }
}
