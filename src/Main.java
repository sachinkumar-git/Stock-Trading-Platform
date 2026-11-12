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
            username = args[0].trim();
            if (!LoginFrame.isValidUsername(username)) {
                System.out.println("Invalid username \"" + args[0]
                        + "\". Only letters, digits and _ are allowed.");
                System.exit(1);
            }
        } else {
            // opens the AWT login window and blocks till login is done
            username = LoginFrame.showAndWait();
            if (username == null) {
                System.out.println("Login cancelled.");
                return;
            }
        }

        // Starts JavaFX and calls TradingApp.start(). Username is passed as a parameter.
        // This is the only place JavaFX is launched, and launch() can run only once per program.
        // It blocks until the JavaFX window is closed.
        try {
            Application.launch(TradingApp.class, username);
        } catch (RuntimeException e) {
            // e.g. an exception thrown inside TradingApp.start() - report it instead of failing silently
            System.out.println("Could not start the trading window: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}
