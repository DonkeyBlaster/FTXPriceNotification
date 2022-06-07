package tk.donkeyblaster.ftxpricenotification;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

    @SuppressLint("ApplySharedPref")
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
        prefEditor.commit();
    }

    public void toggleNotificationService(View view) {
        ImageButton ib = findViewById(R.id.startStopButton);
        if (notificationServiceRunning) {
            // Stopping the service
            ib.setImageResource(R.drawable.ic_baseline_play_arrow_48);
            notificationServiceRunning = false;

        } else {
            // Starting the service
            ib.setImageResource(R.drawable.ic_baseline_stop_48);
            notificationServiceRunning = true;
        }

    }

    public void openAddTickerActivity(View view) {
        Intent intent = new Intent(this, AddTickerActivity.class);
        startActivity(intent);
    }
}