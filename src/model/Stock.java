package model;

// Stock IS-A Asset (inheritance). Normal company share.
public class Stock extends Asset {

    private final String sector;

    public Stock(String symbol, String companyName, double price, String sector) {
        super(symbol, companyName, price);   // calls the Asset constructor
        this.sector = sector;
    }

    @Override
    public String getCategory() {
        return sector;
    }

    // normal stocks move max 2% per update
    @Override
    public double getVolatility() {
        return 0.02;
    }
}
