# Stock Trading Platform

A desktop stock trading simulator written in Java. Users log in, trade a fixed set of stocks at prices that move in real time, and track their portfolio, transaction history and profit or loss. All account data is stored in a local SQLite database.

Built as the course project for **Advanced Java Programming**. The application is designed so that each unit of the syllabus is used for a real feature rather than as a separate example.

> Market data is simulated. The listed companies are predefined and their prices are generated randomly by a background thread. No real market feed is used.

## Features

- **Login** with a username; each user has a separate account starting with $100,000
- **Live market view** with prices that update every 3 seconds
- **Buy, sell and sell-all** orders with validation and clear error messages
- **Portfolio** with average buy price, current value, and realized and unrealized profit or loss
- **Transaction history** for every trade
- **Watchlist** to mark stocks of interest
- **Price history** of the last 10 prices for the selected stock
- **Pause and resume** of market price updates
- **Database backup** to a separate file
- **Account reset** back to the starting balance
- **Persistent storage**: balance, holdings, transactions and watchlist are restored on the next login

## Technology

| Purpose | Technology |
|---|---|
| Language | Java (JDK 24 or newer; developed on JDK 26) |
| Main user interface | JavaFX 26 |
| Login window | AWT |
| Database | SQLite through JDBC (`sqlite-jdbc` 3.54.1.0) |
| Build | `javac` through `run.bat`; no build tool required |

## Getting started

### Requirements

- Windows with a JDK (version 24 or newer) installed and `java` / `javac` available on the `PATH`
- The `lib` folder from this repository, which contains the JavaFX and SQLite JDBC jars

### Run

From the project folder:

```
.\run.bat
```

The script compiles the sources into `out` and starts the application. To skip the login window, pass a username:

```
.\run.bat username
```

The database file `data/trading.db` is created automatically on first run.

## Using the application

1. Enter a username in the login window and press **Enter** or click **Login**.
2. Pick a stock from the tree on the left, the market table, or the drop-down in the order form.
3. Choose **Buy** or **Sell**, enter a quantity and click **Place Order**. **Sell All** sells every share of the selected stock.
4. Open the **Portfolio** and **Transactions** tabs to review holdings and past trades.
5. Use the menus for the remaining actions:

| Menu | Item | Action |
|---|---|---|
| File | Backup Database (`Ctrl+B`) | Copies the database to `data/backup/trading_backup.db` |
| File | Reset Account | Clears holdings, transactions and watchlist and restores the starting balance |
| Market | Pause Price Updates | Stops and restarts the price thread |
| Market | Add / Remove from Watchlist (`Ctrl+W`) | Toggles the selected stock in the watchlist |
| Help | About | Shows application information |

## Project structure

```text
StockTradingPlatform/
├── src/
│   ├── Main.java                  Entry point: database setup, login, launch
│   ├── model/                     Domain classes
│   │   ├── Tradable.java          Interface for anything that can be traded
│   │   ├── Asset.java             Abstract base class with price and price history
│   │   ├── Stock.java             Regular stock
│   │   ├── TechStock.java         Technology stock with higher volatility
│   │   ├── Holding.java           One position in a portfolio
│   │   ├── Portfolio.java         All holdings of a user
│   │   ├── Transaction.java       One buy or sell record
│   │   └── User.java              Account, trading logic and watchlist
│   ├── exception/                 User-defined exceptions
│   ├── service/
│   │   ├── StockMarket.java       Listed stocks and price simulation
│   │   ├── PriceUpdater.java      Background thread that updates prices
│   │   └── BackupTask.java        Background thread that backs up the database
│   ├── db/
│   │   ├── DatabaseManager.java   Connection, table creation, account operations
│   │   ├── Dao.java               Generic data-access interface
│   │   ├── HoldingDao.java        `holdings` table
│   │   └── TransactionDao.java    `transactions` table
│   ├── util/
│   │   ├── Pair.java              Generic two-value container
│   │   └── MarketUtils.java       Generic helper methods and formatting
│   └── ui/
│       ├── LoginFrame.java        AWT login window
│       └── TradingApp.java        JavaFX main window
├── lib/                           JavaFX and SQLite JDBC jars
├── data/                          Database and backups (created at run time)
└── run.bat                        Compile and run script
```

