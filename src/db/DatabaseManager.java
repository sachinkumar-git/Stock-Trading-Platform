package db;

import model.Holding;
import model.Transaction;
import model.User;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// All JDBC work starts from here. Database is SQLite, which is just one file
// (data/trading.db) - no server to install, only the sqlite-jdbc jar in lib folder.
public class DatabaseManager {

    public static final String DATA_FOLDER = "data";
    public static final String DB_FILE = DATA_FOLDER + File.separator + "trading.db";

    // jdbc:sqlite:<file path>  - if the file is not there SQLite creates it
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    private static final HoldingDao holdingDao = new HoldingDao();
    private static final TransactionDao transactionDao = new TransactionDao();

    // static block runs once when the class is loaded - loads the JDBC driver
    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.out.println("SQLite driver missing. Check sqlite-jdbc jar in lib folder.");
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    // Runs at startup. IF NOT EXISTS means it is safe to run every time.
    public static void createTables() throws SQLException {
        File folder = new File(DATA_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // try-with-resources: connection and statement get closed automatically
        try (Connection con = getConnection();
             Statement st = con.createStatement()) {

            st.executeUpdate("CREATE TABLE IF NOT EXISTS users ("
                    + "username TEXT PRIMARY KEY, "
                    + "balance REAL NOT NULL, "
                    + "realized_pl REAL NOT NULL)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS holdings ("
                    + "username TEXT NOT NULL, "
                    + "symbol TEXT NOT NULL, "
                    + "quantity INTEGER NOT NULL, "
                    + "avg_price REAL NOT NULL, "
                    + "PRIMARY KEY (username, symbol))");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS transactions ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT NOT NULL, "
                    + "type TEXT NOT NULL, "
                    + "symbol TEXT NOT NULL, "
                    + "quantity INTEGER NOT NULL, "
                    + "price REAL NOT NULL, "
                    + "profit_loss REAL NOT NULL, "
                    + "date_time TEXT NOT NULL)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS watchlist ("
                    + "username TEXT NOT NULL, "
                    + "symbol TEXT NOT NULL, "
                    + "PRIMARY KEY (username, symbol))");
        }
    }

    // RETRIEVE (and CREATE if it is a new user)
    public static User loadOrCreateUser(String username) throws SQLException {
        try (Connection con = getConnection()) {

            User user = findUser(con, username);

            if (user == null) {
                // first time login -> new row with starting balance
                String sql = "INSERT INTO users (username, balance, realized_pl) VALUES (?, ?, 0)";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, username);
                    ps.setDouble(2, User.STARTING_BALANCE);
                    ps.executeUpdate();
                }
                return new User(username, User.STARTING_BALANCE);
            }

            // old user -> bring back portfolio, transactions and watchlist
            for (Holding holding : holdingDao.findAll(con, username)) {
                user.getPortfolio().putHolding(holding);
            }

            user.getTransactions().addAll(transactionDao.findAll(con, username));

            try (PreparedStatement ps =
                         con.prepareStatement("SELECT symbol FROM watchlist WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        user.getWatchlist().add(rs.getString("symbol"));
                    }
                }
            }
            return user;
        }
    }

    private static User findUser(Connection con, String username) throws SQLException {
        String sql = "SELECT balance, realized_pl FROM users WHERE username = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;    // no such user
                }
                User user = new User(username, rs.getDouble("balance"));
                user.setRealizedProfitLoss(rs.getDouble("realized_pl"));
                return user;
            }
        }
    }

    // Saves one buy/sell. Three tables change together:
    //   users (new balance), holdings (new quantity), transactions (new row)
    // If any one fails we rollback, otherwise money could be cut without shares being added.
    public static void saveTrade(User user, String symbol, Transaction transaction)
            throws SQLException {

        try (Connection con = getConnection()) {
            con.setAutoCommit(false);       // start a database transaction

            // nested try: outer one owns the connection, inner one decides commit / rollback
            try {
                // UPDATE
                String sql = "UPDATE users SET balance = ?, realized_pl = ? WHERE username = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setDouble(1, user.getBalance());
                    ps.setDouble(2, user.getRealizedProfitLoss());
                    ps.setString(3, user.getUsername());
                    ps.executeUpdate();
                }

                Holding holding = user.getPortfolio().getHolding(symbol);
                if (holding == null) {
                    // sold everything -> DELETE the row
                    holdingDao.delete(con, user.getUsername(), symbol);
                } else {
                    holdingDao.save(con, user.getUsername(), holding);
                }

                // CREATE
                transactionDao.save(con, user.getUsername(), transaction);

                con.commit();

            } catch (SQLException e) {
                con.rollback();     // undo whatever was done in this trade
                throw e;            // rethrow so the GUI can show the error
            }
        }
    }

    public static void addToWatchlist(String username, String symbol) throws SQLException {
        String sql = "INSERT OR IGNORE INTO watchlist (username, symbol) VALUES (?, ?)";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, symbol);
            ps.executeUpdate();
        }
    }

    public static void removeFromWatchlist(String username, String symbol) throws SQLException {
        String sql = "DELETE FROM watchlist WHERE username = ? AND symbol = ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, symbol);
            ps.executeUpdate();
        }
    }

    // File -> Reset Account. Deletes everything of this user and gives the starting balance back.
    public static void resetAccount(String username) throws SQLException {
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);

            try {
                holdingDao.deleteAll(con, username);
                transactionDao.deleteAll(con, username);

                try (PreparedStatement ps =
                             con.prepareStatement("DELETE FROM watchlist WHERE username = ?")) {
                    ps.setString(1, username);
                    ps.executeUpdate();
                }

                String sql = "UPDATE users SET balance = ?, realized_pl = 0 WHERE username = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setDouble(1, User.STARTING_BALANCE);
                    ps.setString(2, username);
                    ps.executeUpdate();
                }

                con.commit();

            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }
}
