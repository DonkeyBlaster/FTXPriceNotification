package tk.donkeyblaster.ftxpricenotification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
        notification = getNewNotification("Starting...");

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
        // LinkedHashMap<Ticker, NotionalValue>
        for (Ticker t : Ticker.tickers) {
            subscribedTickers.put(t.getTicker(), t);
        }

        ws.addListener(new WebSocketAdapter() {
            @Override
            public void onTextMessage(WebSocket websocket, String text) {
                if (serviceStopped.get() || text.contains("subscribed")) return; // No need to process
                Double price = getPrice(text);
                String ticker = getTicker(text).replace("-PERP", "");
                String displayedContent = ticker + ": " + price;
                float positionSize = subscribedTickers.get(ticker).getPositionSize();

                if (positionSize > 0) { // Long
                    double priceDiff = getPrice(text) - subscribedTickers.get(ticker).getEntryPrice();
                    double pnl = Math.round(priceDiff * positionSize * 100.0) / 100.0;
                    String prefix = (pnl > 0) ? " (+" : " (";
                    displayedContent += prefix + pnl + ")";
                } else if (positionSize < 0) { // Short
                    double priceDiff = getPrice(text) - subscribedTickers.get(ticker).getEntryPrice();
                    double pnl = Math.round(priceDiff * positionSize * 100.0) / 100.0;
                    String prefix = (pnl > 0) ? " (+" : " (";
                    displayedContent += prefix + pnl + ")";
                }
                notificationData.put(ticker, displayedContent);

                // DO NOT ACCESS displayedContent BELOW HERE
                StringBuilder displayQueue = new StringBuilder();
                for (Map.Entry<String, String> contentSet: notificationData.entrySet()) {
                    displayQueue.append(contentSet.getValue());
                    displayQueue.append("<br>"); // normal space
                }
                Spannable spannable = new SpannableString(Html.fromHtml(displayQueue.substring(0, displayQueue.length() - 4), Html.FROM_HTML_MODE_COMPACT));
                displayNotification(notificationManager, getNewNotification(spannable));
            }

            @Override
            public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {
                super.onSendError(websocket, cause, frame);
                Log.e("NotificationService#runWebsocketLoop.onSendError", cause.toString());
                displayNotification(notificationManager, getNewNotification(cause.toString()));
            }

            @Override
            public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
                super.onError(websocket, cause);
                Log.e("NotificationService#runWebsocketLoop.onError", cause.toString());
                displayNotification(notificationManager, getNewNotification(cause.toString()));
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

    private Notification getNewNotification(CharSequence content) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        // PendingIntent launches activity when user taps the notification
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_show_chart_48)
                .setContentTitle("FTX Perpetual Contract Prices")
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setUsesChronometer(true)
                .setContentIntent(pendingIntent)
                .setContentText(content.toString().split("<br>")[0]) // Only first ticker for small content
                .setStyle(new Notification.BigTextStyle().bigText(content))
                .build();
    }

    private NotificationManager getNotificationManager() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
        return nm;
    }

    private void createNotificationChannel(NotificationManager nm) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_LOW);
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
        Log.d("NotificationService#onDestroy", "onDestory called");
        serviceStopped.set(true);
        changeSubscription(ws, subscribedTickers, true);
        ws.disconnect();
        ws = null;
        factory = null;
        wsThread.interrupt();
        wsThread = null;
        super.onDestroy();
        this.stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}