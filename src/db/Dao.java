package db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

// Generic interface. DAO = Data Access Object, the class that talks to one table.
// T is the type of object stored in that table:
//   HoldingDao     implements Dao<Holding>
//   TransactionDao implements Dao<Transaction>
// Same three operations for both, only the type changes.
public interface Dao<T> {

    void save(Connection con, String username, T item) throws SQLException;

    List<T> findAll(Connection con, String username) throws SQLException;

    void deleteAll(Connection con, String username) throws SQLException;
}
