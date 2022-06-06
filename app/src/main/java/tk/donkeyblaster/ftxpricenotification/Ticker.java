package tk.donkeyblaster.ftxpricenotification;

import java.util.ArrayList;

public class Ticker {
    // Start static bits
    public static ArrayList<Ticker> tickers = new ArrayList<>();

    public static void addTicker(Ticker ticker) {
        tickers.add(ticker);
    }

    public static ArrayList<Ticker> getTickers() {
        return tickers;
    }
    // End static bits

    private final String ticker;
    private float positionSize = 0;
    private float entryPrice = 0;
    private boolean displayPnl = false;

    public Ticker(String ticker, float positionSize, float entryPrice, boolean displayPnl) {
        this.ticker = ticker;
        this.positionSize = positionSize;
        this.entryPrice = entryPrice;
        this.displayPnl = displayPnl;
    }

    public Ticker(String ticker) {
        this.ticker = ticker;
    }

    public String getTicker() {
        return ticker;
    }

    public float getPositionSize() {
        return positionSize;
    }

    public float getEntryPrice() {
        return entryPrice;
    }

    public boolean isPnlDisplayed() {
        return displayPnl;
    }

    public void setPositionSize(float positionSize) {
        this.positionSize = positionSize;
    }

    public void setEntryPrice(float entryPrice) {
        this.entryPrice = entryPrice;
    }

    public void setDisplayPnl(boolean displayPnl) {
        this.displayPnl = displayPnl;
    }
}
