import db.DatabaseManager;
import javafx.application.Application;
import ui.LoginFrame;
import ui.TradingApp;

import java.sql.SQLException;

// Program starts here.
// Flow: create tables -> AWT login window -> JavaFX main window
public class Main {

    public static void main(String[] args) {

        try {
            DatabaseManager.createTables();
        } catch (SQLException e) {
            System.out.println("Could not set up the database: " + e.getMessage());
            return;
        }

        String username;

        if (args.length > 0) {
            // username given on command line, skip login (handy while testing)
            username = args[0];
        } else {
            LoginFrame login = new LoginFrame();
            username = login.waitForUsername();     // blocks till login is done
        }

        // starts JavaFX and calls TradingApp.start(). Username is passed as a parameter.
        Application.launch(TradingApp.class, username);
    }
}
