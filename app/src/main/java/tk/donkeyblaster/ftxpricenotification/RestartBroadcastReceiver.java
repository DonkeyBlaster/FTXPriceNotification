package tk.donkeyblaster.ftxpricenotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class RestartBroadcastReceiver extends BroadcastReceiver {

    public static final String restartAction = "restartNotificationService";

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: remove debug toast
        Toast.makeText(context, "received " + intent.getStringExtra("action"), Toast.LENGTH_SHORT).show();
        Intent notifServiceIntent = new Intent(context, NotificationService.class);
        if (!intent.getStringExtra("action").equals(restartAction)) return;
        context.stopService(notifServiceIntent);
        context.startService(notifServiceIntent);
    }
}
