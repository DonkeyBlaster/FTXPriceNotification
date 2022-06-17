package tk.donkeyblaster.ftxpricenotification;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SyncPositionsActivity extends AppCompatActivity {

    String ENCRYPTED_API_PREFS = "encryptedFTXAPIKeys";

    EditText keyEditText;
    EditText secretEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_postions);

        keyEditText = findViewById(R.id.editKeyTextBox);
        secretEditText = findViewById(R.id.editSecretTextBox);

        try {
            SharedPreferences sharedPreferences = MainActivity.getEncryptedPreferences(this, ENCRYPTED_API_PREFS);
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) sharedPreferences.getAll();
            for (Map.Entry<String, String> mapEntry : map.entrySet()) {
                String mapEntryKey = mapEntry.getKey();
                String mapEntryValue = mapEntry.getValue();
                switch (mapEntryKey) {
                    case "ftxAPIKey":
                        keyEditText.setText(mapEntryValue);
                        break;
                    case "ftxAPISecret":
                        secretEditText.setText(mapEntryValue);
                        break;
                }
            }

        } catch (GeneralSecurityException e) {
            Log.e("SyncPositionsActivity#onCreate.GeneralSecurityException", e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("SyncPositionsActivity#onCreate.IOException", e.toString());
            e.printStackTrace();
        }
    }

    private String toHex(byte[] arg) {
        return String.format("%040x", new BigInteger(1, arg));
    }

    private String sign(long timestamp, String secret) {
        String data = timestamp + "GET" + "/api/positions?showAvgPrice=true";
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        try {
            Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
            hmacSHA256.init(secretKeySpec);
            return toHex(hmacSHA256.doFinal(data.getBytes()));
        } catch (NoSuchAlgorithmException | InvalidKeyException ignored) {}
        return null;
    }

    public void saveReloadPositions(View view) {
        Toast.makeText(this, "Retrieving positions...", Toast.LENGTH_SHORT).show();
        try {
            SharedPreferences.Editor prefEditor = MainActivity.getEncryptedPreferences(this, ENCRYPTED_API_PREFS).edit();
            prefEditor.clear();
            prefEditor.putString("ftxAPIKey", keyEditText.getText().toString());
            prefEditor.putString("ftxAPISecret", secretEditText.getText().toString());
            Log.d("SyncPositionsActivity#saveReloadPositions", "Saved API keys");
            prefEditor.apply();
        } catch (GeneralSecurityException e) {
            Log.e("SyncPositionsActivity#saveReloadPositions.GeneralSecurityException", e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("SyncPositionsActivity#saveReloadPositions.IOException", e.toString());
            e.printStackTrace();
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://ftx.com/api/positions?showAvgPrice=true";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
            response -> {
                JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
                JsonArray jsonArray = jsonObject.getAsJsonArray("result");
                for (JsonElement je : jsonArray) {
                    float netSize = je.getAsJsonObject().get("netSize").getAsFloat();
                    if (netSize != 0) {
                        Log.d("a", je.toString());
                        String ticker = je.getAsJsonObject().get("future").getAsString().replace("-PERP", "");
                        float breakEven = je.getAsJsonObject().get("recentBreakEvenPrice").getAsFloat();
                        Ticker.tickers.removeIf(t -> t.getTicker().equals(ticker));
                        Ticker.tickers.add(new Ticker(ticker, netSize, Math.round(breakEven * 100.0f) / 100.0f, true));
                    }
                }
                Toast.makeText(this, "Positions synced", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.setAction("tk.donkeyblaster.broadcast.POSITIONS_SYNCED");
                sendBroadcast(intent);
            }, error -> {
                String e = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                Log.e("saveReloadPositions Volley Error", e);
                Toast.makeText(getApplicationContext(), e, Toast.LENGTH_SHORT).show();
            }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                long ts = System.currentTimeMillis();
                String key = keyEditText.getText().toString();
                String secret = secretEditText.getText().toString();
                headers.put("FTX-KEY", key);
                headers.put("FTX-SIGN", sign(ts, secret));
                headers.put("FTX-TS", String.valueOf(ts));
                return headers;
            }
        };
        queue.add(stringRequest);
        this.finish();
    }
}