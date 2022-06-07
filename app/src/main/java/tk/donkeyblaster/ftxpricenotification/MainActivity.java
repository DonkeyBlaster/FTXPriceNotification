package tk.donkeyblaster.ftxpricenotification;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TickersAdapter adapter;
    boolean notificationServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView rvTickers = findViewById(R.id.rvTickers);

        List<String> toAdd = new ArrayList<>();
        toAdd.add("BTC");
        toAdd.add("ETH");
        toAdd.add("SOL");
        toAdd.add("FTT");
        for (String ticker : toAdd) {
            Ticker.tickers.add(new Ticker(ticker));
        }
        adapter = new TickersAdapter(Ticker.tickers);
        rvTickers.setAdapter(adapter);
        rvTickers.setLayoutManager(new LinearLayoutManager(this));
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    public void toggleNotificationService(View view) {
        ImageButton ib = findViewById(R.id.startStopButton);
        if (notificationServiceRunning) {
            ib.setImageResource(R.drawable.ic_baseline_play_arrow_48);
            notificationServiceRunning = false;

        } else {
            ib.setImageResource(R.drawable.ic_baseline_stop_48);
            notificationServiceRunning = true;
        }

    }

    public void openAddTickerActivity(View view) {
        Intent intent = new Intent(this, AddTickerActivity.class);
        startActivity(intent);
    }
}