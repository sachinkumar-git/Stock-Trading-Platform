package exception;

// Parent of all our own (user defined) exceptions.
// It extends Exception so it is a CHECKED exception - compiler forces
// the caller to either catch it or declare it with throws.
public class TradingException extends Exception {

    public TradingException(String message) {
        super(message);
    }
}
