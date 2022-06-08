package tk.donkeyblaster.ftxpricenotification;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class EditTickerActivity extends AppCompatActivity {

    Ticker t;
    EditText tickerEditText;
    EditText sizeEditText;
    EditText entryEditText;
    SwitchCompat hoistedSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_ticker);

        t = Ticker.tickers.get(getIntent().getIntExtra("index", -1));
        tickerEditText = findViewById(R.id.editTickerTextBox);
        sizeEditText = findViewById(R.id.editSizeTextBox);
        entryEditText = findViewById(R.id.editEntryTextBox);
        hoistedSwitch = findViewById(R.id.editHoistedSwitch);

        tickerEditText.setText(t.getTicker());
        String oldPositionSize = String.valueOf(t.getPositionSize()).equals("0.0") ? "" : String.valueOf(t.getPositionSize());
        sizeEditText.setText(oldPositionSize);
        String oldEntryPrice = String.valueOf(t.getEntryPrice()).equals("0.0")? "" :String.valueOf(t.getEntryPrice());
        entryEditText.setText(oldEntryPrice);
        hoistedSwitch.setChecked(t.isHoisted());
    }

    public void editTicker(View view) {

        String newPositionSize = sizeEditText.getText().toString();
        String newEntryPrice = entryEditText.getText().toString();

        if ((newPositionSize.equals("") && !newEntryPrice.equals("")) || (!newPositionSize.equals("") && newEntryPrice.equals(""))) {
            Toast.makeText(view.getContext(), "Both position fields should be populated.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newEntryPrice.equals("")) {
            t.setPositionSize(0);
            t.setEntryPrice(0);
        } else {
            t.setPositionSize(Float.parseFloat(sizeEditText.getText().toString()));
            t.setEntryPrice(Float.parseFloat(entryEditText.getText().toString()));
        }
        t.setHoisted(hoistedSwitch.isChecked());

        this.finish();
    }
}