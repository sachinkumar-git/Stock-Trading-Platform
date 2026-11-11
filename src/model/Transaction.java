package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// One BUY or SELL entry. Once created it never changes, so all fields are final.
public final class Transaction {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final String type;          // "BUY" or "SELL"
    private final String stockSymbol;
    private final int quantity;
    private final double price;
    private final double profitLoss;    // 0 for BUY, only SELL has profit/loss
    private final LocalDateTime dateTime;

    // Constructor overloading - two constructors, different parameters.
    // This one is for a new trade happening right now.
    public Transaction(String type, String stockSymbol, int quantity,
                       double price, double profitLoss) {
        this(type, stockSymbol, quantity, price, profitLoss, LocalDateTime.now());
    }

    // This one is used when reading old transactions back from the database,
    // there we already have the date so we pass it in.
    public Transaction(String type, String stockSymbol, int quantity,
                       double price, double profitLoss, LocalDateTime dateTime) {
        this.type = type;
        this.stockSymbol = stockSymbol;
        this.quantity = quantity;
        this.price = price;
        this.profitLoss = profitLoss;
        this.dateTime = dateTime;
    }

    public String getType() {
        return type;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public double getTotalAmount() {
        return quantity * price;
    }

    public double getProfitLoss() {
        return profitLoss;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public String getFormattedDate() {
        return dateTime.format(FORMAT);
    }

    @Override
    public String toString() {
        return type + " " + quantity + " x " + stockSymbol + " @ " + price;
    }
}
