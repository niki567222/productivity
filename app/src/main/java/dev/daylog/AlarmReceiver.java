package dev.daylog;
import android.app.*;
import android.content.*;

public class AlarmReceiver extends BroadcastReceiver {
    public void onReceive(Context c, Intent intent) {
        if (!Alarms.prefs(c).getBoolean("enabled", false)) return;
        // Keep one unanswered prompt; never stack forms or invent missing entries.
        long slot = Alarms.prefs(c).getLong("pending", System.currentTimeMillis());
        Alarms.prefs(c).edit().putLong("pending", slot).apply();
        Alarms.schedule(c); Alarms.channel(c);
        PendingIntent open = PendingIntent.getActivity(c, 1, new Intent(c, EntryActivity.class).putExtra("slot", slot), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(c, Alarms.channelId(c)).setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Чем ты сейчас занимаешься?").setContentText("Нажми, чтобы записать последние " + Alarms.minutes(c) + " мин.")
            .setCategory(Notification.CATEGORY_ALARM).setContentIntent(open).setFullScreenIntent(open, true).setAutoCancel(true).build();
        if (android.os.Build.VERSION.SDK_INT < 33 || c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) c.getSystemService(NotificationManager.class).notify(1, n);
    }
}