## Database

SQLite database stored in `data/trading.db`.

| Table | Columns | Purpose |
|---|---|---|
| `users` | `username` (PK), `balance`, `realized_pl` | One row per account |
| `holdings` | `username`, `symbol` (composite PK), `quantity`, `avg_price` | Shares currently owned |
| `transactions` | `id` (PK), `username`, `type`, `symbol`, `quantity`, `price`, `profit_loss`, `date_time` | Trade history |
| `watchlist` | `username`, `symbol` (composite PK) | Watched stocks |

Each trade updates `users`, `holdings` and `transactions` inside a single database transaction, and is rolled back if any statement fails.

## Syllabus coverage

| Unit | Topic | Where it is used |
|---|---|---|
| 1 | Classes, objects, constructors, `this`, `static`, `final` | `model` package, `Asset.java` |
| 1 | Inheritance, `super`, multilevel hierarchy | `Asset` → `Stock` → `TechStock` |
| 1 | Abstract class and interface | `Asset.java`, `Tradable.java` |
| 1 | Packages and access specifiers | `model`, `exception`, `service`, `db`, `util`, `ui` |
| 1 | Arrays | Stock seed data in `StockMarket.java` |
| 2 | Method overloading | `User.sell(asset)` and `User.sell(asset, quantity)` |
| 2 | Constructor overloading | `Transaction.java` |
| 2 | Method overriding, dynamic method dispatch | `getVolatility()` in `Stock` and `TechStock`, called from `StockMarket.updatePrices()` |
| 2 | User-defined exceptions, `throw`, `throws` | `exception` package, `User.java`, `Portfolio.java` |
| 2 | Multiple catch blocks, `finally`, nested try | `TradingApp.placeOrder()`, `BackupTask.java`, `DatabaseManager.saveTrade()` |
| 3 | Thread using `Runnable` | `PriceUpdater.java` |
| 3 | Thread using `Thread` class, thread priority | `BackupTask.java` |
| 3 | Synchronization | `Asset.java`, `StockMarket.updatePrices()` |
| 3 | Inter-thread communication (`wait`, `notify`) | `PriceUpdater.java`, `LoginFrame.java` |
| 3 | Byte streams (`FileInputStream`, `FileOutputStream`) | `BackupTask.java` |
| 3 | Delegation event model, `KeyEvent`, adapter classes | `LoginFrame.java` |
| 4 | JDBC with SQLite, table creation, CRUD operations | `db` package |
| 4 | `ArrayList`, `LinkedList` | `User.java`, `Asset.java` |
| 4 | `HashSet`, `TreeSet` | `User.java`, `StockMarket.java` |
| 4 | `HashMap`, `Hashtable` | `Portfolio.java`, `StockMarket.java` |
| 4 | `Iterator` | `Portfolio.removeEmptyHoldings()` |
| 5 | Generic class with two type parameters | `Pair.java` |
| 5 | Generic method, bounded type, wildcard arguments | `MarketUtils.java`, `Portfolio.java` |
| 5 | Generic interface | `Dao.java` |
| 5 | JavaFX application skeleton and controls | `TradingApp.java` |
| 5 | `RadioButton`, `ComboBox`, `TextField`, `ListView`, `TreeView` | `TradingApp.java` |
| 5 | `MenuBar`, `Menu`, `MenuItem`, `ToolBar`, effects | `TradingApp.java` |

## Limitations

- Prices are simulated and reset to their starting values each time the application starts.
- Login uses a username only; there is no password.
- The run script targets Windows, and the bundled JavaFX jars are the Windows builds.



<!-- Add your name(s), roll number(s) and section here -->
