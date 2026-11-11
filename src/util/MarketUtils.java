package util;

import model.Asset;

import java.util.Collection;
import java.util.Map;

// small helper methods, all static so no object is needed
public class MarketUtils {

    private MarketUtils() {
        // private constructor - this class is only for static methods
    }

    // Generic method. Works on any map whose values can be compared
    // (V extends Comparable<V> is a bounded type - Double, Integer, String all fit).
    // Returns the entry having the biggest value as a Pair.
    public static <K, V extends Comparable<V>> Pair<K, V> maxByValue(Map<K, V> map) {
        Pair<K, V> best = null;

        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (best == null || entry.getValue().compareTo(best.getValue()) > 0) {
                best = new Pair<>(entry.getKey(), entry.getValue());
            }
        }
        return best;
    }

    // Wildcard argument: accepts List<Stock>, Set<TechStock>, Collection<Asset> ... anything
    // that holds Asset or its children. With plain Collection<Asset> a List<Stock> would not compile.
    public static double averagePrice(Collection<? extends Asset> assets) {
        if (assets.isEmpty()) {
            return 0;
        }

        double sum = 0;
        for (Asset asset : assets) {
            sum += asset.getPrice();
        }
        return sum / assets.size();
    }

    public static String money(double amount) {
        return String.format("$%,.2f", amount);
    }
}
