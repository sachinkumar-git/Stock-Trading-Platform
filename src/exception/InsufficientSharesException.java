package exception;

// thrown when user tries to sell more shares than he owns
public class InsufficientSharesException extends TradingException {

    public InsufficientSharesException(String symbol, int requested, int owned) {
        super("Cannot sell " + requested + " shares of " + symbol
                + ". You own only " + owned + ".");
    }
}
