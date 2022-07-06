package tk.donkeyblaster.ftxpricenotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ReceiverHidePnl extends BroadcastReceiver {

    public static final String togglePnlAction = "toggleHidePnl";

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: remove debug toast
        Toast.makeText(context, "received " + intent.getStringExtra("action"), Toast.LENGTH_SHORT).show();
        Intent notifServiceIntent = new Intent(context, NotificationService.class);
        if (!intent.getStringExtra("action").equals(togglePnlAction)) return;
        // Reverse the boolean
        NotificationService.hidePnL.set(!NotificationService.hidePnL.get());
    }
}
