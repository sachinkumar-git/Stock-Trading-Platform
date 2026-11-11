package exception;

// thrown by StockMarket.getStock() when the symbol is not listed
public class StockNotFoundException extends TradingException {

    public StockNotFoundException(String symbol) {
        super("Stock not found: " + symbol);
    }
}
