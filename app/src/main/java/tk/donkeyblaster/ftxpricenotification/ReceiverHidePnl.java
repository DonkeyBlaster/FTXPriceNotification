package tk.donkeyblaster.ftxpricenotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ReceiverHidePnl extends BroadcastReceiver {

    public static final String togglePnlAction = "toggleHidePnl";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getStringExtra("action").equals(togglePnlAction)) return;
        // Reverse the boolean
        NotificationService.hidePnL.set(!NotificationService.hidePnL.get());
    }
}
