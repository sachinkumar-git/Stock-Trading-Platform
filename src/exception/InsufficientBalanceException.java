package exception;

// thrown from User.buy() when the order costs more than the cash available
public class InsufficientBalanceException extends TradingException {

    private final double required;
    private final double available;

    public InsufficientBalanceException(double required, double available) {
        super(String.format("Not enough balance. Need $%.2f but only $%.2f available.",
                required, available));
        this.required = required;
        this.available = available;
    }

    public double getShortfall() {
        return required - available;
    }
}
