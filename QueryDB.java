import java.sql.*;

public class QueryDB {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:data/trading.db");
        DatabaseMetaData meta = conn.getMetaData();
        ResultSet tables = meta.getTables(null, null, "%", new String[] { "TABLE" });
        System.out.println("       STOCK TRADING PLATFORM - DATABASE REPORT");
        while (tables.next()) {
            String tableName = tables.getString("TABLE_NAME");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName);
            rs.next();
            int rowCount = rs.getInt(1);
            rs.close();
            System.out.println("\nTable: " + tableName + " | Total Rows: " + rowCount);
            ResultSet data = stmt.executeQuery("SELECT * FROM " + tableName);
            ResultSetMetaData rsMeta = data.getMetaData();
            int colCount = rsMeta.getColumnCount();
            StringBuilder header = new StringBuilder();
            for (int i = 1; i <= colCount; i++) {
                header.append(String.format("%-20s", rsMeta.getColumnName(i)));
            }
            System.out.println(header);
            while (data.next()) {
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= colCount; i++) {
                    String val = data.getString(i);
                    if (val == null)
                        val = "NULL";
                    if (val.length() > 18)
                        val = val.substring(0, 18) + "..";
                    row.append(String.format("%-20s", val));
                }
                System.out.println(row);
            }
            data.close();
            stmt.close();
        }
        java.io.File dbFile = new java.io.File("data/trading.db");
        System.out.println("Database File Size: " + (dbFile.length() / 1024) + " KB");
        conn.close();
    }
}
