package model;

import exception.InsufficientBalanceException;
import exception.InsufficientSharesException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class User {

    public static final double STARTING_BALANCE = 100000.00;

    private final String username;
    private double balance;
    private double realizedProfitLoss;     // profit/loss already booked by selling

    private final Portfolio portfolio = new Portfolio();

    // ArrayList - transactions are shown in the same order they happened
    private final List<Transaction> transactions = new ArrayList<>();

    // HashSet - a stock is either in the watchlist or not, no duplicates allowed
    private final Set<String> watchlist = new HashSet<>();

    public User(String username, double balance) {
        this.username = username;
        this.balance = balance;
    }

    // Takes Asset (parent type) so it works for Stock and TechStock both.
    public Transaction buy(Asset asset, int quantity) throws InsufficientBalanceException {

        // IllegalArgumentException is a built-in unchecked exception
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        // read price once and reuse, because the price thread can change it in between
        double price = asset.getPrice();
        double totalCost = price * quantity;

        if (totalCost > balance) {
            throw new InsufficientBalanceException(totalCost, balance);
        }

        balance -= totalCost;
        portfolio.addShares(asset.getSymbol(), quantity, price);

        Transaction transaction = new Transaction("BUY", asset.getSymbol(), quantity, price, 0);
        transactions.add(transaction);
        return transaction;
    }

    public Transaction sell(Asset asset, int quantity) throws InsufficientSharesException {

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        double price = asset.getPrice();
        double averagePrice = portfolio.getAverageBuyPrice(asset.getSymbol());

        // this line throws InsufficientSharesException if we don't own enough
        portfolio.removeShares(asset.getSymbol(), quantity);

        double profitLoss = (price - averagePrice) * quantity;
        balance += price * quantity;
        realizedProfitLoss += profitLoss;

        Transaction transaction =
                new Transaction("SELL", asset.getSymbol(), quantity, price, profitLoss);
        transactions.add(transaction);
        return transaction;
    }

    // Method overloading - same name sell(), but no quantity.
    // Sells every share we have of that stock ("Sell All" button).
    public Transaction sell(Asset asset) throws InsufficientSharesException {
        int owned = portfolio.getQuantity(asset.getSymbol());

        if (owned == 0) {
            throw new InsufficientSharesException(asset.getSymbol(), 1, 0);
        }
        return sell(asset, owned);
    }

    // returns true if the stock is in the watchlist AFTER the toggle
    public boolean toggleWatch(String symbol) {
        if (watchlist.contains(symbol)) {
            watchlist.remove(symbol);
            return false;
        }
        watchlist.add(symbol);
        return true;
    }

    public boolean isWatching(String symbol) {
        return watchlist.contains(symbol);
    }

    public Set<String> getWatchlist() {
        return watchlist;
    }

    public String getUsername() {
        return username;
    }

    public double getBalance() {
        return balance;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public double getRealizedProfitLoss() {
        return realizedProfitLoss;
    }

    public void setRealizedProfitLoss(double realizedProfitLoss) {
        this.realizedProfitLoss = realizedProfitLoss;
    }
}
