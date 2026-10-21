package dev.daylog;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {
    public void onReceive(Context c, Intent i) { if (Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())) Alarms.schedule(c); }
}
