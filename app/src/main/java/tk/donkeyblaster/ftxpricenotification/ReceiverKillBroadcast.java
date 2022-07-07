package tk.donkeyblaster.ftxpricenotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ReceiverKillBroadcast extends BroadcastReceiver {

    public static final String killAction = "killNotificationService";

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent notifServiceIntent = new Intent(context, NotificationService.class);
        if (!intent.getStringExtra("action").equals(killAction)) return;
        context.stopService(notifServiceIntent);
    }
}
