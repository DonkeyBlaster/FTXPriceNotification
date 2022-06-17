package tk.donkeyblaster.ftxpricenotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ServiceBroadcastReceiver extends BroadcastReceiver {

    public static final String restartAction = "restartNotificationService";
    public static final String killAction = "killNotificationService";

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "received " + intent.getStringExtra("action"), Toast.LENGTH_SHORT).show();
        Intent notifServiceIntent = new Intent(context, NotificationService.class);
        switch (intent.getStringExtra("action")) {
            case restartAction:
                context.stopService(notifServiceIntent);
                context.startService(notifServiceIntent);
                break;
            case killAction:
                context.stopService(notifServiceIntent);
                break;

        }
    }
}
