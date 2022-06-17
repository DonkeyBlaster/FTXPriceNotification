package tk.donkeyblaster.ftxpricenotification;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    TickersAdapter adapter;
    protected boolean notificationServiceRunning = false;
    String ENCRYPTED_TICKER_PREFS = "encryptedTickers";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton startStop = findViewById(R.id.startStopButton);
        if (isNotificationServiceRunning()) {
            startStop.setImageResource(R.drawable.ic_baseline_stop_48);
        }

        RecyclerView rvTickers = findViewById(R.id.rvTickers);

        loadTickers();
        adapter = new TickersAdapter(Ticker.tickers);
        rvTickers.setAdapter(adapter);
        rvTickers.setLayoutManager(new LinearLayoutManager(this));

        // Drag and drop reordering
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source, @NonNull RecyclerView.ViewHolder target) {
                if (source.getItemViewType() != target.getItemViewType()) {
                    return false;
                }
                Collections.swap(Ticker.tickers, source.getAdapterPosition(), target.getAdapterPosition());
                adapter.notifyItemMoved(source.getAdapterPosition(), target.getAdapterPosition());
                saveTickers();
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // nothing
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(rvTickers);

        // Broadcast receiver for sync positions button
        BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            @SuppressLint("NotifyDataSetChanged")
            public void onReceive(Context context, Intent intent) {
                adapter.notifyDataSetChanged();
            }
        };
        IntentFilter filter = new IntentFilter("tk.donkeyblaster.broadcast.POSITIONS_SYNCED");
        this.registerReceiver(br, filter);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onResume() {
        super.onResume();
        saveTickers();
        loadTickers();
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveTickers();
    }

    public static SharedPreferences getEncryptedPreferences(Context context, String fileName) throws GeneralSecurityException, IOException {
        KeyGenParameterSpec keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC;
        String mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec);
        return EncryptedSharedPreferences.create(fileName, mainKeyAlias, context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }

    public void loadTickers() {
        Ticker.tickers.clear();
        try {
            SharedPreferences prefs = getEncryptedPreferences(getApplicationContext(), ENCRYPTED_TICKER_PREFS);
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) prefs.getAll();
            for (Map.Entry<String, String> mapEntry : map.entrySet()) {
                String t = mapEntry.getKey();
                String rawPositionData = mapEntry.getValue();
                float size = Float.parseFloat(rawPositionData.split(",")[0]);
                float entry = Float.parseFloat(rawPositionData.split(",")[1]);
                boolean hoisted = Boolean.parseBoolean(rawPositionData.split(",")[2]);
                Ticker.tickers.add(new Ticker(t, size, entry, hoisted));
            }
        } catch (GeneralSecurityException e) {
            Log.e("MainActivity#loadTickers.GeneralSecurityException", e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("MainActivity#loadTickers.IOException", e.toString());
            e.printStackTrace();
        }
    }

    public void saveTickers() {
        try {
            SharedPreferences.Editor prefEditor = getEncryptedPreferences(getApplicationContext(), ENCRYPTED_TICKER_PREFS).edit();
            prefEditor.clear();
            for (Ticker ticker : Ticker.tickers) {
                String t = ticker.getTicker();
                String size = String.valueOf(ticker.getPositionSize());
                String entry = String.valueOf(ticker.getEntryPrice());
                String hoisted = String.valueOf(ticker.isHoisted());

                String saveString = size + "," + entry + "," + hoisted;
                prefEditor.putString(t, saveString);
                Log.d("Saving", t + ":" + saveString);
            }
            prefEditor.apply();
        } catch (GeneralSecurityException e) {
            Log.e("MainActivity#clearAndSaveTickers.GeneralSecurityException", e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("MainActivity#clearAndSaveTickers.IOException", e.toString());
            e.printStackTrace();
        }
    }

    public void toggleNotificationService(View view) {
        ImageButton ib = findViewById(R.id.startStopButton);

        Intent notifServiceIntent = new Intent(this, NotificationService.class);
        if (isNotificationServiceRunning()) {
            // Stopping the service
            stopService(notifServiceIntent);

            ib.setImageResource(R.drawable.ic_baseline_play_arrow_48);

        } else {
            if (Ticker.tickers.size() == 0) {
                Toast.makeText(view.getContext(), "Cannot start notification with 0 tickers.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Starting the service
            startService(notifServiceIntent);

            ib.setImageResource(R.drawable.ic_baseline_stop_48);
        }

    }

    private boolean isNotificationServiceRunning() {
        // Check if notification is actually running
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        StatusBarNotification[] notifications = nm.getActiveNotifications();
        notificationServiceRunning = false;
        for (StatusBarNotification n : notifications) {
            if (n.getId() == 1) {
                notificationServiceRunning = true;
            }
        }
        return notificationServiceRunning;
    }

    public void openAddTickerActivity(View view) {
        Intent intent = new Intent(this, AddTickerActivity.class);
        startActivity(intent);
    }

    public void openSyncPositionsActivity(View view) {
        Intent intent = new Intent(this, SyncPositionsActivity.class);
        startActivity(intent);
    }
}