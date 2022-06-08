package tk.donkeyblaster.ftxpricenotification;

import java.util.ArrayList;

public class Ticker {
    // Start static bits
    public static ArrayList<Ticker> tickers = new ArrayList<>();

    public static void addTicker(Ticker ticker) {
        tickers.add(ticker);
    }
    // End static bits

    private final String ticker;
    private float positionSize = 0;
    private float entryPrice = 0;
    private boolean isHoisted = false;

    public Ticker(String ticker, float positionSize, float entryPrice, boolean isHoisted) {
        this.ticker = ticker;
        this.positionSize = positionSize;
        this.entryPrice = entryPrice;
        this.isHoisted = isHoisted;
    }

    public Ticker(String ticker, boolean isHoisted) {
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

    public boolean isHoisted() {
        return isHoisted;
    }

    public void setPositionSize(float positionSize) {
        this.positionSize = positionSize;
    }

    public void setEntryPrice(float entryPrice) {
        this.entryPrice = entryPrice;
    }

    public void setHoisted(boolean isHoisted) {
        this.isHoisted = isHoisted;
    }

}
