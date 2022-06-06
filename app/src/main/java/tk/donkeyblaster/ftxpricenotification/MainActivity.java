package tk.donkeyblaster.ftxpricenotification;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ArrayList<Ticker> tickers;

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
            tickers.add(new Ticker(ticker));
        }
        TickersAdapter adapter = new TickersAdapter(tickers);
        rvTickers.setAdapter(adapter);
        rvTickers.setLayoutManager(new LinearLayoutManager(this));
    }
}