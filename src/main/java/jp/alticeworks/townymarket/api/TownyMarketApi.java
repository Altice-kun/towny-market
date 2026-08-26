package jp.alticeworks.townymarket.api;

import jp.alticeworks.townymarket.service.FinanceFeatures;
import jp.alticeworks.townymarket.service.MarketService;
import org.bukkit.entity.Player;

/** Stable facade for community add-ons. Keep add-on code independent from internal storage classes. */
public final class TownyMarketApi {
    private final MarketService market; private final FinanceFeatures finance;
    public TownyMarketApi(MarketService market,FinanceFeatures finance){this.market=market;this.finance=finance;}
    public String placeStockOrder(Player player,String symbol,String side,String type,long quantity,long price){return market.placeOrder(player,symbol,side,type,quantity,price);}
    public String publishDividend(Player owner,String company,double amount){return market.payDividend(owner,company,amount);}
    public double effectiveLoanRate(String nation){return finance.effectiveLoanRate(nation);}
    public String loanRateExplanation(String nation){return finance.rateExplanation(nation);}
}
