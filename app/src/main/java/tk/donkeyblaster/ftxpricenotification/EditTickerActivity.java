package tk.donkeyblaster.ftxpricenotification;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EditTickerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_ticker);
    }

    public void addTicker(View view) {
        EditText tickerEditText = findViewById(R.id.tickerTextBox);
        EditText sizeEditText = findViewById(R.id.sizeTextBox);
        EditText entryEditText = findViewById(R.id.entryTextBox);

        String ticker = tickerEditText.getText().toString();
        if (ticker.equals("")) {
            Toast.makeText(view.getContext(), "Ticker field is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        String positionSize = sizeEditText.getText().toString();
        String entryPrice = entryEditText.getText().toString();
        if ((positionSize.equals("") && !entryPrice.equals("")) || (!positionSize.equals("") && entryPrice.equals(""))) {
            Toast.makeText(view.getContext(), "Both position fields should be populated.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (positionSize.equals("")) {
            Ticker.addTicker(new Ticker(ticker));
        } else {
            Ticker.addTicker(new Ticker(ticker, Float.parseFloat(positionSize), Float.parseFloat(entryPrice)));
        }
        this.finish();
    }
}