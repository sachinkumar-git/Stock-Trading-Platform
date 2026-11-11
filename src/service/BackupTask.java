package service;

import db.DatabaseManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.Consumer;

// Copies the database file to data/backup using byte streams.
// Done in a separate thread so the window does not freeze while copying.
// Here the thread is made by extending Thread class (PriceUpdater uses Runnable).
public class BackupTask extends Thread {

    public static final String BACKUP_FOLDER = "data" + File.separator + "backup";
    public static final String BACKUP_FILE = BACKUP_FOLDER + File.separator + "trading_backup.db";

    private final Consumer<String> onDone;    // gets the result message when copy is over

    public BackupTask(Consumer<String> onDone) {
        super("backup-thread");
        this.onDone = onDone;

        // backup is not urgent, so lowest priority
        setPriority(Thread.MIN_PRIORITY);
    }

    @Override
    public void run() {
        FileInputStream in = null;
        FileOutputStream out = null;
        String result;

        try {
            File folder = new File(BACKUP_FOLDER);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            // .db is a binary file, so byte streams (not Reader/Writer which are for text)
            in = new FileInputStream(DatabaseManager.DB_FILE);
            out = new FileOutputStream(BACKUP_FILE);

            byte[] buffer = new byte[4096];
            int bytesRead;
            long total = 0;

            // read() returns -1 when file is finished
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                total += bytesRead;
            }

            result = "Backup saved to " + BACKUP_FILE + " (" + total + " bytes)";

        } catch (FileNotFoundException e) {
            // multiple catch blocks: child exception first, parent (IOException) after
            result = "Backup failed, database file not found.";

        } catch (IOException e) {
            result = "Backup failed: " + e.getMessage();

        } finally {
            // finally runs whether copy worked or failed, so streams always get closed
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                System.out.println("Could not close backup streams: " + e.getMessage());
            }
        }

        onDone.accept(result);
    }
}
