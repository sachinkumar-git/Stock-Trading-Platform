package service;

import exception.StockNotFoundException;
import model.Asset;
import model.Stock;
import model.TechStock;
import util.MarketUtils;
import util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

public class StockMarket {

    // 2D array with the starting data: symbol, company, price, sector
    private static final String[][] STOCK_DATA = {
            {"AAPL",  "Apple Inc.",        "225.00", "Technology"},
            {"GOOGL", "Alphabet Inc.",     "250.00", "Technology"},
            {"MSFT",  "Microsoft Corp.",   "500.00", "Technology"},
            {"AMZN",  "Amazon.com Inc.",   "230.00", "Technology"},
            {"TSLA",  "Tesla Inc.",        "450.00", "Technology"},
            {"META",  "Meta Platforms",    "600.00", "Technology"},
            {"NFLX",  "Netflix Inc.",      "700.00", "Technology"},
            {"JPM",   "JPMorgan Chase",    "310.00", "Banking"},
            {"KO",    "Coca-Cola Co.",     "70.00",  "Consumer"},
            {"WMT",   "Walmart Inc.",      "105.00", "Consumer"},
            {"XOM",   "Exxon Mobil Corp.", "115.00", "Energy"}
    };

    // Hashtable instead of HashMap because two threads touch this map
    // (GUI thread + price updater thread) and Hashtable methods are synchronized.
    private final Map<String, Asset> stocks = new Hashtable<>();

    private final Random random = new Random();

    public StockMarket() {
        for (String[] row : STOCK_DATA) {
            double price = Double.parseDouble(row[2]);

            // parent reference, child object
            Asset asset;
            if (row[3].equals("Technology")) {
                asset = new TechStock(row[0], row[1], price);
            } else {
                asset = new Stock(row[0], row[1], price, row[3]);
            }
            stocks.put(asset.getSymbol(), asset);
        }
    }

    public Asset getStock(String symbol) throws StockNotFoundException {
        Asset asset = stocks.get(symbol.toUpperCase());

        if (asset == null) {
            throw new StockNotFoundException(symbol);
        }
        return asset;
    }

    public Map<String, Asset> getStocks() {
        return stocks;
    }

    // TreeSet keeps the symbols sorted A-Z automatically (Hashtable has no order)
    public TreeSet<String> getSortedSymbols() {
        return new TreeSet<>(stocks.keySet());
    }

    public List<Asset> getAllStocks() {
        List<Asset> list = new ArrayList<>();
        for (String symbol : getSortedSymbols()) {
            list.add(stocks.get(symbol));
        }
        return list;
    }

    // Called by the price thread every few seconds.
    // synchronized so that two updates can never run at the same time.
    public synchronized void updatePrices() {
        for (Asset asset : stocks.values()) {

            // runtime polymorphism: Stock gives 0.02, TechStock gives 0.05
            double swing = asset.getVolatility();

            // random number between -swing and +swing
            double change = (random.nextDouble() * 2 * swing) - swing;
            double newPrice = asset.getPrice() * (1 + change);

            if (newPrice < 1) {
                newPrice = 1;
            }

            // keep only 2 decimal places
            newPrice = Math.round(newPrice * 100) / 100.0;
            asset.setPrice(newPrice);
        }
    }

    // stock that went up the most since the app started -> (symbol, change %)
    public Pair<String, Double> getTopGainer() {
        Map<String, Double> changes = new HashMap<>();

        for (Asset asset : stocks.values()) {
            changes.put(asset.getSymbol(), asset.getChangePercent());
        }
        return MarketUtils.maxByValue(changes);
    }
}
