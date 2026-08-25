package jp.manus.townymarket;

import jp.manus.townymarket.command.MarketCommand;
import jp.manus.townymarket.gui.MarketGui;
import jp.manus.townymarket.service.MarketService;
import jp.manus.townymarket.storage.Database;
import jp.manus.townymarket.towny.TownyHook;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class TownyMarketPlugin extends JavaPlugin {
    private Database database; private MarketService market;
    @Override public void onEnable(){
        saveDefaultConfig();
        try { database=new Database(this); } catch(Exception e){getLogger().severe("DB初期化失敗: "+e.getMessage());getServer().getPluginManager().disablePlugin(this);return;}
        Economy economy=null; RegisteredServiceProvider<Economy> rsp=getServer().getServicesManager().getRegistration(Economy.class); if(rsp!=null)economy=rsp.getProvider();
        market=new MarketService(database,economy,new TownyHook(this));
        MarketGui gui=new MarketGui(this,market); getServer().getPluginManager().registerEvents(gui,this);
        MarketCommand command=new MarketCommand(this,market,gui); getCommand("tm").setExecutor(command);getCommand("tm").setTabCompleter(command);
        getLogger().info("TownyMarket enabled. Vault="+(economy!=null)+" Towny="+(getServer().getPluginManager().getPlugin("Towny")!=null));
    }
    @Override public void onDisable(){if(database!=null)database.close();}
}
