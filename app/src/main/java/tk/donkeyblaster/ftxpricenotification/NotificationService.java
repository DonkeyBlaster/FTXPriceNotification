package tk.donkeyblaster.ftxpricenotification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NotificationService extends Service {

    int NOTIFICATION_ID = 1;
    String CHANNEL_ID = "Price Notification";
    String FTX_API_URI = "wss://ftx.com/ws/";

    NotificationManager notificationManager;
    Notification.Builder notificationBuilder;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("start", "start");
        notificationManager = getNotificationManager();
        createNotificationChannel(notificationManager);
        notificationBuilder = getNotificationBuilder();

        // No networking on main thread over here
        Thread wsThread = new Thread() {
            @Override
            public void run() {
                super.run();
                runWebsocketLoop(notificationManager, notificationBuilder);
            }
        };
        wsThread.start();

        return START_NOT_STICKY;
    }

    private void runWebsocketLoop(NotificationManager nm, Notification.Builder nb) {
        WebSocketFactory factory = new WebSocketFactory();
        factory.setConnectionTimeout(5000); // 5s
        //factory.setVerifyHostname(false);
        WebSocket ws = null;

        //ArrayList<Ticker> tickers = Ticker.tickers; // copy to local since the upstream copy can change
        // LinkedHashMap<Ticker, NotionalValue>
        LinkedHashMap<String, Ticker> tickers = new LinkedHashMap<>();
        for (Ticker t : Ticker.tickers) {
            tickers.put(t.getTicker(), t);
        }

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

        ws.addListener(new WebSocketAdapter() {
            @Override
            public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
                super.onConnected(websocket, headers);
                changeSubscription(websocket, tickers, false);
            }

            @Override
            public void onTextMessage(WebSocket websocket, String text) throws Exception {
                super.onTextMessage(websocket, text);
                String displayedContent = getTicker(text) + ": " + getPrice(text);
                float positionSize = tickers.get(getTicker(text)).getPositionSize();
                if (positionSize > 0) { // Long
                    double priceDiff = getPrice(text) - tickers.get(getTicker(text)).getEntryPrice();
                    displayedContent += " // PnL: " + priceDiff * positionSize;
                } else if (positionSize < 0) { // Short
                    double priceDiff = tickers.get(getTicker(text)).getEntryPrice() - getPrice(text);
                    displayedContent += " // PnL: " + priceDiff * positionSize;
                }

                editNotificationText(nb, nm, displayedContent);

            }

            @Override
            public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
                super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
                changeSubscription(websocket, tickers, true);
            }

            @Override
            public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {
                super.onSendError(websocket, cause, frame);
                Log.e("NotificationService#runWebsocketLoop.onSendError", cause.toString());
                editNotificationText(nb, nm, "Error:" + cause);
            }

            @Override
            public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
                super.onError(websocket, cause);
                Log.e("NotificationService#runWebsocketLoop.onError", cause.toString());
                editNotificationText(nb, nm, "Error:" + cause);
            }
        });
    }

    private String getTicker(String rawData) {
        if (rawData == null) return null;

        Map<String, Object> data = new Gson().fromJson(rawData, new TypeToken<HashMap<String, Object>>() {}.getType());

        try {
            return String.valueOf(data.get("market"));
        } catch (Exception e) {
            Log.w("NotificationService#getPrice", e.toString());
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Double getPrice(String rawData) {
        if (rawData == null) return null;

        Map<String, Object> data = new Gson().fromJson(rawData, new TypeToken<HashMap<String, Object>>() {}.getType());
        LinkedTreeMap<String, String> result;

        try {
            ArrayList<LinkedTreeMap<String, String>> temp = (ArrayList<LinkedTreeMap<String, String>>) data.get("data");
            result = temp.get(0);
            Log.d("asdf", String.valueOf(result.get("price")));
        } catch (Exception e) {
            Log.w("NotificationService#getPrice", e.toString());
            e.printStackTrace();
            return null;
        }

        try {
            return Double.parseDouble(result.get("price"));
        } catch (NullPointerException e) {
            Log.w("NotificationService#getPrice", e.toString());
            e.printStackTrace();
            return null;
        }
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

    private void editNotificationText(Notification.Builder nb, NotificationManager nm, String message) {
        nb.setContentText(message);
        nm.notify(NOTIFICATION_ID, nb.build());
    }

    private Notification.Builder getNotificationBuilder() {
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_show_chart_48)
                .setContentTitle("FTX Perpetual Contract Prices")
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setUsesChronometer(true);
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
        super.onDestroy();
        this.stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}