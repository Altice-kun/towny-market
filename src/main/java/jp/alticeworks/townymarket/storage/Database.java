package jp.alticeworks.townymarket.storage;

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
            s.executeUpdate("CREATE TABLE IF NOT EXISTS dividend_entitlements(dividend_id TEXT NOT NULL, player TEXT NOT NULL, shares INTEGER NOT NULL, gross REAL NOT NULL, tax REAL NOT NULL, net REAL NOT NULL, claimed INTEGER NOT NULL DEFAULT 0, claimed_at INTEGER, PRIMARY KEY(dividend_id,player))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS shareholder_meetings(id TEXT PRIMARY KEY, company_id TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, closes_at INTEGER NOT NULL, created_at INTEGER NOT NULL, status TEXT NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS auto_dividend_rules(company_id TEXT PRIMARY KEY, interval_seconds INTEGER NOT NULL, amount REAL NOT NULL, next_run INTEGER NOT NULL, enabled INTEGER NOT NULL DEFAULT 1, updated_by TEXT NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS loan_rate_history(id INTEGER PRIMARY KEY AUTOINCREMENT, loan_id TEXT NOT NULL, credit_rating TEXT NOT NULL, policy_rate REAL NOT NULL, risk_spread REAL NOT NULL, effective_rate REAL NOT NULL, calculated_at INTEGER NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS meeting_votes(meeting_id TEXT NOT NULL, player TEXT NOT NULL, choice TEXT NOT NULL, voting_power INTEGER NOT NULL, voted_at INTEGER NOT NULL, PRIMARY KEY(meeting_id,player))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS tax_ledger(id INTEGER PRIMARY KEY AUTOINCREMENT, nation TEXT NOT NULL, asset_type TEXT NOT NULL, payer TEXT NOT NULL, gross REAL NOT NULL, tax REAL NOT NULL, created_at INTEGER NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS audit_events(id INTEGER PRIMARY KEY AUTOINCREMENT, actor TEXT NOT NULL, action TEXT NOT NULL, target TEXT, reason TEXT, payload TEXT, created_at INTEGER NOT NULL)");
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_audit_events_time ON audit_events(created_at)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS frozen_accounts(account TEXT PRIMARY KEY, kind TEXT NOT NULL, frozen_by TEXT NOT NULL, reason TEXT NOT NULL, created_at INTEGER NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS refund_requests(id TEXT PRIMARY KEY, requester TEXT NOT NULL, reference_id TEXT NOT NULL, amount REAL NOT NULL, reason TEXT NOT NULL, status TEXT NOT NULL, created_at INTEGER NOT NULL, reviewed_by TEXT)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS asset_reservations(owner TEXT NOT NULL, asset TEXT NOT NULL, kind TEXT NOT NULL, amount REAL NOT NULL, purpose TEXT NOT NULL, reference_id TEXT PRIMARY KEY, created_at INTEGER NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS reconciliation_reports(id TEXT PRIMARY KEY, status TEXT NOT NULL, differences TEXT NOT NULL, created_at INTEGER NOT NULL, repaired_by TEXT)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS companies_charter(company_id TEXT PRIMARY KEY, text TEXT NOT NULL, updated_at INTEGER NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS company_roles(company_id TEXT NOT NULL, player TEXT NOT NULL, role TEXT NOT NULL, permissions TEXT NOT NULL, PRIMARY KEY(company_id,player))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS share_classes(company_id TEXT NOT NULL, class_name TEXT NOT NULL, voting_rights REAL NOT NULL, dividend_multiplier REAL NOT NULL, total_shares INTEGER NOT NULL, lockup_until INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(company_id,class_name))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS shareholder_record_dates(company_id TEXT PRIMARY KEY, record_at INTEGER NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS corporate_loans(id TEXT PRIMARY KEY, borrower TEXT NOT NULL, lender TEXT NOT NULL, principal REAL NOT NULL, interest_rate REAL NOT NULL, due_at INTEGER NOT NULL, status TEXT NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS national_bonds(id TEXT PRIMARY KEY, nation TEXT NOT NULL, principal REAL NOT NULL, interest_rate REAL NOT NULL, maturity_at INTEGER NOT NULL, status TEXT NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS nation_finance(nation TEXT PRIMARY KEY, reserve REAL NOT NULL DEFAULT 0, policy_rate REAL NOT NULL DEFAULT 0, credit_rating TEXT NOT NULL DEFAULT 'BBB')");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS exchange_rates(base_nation TEXT NOT NULL, quote_nation TEXT NOT NULL, rate REAL NOT NULL, updated_at INTEGER NOT NULL, PRIMARY KEY(base_nation,quote_nation))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS backups(id TEXT PRIMARY KEY, path TEXT NOT NULL, sha256 TEXT NOT NULL, encrypted INTEGER NOT NULL, created_at INTEGER NOT NULL, status TEXT NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS two_factor_challenges(id TEXT PRIMARY KEY, actor TEXT NOT NULL, action TEXT NOT NULL, payload TEXT NOT NULL, expires_at INTEGER NOT NULL, used INTEGER NOT NULL DEFAULT 0)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS transaction_passcodes(player TEXT PRIMARY KEY, pass_hash TEXT NOT NULL, updated_at INTEGER NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS transaction_approvals(id TEXT PRIMARY KEY, transaction_ref TEXT NOT NULL, requester TEXT NOT NULL, approver TEXT, status TEXT NOT NULL, created_at INTEGER NOT NULL, approved_at INTEGER)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS employment_contracts(id TEXT PRIMARY KEY, company_id TEXT NOT NULL, player TEXT NOT NULL, role TEXT NOT NULL, salary REAL NOT NULL, benefits TEXT NOT NULL, starts_at INTEGER NOT NULL, ends_at INTEGER, status TEXT NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS benefit_plans(id TEXT PRIMARY KEY, company_id TEXT NOT NULL, name TEXT NOT NULL, details TEXT NOT NULL, cost REAL NOT NULL, enabled INTEGER NOT NULL DEFAULT 1)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS role_compensation(company_id TEXT NOT NULL, role TEXT NOT NULL, salary REAL NOT NULL, bonus REAL NOT NULL DEFAULT 0, PRIMARY KEY(company_id,role))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS intercompany_contracts(id TEXT PRIMARY KEY, seller_company TEXT NOT NULL, buyer_company TEXT NOT NULL, title TEXT NOT NULL, terms TEXT NOT NULL, amount REAL NOT NULL, due_at INTEGER NOT NULL, status TEXT NOT NULL, created_at INTEGER NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS invoices(id TEXT PRIMARY KEY, contract_id TEXT, issuer_company TEXT NOT NULL, recipient_company TEXT NOT NULL, amount REAL NOT NULL, due_at INTEGER NOT NULL, status TEXT NOT NULL, issued_at INTEGER NOT NULL, paid_at INTEGER)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS company_credit_scores(company_id TEXT PRIMARY KEY, score INTEGER NOT NULL, grade TEXT NOT NULL, factors TEXT NOT NULL, calculated_at INTEGER NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS company_relations(source_company TEXT NOT NULL, target_company TEXT NOT NULL, relation TEXT NOT NULL, note TEXT NOT NULL, updated_at INTEGER NOT NULL, PRIMARY KEY(source_company,target_company))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS company_status(company_id TEXT PRIMARY KEY, status TEXT NOT NULL DEFAULT 'ACTIVE', reason TEXT NOT NULL DEFAULT '', closed_at INTEGER)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS fund_accounts(account_id TEXT PRIMARY KEY, owner TEXT NOT NULL, owner_kind TEXT NOT NULL, currency TEXT NOT NULL, available REAL NOT NULL DEFAULT 0, reserved REAL NOT NULL DEFAULT 0, updated_at INTEGER NOT NULL, UNIQUE(owner,owner_kind,currency))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS fund_journal(id INTEGER PRIMARY KEY AUTOINCREMENT, tx_key TEXT NOT NULL UNIQUE, debit_account TEXT, credit_account TEXT, currency TEXT NOT NULL, amount REAL NOT NULL, fee REAL NOT NULL DEFAULT 0, memo TEXT NOT NULL, created_at INTEGER NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS fund_reservations(id TEXT PRIMARY KEY, account_id TEXT NOT NULL, currency TEXT NOT NULL, amount REAL NOT NULL, reference_id TEXT NOT NULL UNIQUE, status TEXT NOT NULL, created_at INTEGER NOT NULL, released_at INTEGER)");
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_fund_journal_time ON fund_journal(created_at)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS nation_nisa(nation TEXT PRIMARY KEY, limit_amount REAL NOT NULL DEFAULT 0, used_amount REAL NOT NULL DEFAULT 0, updated_at INTEGER NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS nisa_ledger(id INTEGER PRIMARY KEY AUTOINCREMENT, nation TEXT NOT NULL, player TEXT NOT NULL, asset_type TEXT NOT NULL, gross REAL NOT NULL, exempt REAL NOT NULL, taxable REAL NOT NULL, tax REAL NOT NULL, created_at INTEGER NOT NULL)");
        }
    }
    public Connection connection() { return connection; }
    @Override public void close() { try { connection.close(); } catch (SQLException ignored) {} }
}
