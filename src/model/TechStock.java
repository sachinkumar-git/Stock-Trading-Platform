package model;

// Multilevel inheritance: Asset -> Stock -> TechStock
// Tech shares jump around more, so only the volatility is different.
// final class - nobody can extend TechStock further.
public final class TechStock extends Stock {

    public TechStock(String symbol, String companyName, double price) {
        super(symbol, companyName, price, "Technology");
    }

    // method overriding. When StockMarket calls asset.getVolatility() on an
    // Asset reference, java picks this version at runtime if the object is a TechStock
    // (dynamic method dispatch).
    @Override
    public double getVolatility() {
        return 0.05;
    }

    @Override
    public String toString() {
        return super.toString() + " (Tech)";
    }
}
