package db;

import model.Holding;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// handles the "holdings" table
public class HoldingDao implements Dao<Holding> {

    // INSERT OR REPLACE: if (username, symbol) row is already there it gets replaced,
    // so the same query works for first buy and for buying more later.
    @Override
    public void save(Connection con, String username, Holding holding) throws SQLException {
        String sql = "INSERT OR REPLACE INTO holdings (username, symbol, quantity, avg_price) "
                + "VALUES (?, ?, ?, ?)";

        // PreparedStatement with ? placeholders - values are never joined into the
        // query string, so no SQL injection problem
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, holding.getSymbol());
            ps.setInt(3, holding.getQuantity());
            ps.setDouble(4, holding.getAverageBuyPrice());
            ps.executeUpdate();
        }
    }

    @Override
    public List<Holding> findAll(Connection con, String username) throws SQLException {
        List<Holding> list = new ArrayList<>();
        String sql = "SELECT symbol, quantity, avg_price FROM holdings WHERE username = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Holding(
                            rs.getString("symbol"),
                            rs.getInt("quantity"),
                            rs.getDouble("avg_price")));
                }
            }
        }
        return list;
    }

    // when all shares of one stock are sold, its row is removed
    public void delete(Connection con, String username, String symbol) throws SQLException {
        String sql = "DELETE FROM holdings WHERE username = ? AND symbol = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, symbol);
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteAll(Connection con, String username) throws SQLException {
        try (PreparedStatement ps =
                     con.prepareStatement("DELETE FROM holdings WHERE username = ?")) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
    }
}
