package ui;

import java.awt.Button;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;

// Small login window made with AWT.
// Delegation event model: the component (source) does not handle the event itself,
// it passes (delegates) it to a listener object that we register on it.
//   source   -> loginButton, nameField, the frame
//   event    -> ActionEvent, KeyEvent, WindowEvent
//   listener -> this class (ActionListener), KeyAdapter, WindowAdapter
//
// Threads involved:
//   main thread      -> calls showAndWait() and sleeps in wait() until the user is done
//   AWT event thread -> builds the window, handles the click, closes the window,
//                       then wakes the main thread with notifyAll()
// The two threads share "username" and "finished", always guarded by "lock".
public class LoginFrame extends Frame implements ActionListener {

    private static final String VALID_NAME = "[A-Za-z0-9_]+";

    private final TextField nameField = new TextField(20);
    private final Label errorLabel = new Label("", Label.CENTER);

    // Separate private object used as the monitor for wait()/notifyAll().
    // We must NOT use the frame itself ("this"): AWT locks the Frame object internally
    // (e.g. Frame.getState() is synchronized), so holding that lock while asking the
    // event thread to dispose the window makes both threads wait for each other forever.
    private final Object lock = new Object();
    private String username;        // set once a valid name is entered, null if cancelled
    private boolean finished;       // true after login or after the window is closed

    // Private: build the window only through showAndWait(), which does it on the event thread.
    private LoginFrame() {
        super("Stock Trading Platform - Login");

        setLayout(new GridLayout(4, 1, 5, 5));

        Label heading = new Label("Enter your username", Label.CENTER);
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));

        Button loginButton = new Button("Login");
        errorLabel.setForeground(Color.RED);

        add(heading);
        add(nameField);
        add(loginButton);
        add(errorLabel);

        // this class itself is the listener, see actionPerformed() below
        loginButton.addActionListener(this);

        // KeyListener has 3 methods (keyPressed, keyReleased, keyTyped).
        // We need only keyPressed, so KeyAdapter is used - it has empty versions
        // of all three and we override just the one we want.
        nameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    tryLogin();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    nameField.setText("");
                }
            }
        });

        // same idea, WindowListener has 7 methods, WindowAdapter lets us write only one.
        // Without this the X button of an AWT frame does nothing.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                finish(null);       // user closed the window: login cancelled
            }
        });

        setSize(360, 200);
        setLocationRelativeTo(null);    // centre of the screen
    }

    // Shows the login window and blocks the calling thread until the user logs in.
    // Returns the username, or null if the window was closed without logging in.
    // Must not be called on the AWT event thread, because that thread has to stay
    // free to handle the clicks we are waiting for.
    public static String showAndWait() {
        if (EventQueue.isDispatchThread()) {
            throw new IllegalStateException("showAndWait() must not be called on the AWT event thread");
        }

        LoginFrame[] holder = new LoginFrame[1];
        try {
            // AWT components should be created and shown on the event thread
            EventQueue.invokeAndWait(() -> {
                holder[0] = new LoginFrame();
                holder[0].setVisible(true);
                holder[0].nameField.requestFocus();
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Could not open the login window", e.getCause());
        }
        return holder[0].waitForUsername();
    }

    public static boolean isValidUsername(String name) {
        return name != null && name.matches(VALID_NAME);
    }

    // from ActionListener - runs when Login button is clicked
    @Override
    public void actionPerformed(ActionEvent e) {
        tryLogin();
    }

    // runs on the AWT event thread (button click or Enter key)
    private void tryLogin() {
        String text = nameField.getText().trim();

        if (text.isEmpty()) {
            errorLabel.setText("Username cannot be empty");
            return;
        }
        if (!isValidUsername(text)) {
            errorLabel.setText("Only letters, digits and _ allowed");
            return;
        }

        finish(text);
    }

    // Runs on the AWT event thread. Closes the window and wakes the waiting main thread.
    // dispose() is called here, on the event thread and without holding "lock",
    // so it can never block on the main thread.
    private void finish(String name) {
        synchronized (lock) {
            if (finished) {
                return;     // double click / Enter + click: only the first one counts
            }
        }

        dispose();      // close the login window

        synchronized (lock) {
            username = name;
            finished = true;
            lock.notifyAll();   // wake up the main thread waiting in waitForUsername()
        }
    }

    // Main thread sleeps here until the user logs in or closes the window.
    // wait() is inside a while loop because a thread can wake up without being
    // notified (spurious wakeup), so the condition is checked again every time.
    private String waitForUsername() {
        synchronized (lock) {
            while (!finished) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    EventQueue.invokeLater(this::dispose);  // don't leave the window open
                    return null;
                }
            }
            return username;
        }
    }
}
