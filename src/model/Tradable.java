package model;

// Interface = only says WHAT a tradable thing must provide, not how.
// Asset implements this, so every Stock / TechStock is also a Tradable.
public interface Tradable {

    String getSymbol();

    String getCompanyName();

    double getPrice();
}
