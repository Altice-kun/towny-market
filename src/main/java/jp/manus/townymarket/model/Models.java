package jp.manus.townymarket.model;

import java.util.UUID;

public final class Models {
    private Models() {}
    public record Company(String id, UUID owner, String name, String description, boolean listed, long totalShares, long sharePrice) {}
    public record NationCurrency(String nation, UUID issuer, String symbol, long supply, long price) {}
    public record Crypto(String id, UUID creator, String name, String symbol, long supply, long price) {}
}
