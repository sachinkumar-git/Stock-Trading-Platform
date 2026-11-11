package model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

// Abstract class - we never do "new Asset()", only its child classes (Stock, TechStock).
// Common things like symbol, price and price history are kept here so they are not repeated.
public abstract class Asset implements Tradable {

    // constant, same for every asset
    public static final int HISTORY_SIZE = 10;

    // static = one copy shared by all objects, counts how many assets got created
    private static int totalAssets = 0;

    private final String symbol;        // final -> cannot change after constructor
    private final String companyName;
    private final double openPrice;     // price when the app started, used for change %
    private double price;

    // LinkedList because we keep adding at the end and removing from the front
    private final LinkedList<Double> priceHistory = new LinkedList<>();

    protected Asset(String symbol, String companyName, double price) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.price = price;
        this.openPrice = price;
        priceHistory.add(price);
        totalAssets++;
    }

    // child classes MUST give their own version of these two
    public abstract String getCategory();

    // how much the price can move in one update (0.02 means max 2% up or down)
    public abstract double getVolatility();

    // final method - child class is not allowed to override this
    @Override
    public final String getSymbol() {
        return symbol;
    }

    @Override
    public String getCompanyName() {
        return companyName;
    }

    // synchronized because the price thread writes the price
    // while the GUI thread is reading it at the same time
    @Override
    public synchronized double getPrice() {
        return price;
    }

    public synchronized void setPrice(double newPrice) {
        if (newPrice <= 0) {
            return;
        }
        price = newPrice;

        priceHistory.addLast(newPrice);
        if (priceHistory.size() > HISTORY_SIZE) {
            priceHistory.removeFirst();     // drop the oldest one
        }
    }

    public synchronized double getChangePercent() {
        return (price - openPrice) / openPrice * 100;
    }

    // returns a copy so nobody outside can mess with the real list
    public synchronized List<Double> getPriceHistory() {
        return new ArrayList<>(priceHistory);
    }

    public static int getTotalAssets() {
        return totalAssets;
    }

    // toString() comes from Object class, we override it
    @Override
    public String toString() {
        return symbol + " - " + companyName;
    }
}
