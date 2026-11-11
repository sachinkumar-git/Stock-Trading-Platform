package ui;

import java.awt.Button;
import java.awt.Color;
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

// Small login window made with AWT.
// Delegation event model: the component (source) does not handle the event itself,
// it passes (delegates) it to a listener object that we register on it.
//   source   -> loginButton, nameField, the frame
//   event    -> ActionEvent, KeyEvent, WindowEvent
//   listener -> this class (ActionListener), KeyAdapter, WindowAdapter
public class LoginFrame extends Frame implements ActionListener {

    private final TextField nameField = new TextField(20);
    private final Label errorLabel = new Label("", Label.CENTER);

    private String username;    // stays null until a valid name is entered

    public LoginFrame() {
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
                System.exit(0);
            }
        });

        setSize(360, 200);
        setLocationRelativeTo(null);    // centre of the screen
        setVisible(true);
    }

    // from ActionListener - runs when Login button is clicked
    @Override
    public void actionPerformed(ActionEvent e) {
        tryLogin();
    }

    private synchronized void tryLogin() {
        String text = nameField.getText().trim();

        if (text.isEmpty()) {
            errorLabel.setText("Username cannot be empty");
            return;
        }
        if (!text.matches("[A-Za-z0-9_]+")) {
            errorLabel.setText("Only letters, digits and _ allowed");
            return;
        }

        username = text;
        notify();   // wake up the main thread waiting in waitForUsername()
    }

    // main thread calls this and sleeps here until the user logs in.
    // The click is handled on AWT's own event thread, which then calls notify().
    public synchronized String waitForUsername() {
        while (username == null) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "guest";
            }
        }
        dispose();      // close the login window
        return username;
    }
}
