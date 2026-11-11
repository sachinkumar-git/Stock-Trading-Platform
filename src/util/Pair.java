package util;

// Generic class with two type parameters.
// K and V are decided by whoever uses it, eg. Pair<String, Double> for (symbol, change %).
// Saves us from writing a separate small class every time we want to return 2 values.
public class Pair<K, V> {

    private final K key;
    private final V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    @Override
    public String toString() {
        return key + " = " + value;
    }
}
