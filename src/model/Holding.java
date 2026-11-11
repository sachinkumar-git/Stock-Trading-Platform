package model;

// One row of the portfolio: which stock, how many shares, and at what average price we bought.
public class Holding {

    private final String symbol;
    private int quantity;
    private double averageBuyPrice;

    public Holding(String symbol, int quantity, double averageBuyPrice) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.averageBuyPrice = averageBuyPrice;
    }

    // buying more of the same stock changes the average price
    // eg. 10 shares @100 + 10 shares @200  ->  20 shares @150
    public void addShares(int moreQuantity, double price) {
        double oldInvestment = quantity * averageBuyPrice;
        double newInvestment = moreQuantity * price;

        quantity = quantity + moreQuantity;
        averageBuyPrice = (oldInvestment + newInvestment) / quantity;
    }

    // selling does not change the average buy price, only the count
    public void removeShares(int soldQuantity) {
        quantity = quantity - soldQuantity;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getAverageBuyPrice() {
        return averageBuyPrice;
    }

    public double getInvestedAmount() {
        return quantity * averageBuyPrice;
    }
}
