package tk.donkeyblaster.ftxpricenotification;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    TickersAdapter adapter;
    boolean notificationServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView rvTickers = findViewById(R.id.rvTickers);

        loadTickers();
        adapter = new TickersAdapter(Ticker.tickers);
        rvTickers.setAdapter(adapter);
        rvTickers.setLayoutManager(new LinearLayoutManager(this));
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onResume() {
        super.onResume();
        clearAndSaveTickers();
        loadTickers();
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        clearAndSaveTickers();
    }

    public void loadTickers() {
        Ticker.tickers.clear();
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        @SuppressWarnings("unchecked")
        Map<String, String> map = (Map<String, String>) prefs.getAll();
        for (Map.Entry<String, String> mapEntry : map.entrySet()) {
            String t = mapEntry.getKey();
            String rawPositionData = mapEntry.getValue();
            float size = Float.parseFloat(rawPositionData.split(",")[0]);
            float entry = Float.parseFloat(rawPositionData.split(",")[1]);
            Ticker.tickers.add(new Ticker(t, size, entry));
        }
    }

    public void clearAndSaveTickers() {
        SharedPreferences.Editor prefEditor = getPreferences(Context.MODE_PRIVATE).edit();
        prefEditor.clear();
        for (Ticker ticker : Ticker.tickers) {
            String t = ticker.getTicker();
            String size = String.valueOf(ticker.getPositionSize());
            String entry = String.valueOf(ticker.getEntryPrice());
            prefEditor.putString(t, size + "," + entry);
            Log.d("saving", t + ":" + size + "," + entry);
        }
        prefEditor.apply();
    }

    public void toggleNotificationService(View view) {
        ImageButton ib = findViewById(R.id.startStopButton);
        Intent notifServiceIntent = new Intent(this, NotificationService.class);
        if (notificationServiceRunning) {
            // Stopping the service
            stopService(notifServiceIntent);

            ib.setImageResource(R.drawable.ic_baseline_play_arrow_48);
            notificationServiceRunning = false;

        } else {
            if (Ticker.tickers.size() == 0) {
                Toast.makeText(view.getContext(), "Cannot start notification with 0 tickers.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Starting the service
            startService(notifServiceIntent);

            ib.setImageResource(R.drawable.ic_baseline_stop_48);
            notificationServiceRunning = true;
        }

    }

    public void openAddTickerActivity(View view) {
        Intent intent = new Intent(this, AddTickerActivity.class);
        startActivity(intent);
    }
}