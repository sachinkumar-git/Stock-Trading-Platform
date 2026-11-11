package ui;

import db.DatabaseManager;
import exception.TradingException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import model.Asset;
import model.Holding;
import model.Transaction;
import model.User;
import service.BackupTask;
import service.PriceUpdater;
import service.StockMarket;
import util.MarketUtils;
import util.Pair;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

// Main window (JavaFX).
// Every JavaFX program extends Application and overrides start().
// Stage = the window, Scene = what is inside it, and controls are arranged in layout panes.
public class TradingApp extends Application {

    private StockMarket market;
    private User user;
    private PriceUpdater priceUpdater;

    // controls that other methods need to update are kept as fields
    private final Label balanceLabel = new Label();
    private final Label statusLabel = new Label();
    private final Label messageLabel = new Label();
    private final Label priceLabel = new Label("Select a stock");
    private final Label summaryLabel = new Label();

    private final TableView<Asset> marketTable = new TableView<>();
    private final TableView<Holding> portfolioTable = new TableView<>();
    private final TableView<Transaction> transactionTable = new TableView<>();

    private final ComboBox<String> stockBox = new ComboBox<>();
    private final RadioButton buyRadio = new RadioButton("Buy");
    private final RadioButton sellRadio = new RadioButton("Sell");
    private final TextField quantityField = new TextField();
    private final ListView<String> historyList = new ListView<>();

    @Override
    public void start(Stage stage) {

        // username comes from Main through Application.launch(...)
        String username = getParameters().getRaw().isEmpty()
                ? "guest"
                : getParameters().getRaw().get(0);

        market = new StockMarket();

        try {
            user = DatabaseManager.loadOrCreateUser(username);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database error",
                    "Could not load account: " + e.getMessage());
            Platform.exit();
            return;
        }

        // BorderPane has 5 areas: top, left, center, right, bottom
        BorderPane root = new BorderPane();
        root.setTop(new VBox(buildMenuBar(), buildToolBar()));
        root.setLeft(buildStockTree());
        root.setCenter(buildTabs());
        root.setRight(buildTradePanel());
        root.setBottom(statusLabel);

        statusLabel.setPadding(new Insets(5, 10, 5, 10));

        stage.setTitle("Stock Trading Platform - " + user.getUsername());
        stage.setScene(new Scene(root, 1180, 640));
        stage.show();

