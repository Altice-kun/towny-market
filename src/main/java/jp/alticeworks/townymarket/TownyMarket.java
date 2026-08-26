package jp.alticeworks.townymarket;

import jp.alticeworks.townymarket.command.MarketCommand;
import jp.alticeworks.townymarket.gui.MarketGui;
import jp.alticeworks.townymarket.service.MarketService;
import jp.alticeworks.townymarket.service.FinanceFeatures;
import jp.alticeworks.townymarket.service.CorporateOperations;
import jp.alticeworks.townymarket.service.FundsLedger;
import jp.alticeworks.townymarket.api.TownyMarketApi;
import jp.alticeworks.townymarket.storage.Database;
import jp.alticeworks.townymarket.towny.TownyHook;
import jp.alticeworks.townymarket.web.WebConsoleServer;
import jp.alticeworks.townymarket.security.AuditAndBackup;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class TownyMarket extends JavaPlugin {
    private Database database; private MarketService market; private FinanceFeatures finance; private CorporateOperations corporate; private FundsLedger funds; private WebConsoleServer webConsole; private AuditAndBackup auditBackup;
    @Override public void onEnable(){
        saveDefaultConfig();
        try { database=new Database(this); } catch(Exception e){getLogger().severe("DB初期化失敗: "+e.getMessage());getServer().getPluginManager().disablePlugin(this);return;}
        Economy economy=null; RegisteredServiceProvider<Economy> rsp=getServer().getServicesManager().getRegistration(Economy.class); if(rsp!=null)economy=rsp.getProvider();
        market=new MarketService(database,economy,new TownyHook(this)); finance=new FinanceFeatures(database,market); corporate=new CorporateOperations(database); funds=new FundsLedger(database); getServer().getServicesManager().register(TownyMarketApi.class,new TownyMarketApi(market,finance),this,org.bukkit.plugin.ServicePriority.Normal);
        MarketGui gui=new MarketGui(this,market); getServer().getPluginManager().registerEvents(gui,this); getServer().getPluginManager().registerEvents(new jp.alticeworks.townymarket.command.NisaCommandListener(market),this);
        MarketCommand command=new MarketCommand(this,market,finance,corporate,funds,gui); getCommand("tm").setExecutor(command);getCommand("tm").setTabCompleter(command); webConsole=new WebConsoleServer(this,database,market,finance); webConsole.start(); auditBackup=new AuditAndBackup(this,database); getServer().getScheduler().runTaskTimer(this,()->{auditBackup.purgeAudit30Days(); auditBackup.backup();},20L*60L,20L*60L*60L*24L); getServer().getScheduler().runTaskTimer(this,()->finance.runDueAutoDividends(),20L*60L,20L*60L);
        getLogger().info("TownyMarket enabled. Vault="+(economy!=null)+" Towny="+(getServer().getPluginManager().getPlugin("Towny")!=null));
    }
    @Override public void onDisable(){if(webConsole!=null)webConsole.stop();if(database!=null)database.close();}
}
