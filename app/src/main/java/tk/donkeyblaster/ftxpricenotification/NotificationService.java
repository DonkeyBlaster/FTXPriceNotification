package tk.donkeyblaster.ftxpricenotification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.IBinder;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class NotificationService extends Service {

    int NOTIFICATION_ID = 1;
    String CHANNEL_ID = "Price Notification";
    String FTX_API_URI = "wss://ftx.com/ws/";
    AtomicBoolean serviceStopped = new AtomicBoolean(false);

    static AtomicBoolean hidePnL = new AtomicBoolean(false);

    NotificationManager notificationManager;
    Notification notification;

    WebSocketFactory factory = new WebSocketFactory().setConnectionTimeout(5000);
    WebSocket ws = null;
    Thread wsThread = null;
    LinkedHashMap<String, Ticker> subscribedTickers = new LinkedHashMap<>();

    // LinkedHashMap<Ticker, displayedContent>
    LinkedHashMap<String, String> notificationData = new LinkedHashMap<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("NotificationService#onStartCommand", "Starting notification service");
        serviceStopped.set(false);
        notificationManager = getNotificationManager();
        createNotificationChannel(notificationManager);
        notification = getNewSingleNotification("Starting...");

        // No networking on main thread over here
        wsThread = new Thread() {
            @Override
            public void run() {
                super.run();
                runWebsocketLoop();
            }
        };
        wsThread.start();

        startForeground(NOTIFICATION_ID, notification);

        return START_NOT_STICKY;
    }

    private void runWebsocketLoop() {
        try {
            ws = factory.createSocket(FTX_API_URI);
            ws.setPingInterval(15000); // Ping every 15s
            ws.connect();
            Log.d("NotificationService#runWebsocketLoop", "ws connected");
        } catch (IOException e) {
            Log.e("NotificationService#runWebsocketLoop.IOException", e.toString());
            e.printStackTrace();
        } catch (WebSocketException e) {
            Log.e("NotificationService#runWebsocketLoop.WebSocketException", e.toString());
            e.printStackTrace();
        }

        if (ws == null) return;

        // copy to local since the upstream copy can change
        for (Ticker t : Ticker.tickers) {
            subscribedTickers.put(t.getTicker(), t);
            notificationData.put(t.getTicker(), t.getTicker() + ": Waiting for data..."); // append to data now so order is correct
        }

        ws.addListener(new WebSocketAdapter() {
            @Override
            public void onTextMessage(WebSocket websocket, String text) {
                if (serviceStopped.get() || text.contains("subscribed")) return; // No need to process
                Double price = getPrice(text);
                String ticker = getTicker(text).replace("-PERP", "");
                String displayedContent = ticker + ": " + price;
                float positionSize = subscribedTickers.get(ticker).getPositionSize();

                if (!hidePnL.get()) { // Skip over pnl calculation when enabled
                    if (positionSize > 0) { // Long
                        double priceDiff = getPrice(text) - subscribedTickers.get(ticker).getEntryPrice();
                        double pnl = Math.round(priceDiff * positionSize * 100.0) / 100.0;
                        String plus = (pnl > 0) ? "+" : "";
                        displayedContent += " (" + plus + pnl + ")";
                    } else if (positionSize < 0) { // Short
                        double priceDiff = getPrice(text) - subscribedTickers.get(ticker).getEntryPrice();
                        double pnl = Math.round(priceDiff * positionSize * 100.0) / 100.0;
                        String plus = (pnl > 0) ? "+" : "";
                        displayedContent += " (" + plus + pnl + ")";
                    }
                }
                notificationData.put(ticker, displayedContent);

                // DO NOT ACCESS displayedContent BELOW HERE
                // displayedContent contains ONLY the updated ticker data, not all
                StringBuilder condensedDisplayQueue = new StringBuilder();
                StringBuilder displayQueue = new StringBuilder();
                boolean needsFormatterPrefix = false;
                int lowPriorityCount = 0;
                for (Map.Entry<String, String> contentSet: notificationData.entrySet()) {
                    if (subscribedTickers.get(contentSet.getKey()).isHoisted()) {
                        if (needsFormatterPrefix) {
                            condensedDisplayQueue.append(" • ");
                        }
                        condensedDisplayQueue.append(contentSet.getValue());
                        needsFormatterPrefix = true;
                    } else {
                        lowPriorityCount++;
                    }
                    displayQueue.append(contentSet.getValue()).append("<br>");
                }
                if (lowPriorityCount > 0) {
                    condensedDisplayQueue.append(" and ").append(lowPriorityCount).append(" more");
                }
                Spannable spannable = new SpannableString(Html.fromHtml(displayQueue.substring(0, displayQueue.length() - 4), Html.FROM_HTML_MODE_COMPACT));
                displayNotification(notificationManager, getNewNotification(condensedDisplayQueue, spannable));
            }

            @Override
            public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
                // This error is triggered when internet is disconnected with an active websocket
                // TODO: Auto reconnect logic
                super.onError(websocket, cause);
                Log.e("NotificationService#runWebsocketLoop.onError", cause.toString());
                displayNotification(notificationManager, getNewSingleNotification(cause.toString()));
            }
        });
        changeSubscription(ws, subscribedTickers, false);
    }

    private String getTicker(String rawData) {
        if (rawData == null) return null;

        JsonObject jsonObject = JsonParser.parseString(rawData).getAsJsonObject();
        return jsonObject.get("market").getAsString();
    }

    private Double getPrice(String rawData) {
        if (rawData == null) return null;

        JsonObject jsonObject = JsonParser.parseString(rawData).getAsJsonObject();
        JsonArray jsonArray = jsonObject.getAsJsonArray("data");
        JsonObject finalObject = jsonArray.get(0).getAsJsonObject();
        return finalObject.get("price").getAsDouble();
    }

    private void changeSubscription(WebSocket ws, LinkedHashMap<String, Ticker> tickers, boolean unsubscribe) {
        String opSwitch = (unsubscribe) ? "unsubscribe" : "subscribe";
        for (Map.Entry<String, Ticker> entry : tickers.entrySet()) {
            JsonObject request = new JsonObject();
            request.addProperty("op", opSwitch);
            request.addProperty("channel", "trades");
            request.addProperty("market", entry.getKey() + "-PERP");
            ws.sendText(String.valueOf(request));
        }
    }

    private void displayNotification(NotificationManager nm, Notification n) {
        nm.notify(NOTIFICATION_ID, n);
    }

    private Notification getNewNotification(CharSequence content, CharSequence expandedContent) {
        // launches activity when user taps the notification
        PendingIntent launchActivityPI = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);

        // restarts notification service when user taps corresponding button
        Intent restartNotifServiceI = new Intent(this, ReceiverRestartBroadcast.class).putExtra("action", ReceiverRestartBroadcast.restartAction);
        PendingIntent restartNotifServicePI = PendingIntent.getBroadcast(this, 0, restartNotifServiceI, PendingIntent.FLAG_IMMUTABLE);

        // kills notification service when user taps corresponding button
        Intent killNotifServiceI = new Intent(this, ReceiverKillBroadcast.class).putExtra("action", ReceiverKillBroadcast.killAction);
        PendingIntent killNotifServicePI = PendingIntent.getBroadcast(this, 0, killNotifServiceI, PendingIntent.FLAG_IMMUTABLE);

        // toggle pnl until next notification restart or toggled again
        Intent togglePnlI = new Intent(this, ReceiverHidePnl.class).putExtra("action", ReceiverHidePnl.togglePnlAction);
        PendingIntent togglePnlPI = PendingIntent.getBroadcast(this, 0, togglePnlI, PendingIntent.FLAG_IMMUTABLE);
        String togglePnlString = (hidePnL.get()) ? "Show PnL" : "Hide PnL";

        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_show_chart_48)
                .setContentTitle("FTX Perpetual Contract Prices")
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setUsesChronometer(true)
                .setContentIntent(launchActivityPI)
                .addAction(new Notification.Action.Builder(Icon.createWithResource(getApplicationContext(), R.drawable.ic_baseline_refresh_24), "Restart", restartNotifServicePI).build())
                .addAction(new Notification.Action.Builder(Icon.createWithResource(getApplicationContext(), R.drawable.ic_baseline_delete_24), "Remove", killNotifServicePI).build())
                .addAction(new Notification.Action.Builder(Icon.createWithResource(getApplicationContext(), R.drawable.ic_baseline_bar_chart_24), togglePnlString, togglePnlPI).build())
                .setContentText(content.toString().split("<br>")[0]) // Only first ticker for small content
                .setStyle(new Notification.BigTextStyle().bigText(expandedContent))
                .build();
    }

    private Notification getNewSingleNotification(CharSequence content) {
        return getNewNotification(content, content);
    }

    private NotificationManager getNotificationManager() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
        return nm;
    }

    private void createNotificationChannel(NotificationManager nm) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("ooga booga BOOOGA booga booga");
        channel.enableLights(false);
        channel.enableVibration(false);
        nm.createNotificationChannel(channel);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.d("NotificationService#onDestroy", "onDestroy called");
        serviceStopped.set(true);
        changeSubscription(ws, subscribedTickers, true);
        ws.disconnect();
        ws = null;
        factory = null;
        wsThread.interrupt();
        wsThread = null;
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}