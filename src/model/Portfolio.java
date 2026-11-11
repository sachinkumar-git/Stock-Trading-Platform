package model;

import exception.InsufficientSharesException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Portfolio {

    // symbol -> holding. HashMap so we can find a stock directly by its symbol
    private final Map<String, Holding> holdings = new HashMap<>();

    public void addShares(String symbol, int quantity, double price) {
        Holding holding = holdings.get(symbol);

        if (holding == null) {
            holdings.put(symbol, new Holding(symbol, quantity, price));
        } else {
            holding.addShares(quantity, price);
        }
    }

    // "throws" tells the caller that this method can fail with our own exception
    public void removeShares(String symbol, int quantity) throws InsufficientSharesException {
        int owned = getQuantity(symbol);

        if (quantity > owned) {
            throw new InsufficientSharesException(symbol, quantity, owned);
        }

        holdings.get(symbol).removeShares(quantity);
        removeEmptyHoldings();
    }

    // Using Iterator here on purpose - if we remove from a map inside a normal
    // for-each loop we get ConcurrentModificationException. iterator.remove() is the safe way.
    private void removeEmptyHoldings() {
        Iterator<Holding> iterator = holdings.values().iterator();

        while (iterator.hasNext()) {
            Holding holding = iterator.next();
            if (holding.getQuantity() == 0) {
                iterator.remove();
            }
        }
    }

    // used while loading saved data from the database
    public void putHolding(Holding holding) {
        holdings.put(holding.getSymbol(), holding);
    }

    public Holding getHolding(String symbol) {
        return holdings.get(symbol);
    }

    public int getQuantity(String symbol) {
        Holding holding = holdings.get(symbol);
        return holding == null ? 0 : holding.getQuantity();
    }

    public double getAverageBuyPrice(String symbol) {
        Holding holding = holdings.get(symbol);
        return holding == null ? 0 : holding.getAverageBuyPrice();
    }

    public Collection<Holding> getHoldings() {
        return holdings.values();
    }

    public double getTotalInvestment() {
        double total = 0;
        for (Holding holding : holdings.values()) {
            total += holding.getInvestedAmount();
        }
        return total;
    }

    // "? extends Asset" = map of Asset or any child of Asset (Stock, TechStock) is accepted
    public double getCurrentValue(Map<String, ? extends Asset> marketStocks) {
        double total = 0;

        for (Holding holding : holdings.values()) {
            Asset asset = marketStocks.get(holding.getSymbol());
            if (asset != null) {
                total += holding.getQuantity() * asset.getPrice();
            }
        }
        return total;
    }

    // profit/loss on shares we still hold (not sold yet)
    public double getUnrealizedProfitLoss(Map<String, ? extends Asset> marketStocks) {
        return getCurrentValue(marketStocks) - getTotalInvestment();
    }
}
