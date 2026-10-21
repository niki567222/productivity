package dev.daylog;

import android.app.*;
import android.content.*;
import android.os.Build;

final class Alarms {
    static final long INTERVAL = 15 * 60 * 1000L;
    static android.content.SharedPreferences prefs(Context c) { return c.getSharedPreferences("alarm", Context.MODE_PRIVATE); }
    static PendingIntent pending(Context c) { return PendingIntent.getBroadcast(c, 1, new Intent(c, AlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE); }
    static boolean allowed(Context c) { return Build.VERSION.SDK_INT < 31 || c.getSystemService(AlarmManager.class).canScheduleExactAlarms(); }
    static void schedule(Context c) {
        if (!prefs(c).getBoolean("enabled", false) || !allowed(c)) return;
        long next = System.currentTimeMillis() + INTERVAL;
        c.getSystemService(AlarmManager.class).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending(c));
        prefs(c).edit().putLong("next", next).apply();
    }
    static void stop(Context c) {
        prefs(c).edit().putBoolean("enabled", false).remove("pending").remove("next").apply();
        c.getSystemService(AlarmManager.class).cancel(pending(c));
        c.getSystemService(NotificationManager.class).cancel(1);
    }
    static void channel(Context c) {
        NotificationChannel ch = new NotificationChannel("checkin", "Записать занятие", NotificationManager.IMPORTANCE_HIGH);
        ch.setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM), new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ALARM).build());
        ch.enableVibration(true);
        c.getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }
}
