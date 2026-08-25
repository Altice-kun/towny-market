package jp.manus.townymarket.storage;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.sql.*;

public final class Database implements AutoCloseable {
    private final Connection connection;
    public Database(JavaPlugin plugin) throws SQLException {
        plugin.getDataFolder().mkdirs();
        connection = DriverManager.getConnection("jdbc:sqlite:" + new File(plugin.getDataFolder(), "market.db"));
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("PRAGMA journal_mode=WAL");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS companies(id TEXT PRIMARY KEY, owner TEXT NOT NULL, name TEXT UNIQUE NOT NULL, description TEXT NOT NULL, listed INTEGER NOT NULL DEFAULT 0, total_shares INTEGER NOT NULL, share_price INTEGER NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS holdings(player TEXT NOT NULL, asset TEXT NOT NULL, kind TEXT NOT NULL, amount INTEGER NOT NULL, PRIMARY KEY(player,asset,kind))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS nation_currencies(nation TEXT PRIMARY KEY, issuer TEXT NOT NULL, symbol TEXT UNIQUE NOT NULL, supply INTEGER NOT NULL, price INTEGER NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS cryptos(id TEXT PRIMARY KEY, creator TEXT NOT NULL, name TEXT UNIQUE NOT NULL, symbol TEXT UNIQUE NOT NULL, supply INTEGER NOT NULL, price INTEGER NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS wallets(player TEXT NOT NULL, currency TEXT NOT NULL, amount INTEGER NOT NULL, PRIMARY KEY(player,currency))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS orders(id TEXT PRIMARY KEY, asset TEXT NOT NULL, owner TEXT NOT NULL, side TEXT NOT NULL, type TEXT NOT NULL, price INTEGER NOT NULL, quantity INTEGER NOT NULL, remaining INTEGER NOT NULL, status TEXT NOT NULL, created_at INTEGER NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS trades(id INTEGER PRIMARY KEY AUTOINCREMENT, asset TEXT NOT NULL, price INTEGER NOT NULL, quantity INTEGER NOT NULL, executed_at INTEGER NOT NULL)");
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_orders_book ON orders(asset,side,status,price,created_at)");
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_trades_asset_time ON trades(asset,executed_at)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS crypto_balances(player TEXT NOT NULL, symbol TEXT NOT NULL, amount INTEGER NOT NULL, PRIMARY KEY(player,symbol))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS crypto_stakes(player TEXT NOT NULL, symbol TEXT NOT NULL, amount INTEGER NOT NULL, started_at INTEGER NOT NULL, last_reward INTEGER NOT NULL, PRIMARY KEY(player,symbol))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS mining_stats(player TEXT NOT NULL, symbol TEXT NOT NULL, last_mined INTEGER NOT NULL, total_mined INTEGER NOT NULL, PRIMARY KEY(player,symbol))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS nation_tax(nation TEXT PRIMARY KEY, foreign_rate REAL NOT NULL DEFAULT 0, stock_rate REAL NOT NULL DEFAULT 0, crypto_rate REAL NOT NULL DEFAULT 0, treasury REAL NOT NULL DEFAULT 0)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS dividends(id TEXT PRIMARY KEY, company_id TEXT NOT NULL, owner TEXT NOT NULL, total_amount REAL NOT NULL, per_share REAL NOT NULL, created_at INTEGER NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS dividend_claims(dividend_id TEXT NOT NULL, player TEXT NOT NULL, shares INTEGER NOT NULL, gross REAL NOT NULL, tax REAL NOT NULL, net REAL NOT NULL, claimed_at INTEGER NOT NULL, PRIMARY KEY(dividend_id,player))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS tax_ledger(id INTEGER PRIMARY KEY AUTOINCREMENT, nation TEXT NOT NULL, asset_type TEXT NOT NULL, payer TEXT NOT NULL, gross REAL NOT NULL, tax REAL NOT NULL, created_at INTEGER NOT NULL)");
        }
    }
    public Connection connection() { return connection; }
    @Override public void close() { try { connection.close(); } catch (SQLException ignored) {} }
}
