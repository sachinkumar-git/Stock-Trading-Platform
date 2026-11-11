package service;

// Background job that keeps changing the stock prices, like a live market.
// Thread is created the "Runnable" way:  new Thread(new PriceUpdater(...)).start()
// (the other way, extending Thread, is used in BackupTask)
public class PriceUpdater implements Runnable {

    public static final int INTERVAL_MS = 3000;

    private final StockMarket market;
    private final Runnable afterUpdate;     // GUI gives this, we call it after every price change

    private boolean paused = false;

    // volatile - changed by GUI thread, read by this thread, so always read the fresh value
    private volatile boolean running = true;

    public PriceUpdater(StockMarket market, Runnable afterUpdate) {
        this.market = market;
        this.afterUpdate = afterUpdate;
    }

    @Override
    public void run() {
        while (running) {
            try {
                waitWhilePaused();

                market.updatePrices();
                afterUpdate.run();

                Thread.sleep(INTERVAL_MS);

            } catch (InterruptedException e) {
                // someone interrupted us (app is closing) -> end the loop
                running = false;
            }
        }
    }

    // Inter-thread communication.
    // wait() makes THIS thread sleep and release the lock until another thread calls notify.
    // while loop (not if) because a thread can wake up without notify sometimes.
    private synchronized void waitWhilePaused() throws InterruptedException {
        while (paused) {
            wait();
        }
    }

    // called from GUI thread (Market menu -> Pause Price Updates)
    public synchronized void pause() {
        paused = true;
    }

    public synchronized void resume() {
        paused = false;
        notifyAll();    // wakes up the updater thread that is stuck in wait()
    }

    public void stop() {
        running = false;
    }
}