        refreshAccount();
        startPriceThread();
    }

    // JavaFX calls stop() when the window is closed
    @Override
    public void stop() {
        if (priceUpdater != null) {
            priceUpdater.stop();
        }
    }

    private void startPriceThread() {
        // The updater runs on its own thread. It is NOT allowed to touch JavaFX controls
        // directly (only the JavaFX Application Thread can), so it hands the refresh
        // to Platform.runLater which runs it on the GUI thread.
        priceUpdater = new PriceUpdater(market, () -> Platform.runLater(this::refreshPrices));

        Thread thread = new Thread(priceUpdater, "price-updater");
        thread.setDaemon(true);                             // dies automatically when app closes
        thread.setPriority(Thread.NORM_PRIORITY - 1);       // GUI is more important than this
        thread.start();
    }

    // ------------------------------------------------------------------ menu + toolbar

    // MenuBar holds Menus, a Menu holds MenuItems
    private MenuBar buildMenuBar() {

        MenuItem backupItem = new MenuItem("Backup Database");
        backupItem.setAccelerator(KeyCombination.keyCombination("Ctrl+B"));    // shortcut key
        backupItem.setGraphic(new Circle(5, Color.SEAGREEN));                  // small icon
        backupItem.setOnAction(e -> runBackup());

        MenuItem resetItem = new MenuItem("Reset Account");
        resetItem.setGraphic(new Circle(5, Color.INDIANRED));
        resetItem.setOnAction(e -> resetAccount());

        MenuItem exitItem = new MenuItem("Exit");
        // same thing as the lambdas above but written the long way with an
        // anonymous inner class, to show what the lambda actually stands for
        exitItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Platform.exit();
            }
        });

        Menu fileMenu = new Menu("File");
        fileMenu.getItems().addAll(backupItem, resetItem, new SeparatorMenuItem(), exitItem);

        // CheckMenuItem = menu item with a tick mark
        CheckMenuItem pauseItem = new CheckMenuItem("Pause Price Updates");
        pauseItem.setOnAction(e -> {
            if (pauseItem.isSelected()) {
                priceUpdater.pause();
                showMessage("Price updates paused.", true);
            } else {
                priceUpdater.resume();
                showMessage("Price updates resumed.", true);
            }
        });

        MenuItem watchItem = new MenuItem("Add / Remove from Watchlist");
        watchItem.setAccelerator(KeyCombination.keyCombination("Ctrl+W"));
        watchItem.setOnAction(e -> toggleWatchlist());

        Menu marketMenu = new Menu("Market");
        marketMenu.getItems().addAll(pauseItem, watchItem);

        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAlert(Alert.AlertType.INFORMATION, "About",
                "Stock Trading Platform\n"
                        + "Advanced Java Programming project\n\n"
                        + "Stocks listed: " + Asset.getTotalAssets() + "\n"
                        + "Prices are simulated and change every "
                        + (PriceUpdater.INTERVAL_MS / 1000) + " seconds."));

        Menu helpMenu = new Menu("Help");
        helpMenu.getItems().add(aboutItem);

        return new MenuBar(fileMenu, marketMenu, helpMenu);
    }

    private ToolBar buildToolBar() {
        Label title = new Label("STOCK TRADING PLATFORM");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        title.setEffect(new DropShadow(4, Color.GRAY));     // effect on a node

        Label userLabel = new Label("User: " + user.getUsername());
        balanceLabel.setStyle("-fx-font-weight: bold;");

        Button watchButton = new Button("Watch / Unwatch");
        watchButton.setOnAction(e -> toggleWatchlist());

        Button backupButton = new Button("Backup");
        backupButton.setOnAction(e -> runBackup());

        // empty box that grows, pushes the buttons to the right side
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return new ToolBar(title, new Separator(), userLabel, new Separator(),
                balanceLabel, spacer, watchButton, backupButton);
    }

    // ------------------------------------------------------------------ left side tree

    // TreeView: Market -> sector -> stock symbols. Clicking a symbol selects it in the order form.
    private TreeView<String> buildStockTree() {
        TreeItem<String> rootItem = new TreeItem<>("Market");
        rootItem.setExpanded(true);

        // sector name -> its tree node, so each sector node is made only once
        Map<String, TreeItem<String>> sectorNodes = new HashMap<>();

        for (Asset asset : market.getAllStocks()) {
            String sector = asset.getCategory();    // Stock / TechStock decide this

            TreeItem<String> sectorNode = sectorNodes.get(sector);
            if (sectorNode == null) {
                sectorNode = new TreeItem<>(sector);
                sectorNode.setExpanded(true);
                sectorNodes.put(sector, sectorNode);
                rootItem.getChildren().add(sectorNode);
            }
            sectorNode.getChildren().add(new TreeItem<>(asset.getSymbol()));
        }

        TreeView<String> tree = new TreeView<>(rootItem);
        tree.setPrefWidth(170);

        // listener fires whenever the selected tree item changes
        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            // only leaf nodes are stocks, "Market" and sector names are not
            if (newItem != null && newItem.isLeaf()) {
                stockBox.setValue(newItem.getValue());
            }
        });
        return tree;
    }

    // ------------------------------------------------------------------ center tabs

    private TabPane buildTabs() {

        // --- market tab
        marketTable.getColumns().add(column("Symbol", 80, (Asset a) -> a.getSymbol()));
        marketTable.getColumns().add(column("Company", 180, (Asset a) -> a.getCompanyName()));
        marketTable.getColumns().add(column("Sector", 100, (Asset a) -> a.getCategory()));
        marketTable.getColumns().add(column("Price", 100,
                (Asset a) -> MarketUtils.money(a.getPrice())));
        marketTable.getColumns().add(column("Change", 90,
                (Asset a) -> String.format("%+.2f%%", a.getChangePercent())));
        marketTable.getColumns().add(column("Watch", 60,
                (Asset a) -> user.isWatching(a.getSymbol()) ? "★" : ""));

        marketTable.getItems().addAll(market.getAllStocks());

        // clicking a row fills the stock in the order form
        marketTable.getSelectionModel().selectedItemProperty().addListener((obs, old, asset) -> {
            if (asset != null) {
                stockBox.setValue(asset.getSymbol());
            }
        });

        // --- portfolio tab
        portfolioTable.getColumns().add(column("Symbol", 80, (Holding h) -> h.getSymbol()));
        portfolioTable.getColumns().add(column("Quantity", 80,
                (Holding h) -> String.valueOf(h.getQuantity())));
        portfolioTable.getColumns().add(column("Avg Buy Price", 120,
                (Holding h) -> MarketUtils.money(h.getAverageBuyPrice())));
        portfolioTable.getColumns().add(column("Current Price", 120,
                (Holding h) -> MarketUtils.money(currentPrice(h.getSymbol()))));
        portfolioTable.getColumns().add(column("Profit / Loss", 120,
                (Holding h) -> MarketUtils.money(
                        (currentPrice(h.getSymbol()) - h.getAverageBuyPrice()) * h.getQuantity())));
        portfolioTable.setPlaceholder(new Label("No stocks yet. Buy something from the right side."));

        summaryLabel.setPadding(new Insets(8));
        VBox portfolioBox = new VBox(portfolioTable, summaryLabel);
        VBox.setVgrow(portfolioTable, Priority.ALWAYS);

        // --- transactions tab
        transactionTable.getColumns().add(column("Type", 60, (Transaction t) -> t.getType()));
        transactionTable.getColumns().add(column("Symbol", 80,
                (Transaction t) -> t.getStockSymbol()));
        transactionTable.getColumns().add(column("Qty", 60,
                (Transaction t) -> String.valueOf(t.getQuantity())));
        transactionTable.getColumns().add(column("Price", 100,
                (Transaction t) -> MarketUtils.money(t.getPrice())));
        transactionTable.getColumns().add(column("Total", 110,
                (Transaction t) -> MarketUtils.money(t.getTotalAmount())));
        transactionTable.getColumns().add(column("Profit / Loss", 110,
                (Transaction t) -> MarketUtils.money(t.getProfitLoss())));
        transactionTable.getColumns().add(column("Date & Time", 160,
                (Transaction t) -> t.getFormattedDate()));
        transactionTable.setPlaceholder(new Label("No transactions yet."));

        Tab marketTab = new Tab("Market", marketTable);
        Tab portfolioTab = new Tab("Portfolio", portfolioBox);
        Tab transactionTab = new Tab("Transactions", transactionTable);

        TabPane tabs = new TabPane(marketTab, portfolioTab, transactionTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);    // no X on tabs
        return tabs;
    }

    // Generic method: <T> is the row type of the table (Asset, Holding or Transaction).
    // One helper builds columns for all three tables, "getter" says what text to show for a row.
    private <T> TableColumn<T, String> column(String title, int width, Function<T, String> getter) {
        TableColumn<T, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellValueFactory(cell -> new SimpleStringProperty(getter.apply(cell.getValue())));
        return col;
    }

    private double currentPrice(String symbol) {
        Asset asset = market.getStocks().get(symbol);
        return asset == null ? 0 : asset.getPrice();
    }

    // ------------------------------------------------------------------ right side order form

    private VBox buildTradePanel() {
        Label heading = new Label("Place Order");
        heading.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // ComboBox = drop down list. TreeSet gives the symbols already sorted.
        stockBox.getItems().addAll(market.getSortedSymbols());
        stockBox.setPromptText("Choose stock");
        stockBox.setMaxWidth(Double.MAX_VALUE);
        stockBox.setOnAction(e -> showSelectedStock());

        // both radio buttons in one ToggleGroup so only one can be selected at a time
        ToggleGroup orderType = new ToggleGroup();
        buyRadio.setToggleGroup(orderType);
        sellRadio.setToggleGroup(orderType);
        buyRadio.setSelected(true);

        quantityField.setPromptText("Quantity");
        // pressing Enter inside the text field also places the order
        quantityField.setOnAction(e -> placeOrder(false));

        Button orderButton = new Button("Place Order");
        orderButton.setOnAction(e -> placeOrder(false));

        Button sellAllButton = new Button("Sell All");
        sellAllButton.setOnAction(e -> placeOrder(true));

        messageLabel.setWrapText(true);

        historyList.setPrefHeight(230);
        historyList.setPlaceholder(new Label("Select a stock"));

        VBox panel = new VBox(10,
                heading,
                stockBox,
                priceLabel,
                new HBox(15, buyRadio, sellRadio),
                quantityField,
                new HBox(10, orderButton, sellAllButton),
                messageLabel,
                new Separator(),
                new Label("Recent prices (newest last)"),
                historyList);

        panel.setPadding(new Insets(12));
        panel.setPrefWidth(250);
        return panel;
    }

    // ------------------------------------------------------------------ event handlers

    // sellEverything = true when "Sell All" was clicked
    private void placeOrder(boolean sellEverything) {
        String symbol = stockBox.getValue();

        if (symbol == null) {
            showMessage("Please choose a stock first.", false);
            return;
        }

        try {
            Asset asset = market.getStock(symbol);
            Transaction transaction;

            if (sellEverything) {
                transaction = user.sell(asset);                 // overloaded sell()
            } else {
                // parseInt throws NumberFormatException for "abc" or empty text
                int quantity = Integer.parseInt(quantityField.getText().trim());

                if (buyRadio.isSelected()) {
                    transaction = user.buy(asset, quantity);
                } else {
                    transaction = user.sell(asset, quantity);
                }
            }

            DatabaseManager.saveTrade(user, symbol, transaction);

            showMessage("Done: " + transaction + "  (total "
                    + MarketUtils.money(transaction.getTotalAmount()) + ")", true);

        } catch (NumberFormatException e) {
            // must come before IllegalArgumentException because it is its child class,
            // otherwise compiler says "exception already caught"
            showMessage("Quantity must be a whole number.", false);

        } catch (IllegalArgumentException e) {
            showMessage(e.getMessage(), false);

        } catch (TradingException e) {
            // one catch for all our own exceptions (InsufficientBalance,
            // InsufficientShares, StockNotFound) since TradingException is their parent
            showMessage(e.getMessage(), false);

        } catch (SQLException e) {
            showMessage("Trade done but could not be saved: " + e.getMessage(), false);

        } finally {
            // runs in every case, success or any exception
            quantityField.clear();
            refreshAccount();
        }
    }

    private void toggleWatchlist() {
        String symbol = stockBox.getValue();

        if (symbol == null) {
            showMessage("Choose a stock to add to the watchlist.", false);
            return;
        }

        try {
            boolean nowWatching = user.toggleWatch(symbol);

            if (nowWatching) {
                DatabaseManager.addToWatchlist(user.getUsername(), symbol);
                showMessage(symbol + " added to watchlist.", true);
            } else {
                DatabaseManager.removeFromWatchlist(user.getUsername(), symbol);
                showMessage(symbol + " removed from watchlist.", true);
            }
        } catch (SQLException e) {
            showMessage("Could not save watchlist: " + e.getMessage(), false);
        }
        marketTable.refresh();
    }

    private void runBackup() {
        // BackupTask is a Thread. When it finishes it gives us a message, and again
        // we go through Platform.runLater because we are on the backup thread at that point.
        BackupTask task = new BackupTask(result ->
                Platform.runLater(() -> showMessage(result, !result.contains("failed"))));
        task.start();
    }

    private void resetAccount() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "This deletes your portfolio, transactions and watchlist and sets the balance "
                        + "back to " + MarketUtils.money(User.STARTING_BALANCE) + ". Continue?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Reset account");

        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.YES) {
            return;
        }

        try {
            DatabaseManager.resetAccount(user.getUsername());
            user = DatabaseManager.loadOrCreateUser(user.getUsername());    // fresh copy
            showMessage("Account reset.", true);
        } catch (SQLException e) {
            showMessage("Reset failed: " + e.getMessage(), false);
        }
        refreshAccount();
    }

    // ------------------------------------------------------------------ refreshing the screen

    // after a trade / reset: table rows themselves changed
    private void refreshAccount() {
        portfolioTable.getItems().setAll(user.getPortfolio().getHoldings());
        transactionTable.getItems().setAll(user.getTransactions());
        refreshPrices();
    }

    // after every price update: same rows, only the numbers inside changed
    private void refreshPrices() {
        marketTable.refresh();
        portfolioTable.refresh();

        balanceLabel.setText("Balance: " + MarketUtils.money(user.getBalance()));

        double invested = user.getPortfolio().getTotalInvestment();
        double value = user.getPortfolio().getCurrentValue(market.getStocks());
        double unrealized = user.getPortfolio().getUnrealizedProfitLoss(market.getStocks());

        summaryLabel.setText("Invested: " + MarketUtils.money(invested)
                + "     Current value: " + MarketUtils.money(value)
                + "     Unrealized P/L: " + MarketUtils.money(unrealized)
                + "     Realized P/L: " + MarketUtils.money(user.getRealizedProfitLoss()));

        // Pair<String, Double> = (symbol, change %)
        Pair<String, Double> top = market.getTopGainer();
        statusLabel.setText(String.format("Top gainer: %s (%+.2f%%)     Average market price: %s",
                top.getKey(), top.getValue(),
                MarketUtils.money(MarketUtils.averagePrice(market.getStocks().values()))));

        showSelectedStock();
    }

    private void showSelectedStock() {
        String symbol = stockBox.getValue();
        if (symbol == null) {
            return;
        }

        Asset asset = market.getStocks().get(symbol);

        priceLabel.setText(asset.getCompanyName() + "\nPrice: " + MarketUtils.money(asset.getPrice())
                + "   You own: " + user.getPortfolio().getQuantity(symbol));

        historyList.getItems().clear();
        for (double price : asset.getPriceHistory()) {
            historyList.getItems().add(MarketUtils.money(price));
        }
    }

    private void showMessage(String text, boolean success) {
        messageLabel.setText(text);
        messageLabel.setTextFill(success ? Color.GREEN : Color.RED);
    }

    private void showAlert(Alert.AlertType type, String header, String text) {
        Alert alert = new Alert(type, text);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}
