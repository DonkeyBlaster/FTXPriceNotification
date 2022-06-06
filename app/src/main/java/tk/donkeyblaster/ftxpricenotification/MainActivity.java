package tk.donkeyblaster.ftxpricenotification;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView rvTickers = (RecyclerView) findViewById(R.id.rvTickers);

        List<String> toAdd = new ArrayList<>();
        toAdd.add("BTC");
        toAdd.add("ETH");
        toAdd.add("SOL");
        toAdd.add("FTT");
        for (String ticker : toAdd) {
            Ticker.tickers.add(new Ticker(ticker));
        }
        TickersAdapter adapter = new TickersAdapter(Ticker.tickers);
        rvTickers.setAdapter(adapter);
        rvTickers.setLayoutManager(new LinearLayoutManager(this));
    }

    public void toggleNotificationService(View view) {

    }

    public void addNewTicker(View view) {
        Intent intent = new Intent(this, AddTickerActivity.class);
        startActivity(intent);
    }
}