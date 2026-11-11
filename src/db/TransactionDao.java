package db;

import model.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// handles the "transactions" table
public class TransactionDao implements Dao<Transaction> {

    @Override
    public void save(Connection con, String username, Transaction t) throws SQLException {
        String sql = "INSERT INTO transactions "
                + "(username, type, symbol, quantity, price, profit_loss, date_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, t.getType());
            ps.setString(3, t.getStockSymbol());
            ps.setInt(4, t.getQuantity());
            ps.setDouble(5, t.getPrice());
            ps.setDouble(6, t.getProfitLoss());
            ps.setString(7, t.getDateTime().toString());    // SQLite has no date type, stored as text
            ps.executeUpdate();
        }
    }

    @Override
    public List<Transaction> findAll(Connection con, String username) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT type, symbol, quantity, price, profit_loss, date_time "
                + "FROM transactions WHERE username = ? ORDER BY id";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // second constructor of Transaction (the one that takes the date)
                    list.add(new Transaction(
                            rs.getString("type"),
                            rs.getString("symbol"),
                            rs.getInt("quantity"),
                            rs.getDouble("price"),
                            rs.getDouble("profit_loss"),
                            LocalDateTime.parse(rs.getString("date_time"))));
                }
            }
        }
        return list;
    }

    @Override
    public void deleteAll(Connection con, String username) throws SQLException {
        try (PreparedStatement ps =
                     con.prepareStatement("DELETE FROM transactions WHERE username = ?")) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
    }
}
