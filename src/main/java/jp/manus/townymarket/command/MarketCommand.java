package jp.manus.townymarket.command;

import jp.manus.townymarket.service.MarketService;
import jp.manus.townymarket.gui.MarketGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public final class MarketCommand implements CommandExecutor, TabCompleter {
    private final org.bukkit.plugin.java.JavaPlugin plugin; private final MarketService service; private final MarketGui gui;
    public MarketCommand(org.bukkit.plugin.java.JavaPlugin plugin,MarketService service,MarketGui gui){this.plugin=plugin;this.service=service;this.gui=gui;}
    private void msg(CommandSender s,String text){s.sendMessage(Component.text(text,NamedTextColor.YELLOW));}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] a){
        if(!(sender instanceof Player p)){msg(sender,"プレイヤーのみ実行できます。");return true;}
        if(a.length==0){msg(p,"/tm gui または /tm market を使用してください。詳しくは /tm help");return true;}
        try { switch(a[0].toLowerCase()){case "help" -> msg(p,"/tm company <名前> <株数> <株価> /tm list <名前> /tm foreign <記号> <供給量> <価格> /tm crypto <名前> <記号> <供給量> <開始値> /tm order <BUY|SELL> <株式名> <LIMIT|MARKET> <数量> [価格] /tm market");case "company" -> {if(a.length<4){msg(p,"/tm company <名前> <株数> <株価> [説明]");break;}msg(p,service.createCompany(p,a[1],a.length>4?String.join(" ",Arrays.copyOfRange(a,4,a.length)):"",Long.parseLong(a[2]),Long.parseLong(a[3])));}case "list" -> {if(a.length<2){msg(p,"企業名を指定してください。");break;}msg(p,service.listCompany(p,a[1]));}case "foreign" -> {if(a.length<4){msg(p,"/tm foreign <記号> <供給量> <価格>");break;}msg(p,service.issueForeign(p,a[1],Long.parseLong(a[2]),Long.parseLong(a[3])));}case "crypto" -> {if(a.length<5){msg(p,"/tm crypto <名前> <記号> <供給量> <開始値>");break;}msg(p,service.createCrypto(p,a[1],a[2],Long.parseLong(a[3]),Long.parseLong(a[4])));}case "mine" -> {if(a.length<2){msg(p,"/tm mine <記号>");break;}msg(p,service.mine(p,a[1]));}case "stake" -> {if(a.length<3){msg(p,"/tm stake <記号> <数量>");break;}msg(p,service.stake(p,a[1],Long.parseLong(a[2])));}case "unstake" -> {if(a.length<2){msg(p,"/tm unstake <記号>");break;}msg(p,service.unstake(p,a[1]));}case "balance" -> {if(a.length<2){msg(p,"/tm balance <記号>");break;}msg(p,service.cryptoInfo(p,a[1]));}case "dividend" -> {if(a.length<3){msg(p,"/tm dividend <企業名> <総額>");break;}msg(p,service.payDividend(p,a[1],Double.parseDouble(a[2])));}case "tax" -> {if(a.length<3){msg(p,"/tm tax <foreign|stock|crypto> <税率%>");break;}msg(p,service.setTax(p,a[1],Double.parseDouble(a[2])));}case "taxinfo" -> msg(p,service.taxInfo(p));case "order" -> {if(a.length<5){msg(p,"/tm order <BUY|SELL> <株式名> <LIMIT|MARKET> <数量> [価格]");break;}long quantity=Long.parseLong(a[4]);long price=a[3].equalsIgnoreCase("MARKET")?0:(a.length>=6?Long.parseLong(a[5]):0);msg(p,service.placeOrder(p,a[2],a[1],a[3],quantity,price));}case "buy", "sell" -> {if(a.length<4){msg(p,"/tm "+a[0]+" <stock|crypto> <企業名または記号> <数量>");break;}msg(p,"旧式即時売買は注文板移行後、/tm order を使用してください。");}case "market" -> {msg(p,"--- Market ---");service.market().forEach(x->msg(p,x));}case "gui" -> gui.open(p);default -> msg(p,"不明なコマンドです。/tm help");} } catch(NumberFormatException e){msg(p,"数値が不正です。");} return true;
    }
    @Override public List<String> onTabComplete(CommandSender s,Command c,String l,String[] a){if(a.length==1)return List.of("help","company","list","foreign","crypto","mine","stake","unstake","balance","dividend","tax","taxinfo","order","buy","sell","market","gui").stream().filter(x->x.startsWith(a[0].toLowerCase())).toList();return List.of();}
}
